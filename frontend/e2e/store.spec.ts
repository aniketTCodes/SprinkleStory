import { test, expect, type Page } from '@playwright/test'
const category = { id: 'c1', name: 'Chocolates', description: 'Small-batch treats' }
const unit = { id: 'u1', code: 'PCS', name: 'Piece' }
const product = { id: 'p1', productCode: 'SS-00001', displayName: 'Dark chocolate truffles', category, unit, mrp: 120, status: 'ACTIVE', barcode: null, onHandQty: 24 }
const supplier = { id: 's1', name: 'Sweet Supply Co.', contact: '9876543210', corpName: 'Sweet Supply', pocName: 'Anita' }
async function mockStore(page: Page, options: { empty?: boolean; noCategories?: boolean; conflict?: boolean } = {}) {
  const mutations: string[] = []
  await page.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url())
    const method = route.request().method()
    if (method !== 'GET') {
      mutations.push(method + ' ' + url.pathname)
      if (options.conflict) return route.fulfill({ status: 409, json: { message: 'SKU has inventory and cannot be disabled' } })
      return route.fulfill({ status: method === 'DELETE' || url.pathname.endsWith('/disable') ? 204 : 200, ...(method === 'DELETE' || url.pathname.endsWith('/disable') ? {} : { json: product }) })
    }
    const rows = url.pathname.endsWith('/categories') ? options.noCategories ? [] : [category] : url.pathname.endsWith('/suppliers') ? [supplier] : options.empty || url.searchParams.get('q') === 'missing' || url.searchParams.get('status') === 'INACTIVE' ? [] : [product]
    return route.fulfill({ json: rows })
  })
  return mutations
}
test('product filters preserve units from the complete catalog', async ({ page }) => {
  await mockStore(page)
  await page.goto('/products')
  await expect(page.getByText(product.displayName, { exact: true })).toBeVisible()
  const requests: string[] = []
  page.on('request', req => { if (req.url().includes('/api/v1/sku?')) requests.push(req.url()) })
  await page.getByLabel('Search products').fill('missing')
  await expect(page.getByText('No matches on these shelves')).toBeVisible()
  await page.getByRole('button', { name: 'Add product', exact: true }).first().click()
  await expect(page.getByLabel('Unit *').locator('option')).toHaveText(['Choose unit', 'Piece (PCS)'])
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).not.toBeVisible()
  expect(requests.some(url => url.includes('q=missing'))).toBe(true)
  await page.getByLabel('Filter by status').selectOption('INACTIVE')
  await page.getByLabel('Filter by category').selectOption('c1')
  await expect.poll(() => requests.some(url => url.includes('status=INACTIVE') && url.includes('categoryId=c1'))).toBe(true)
})
test('empty catalog blocks creation without fabricating units', async ({ page }) => {
  await mockStore(page, { empty: true })
  await page.goto('/products')
  await expect(page.getByText('No units are available', { exact: false })).toBeVisible()
  for (const button of await page.getByRole('button', { name: 'Add product', exact: true }).all()) await expect(button).toBeDisabled()
})
test('missing categories blocks creation', async ({ page }) => {
  await mockStore(page, { noCategories: true })
  await page.goto('/products')
  await expect(page.getByText('Add a category before creating a product.')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Add product', exact: true })).toBeDisabled()
})
test('confirmed disable reports backend stock conflicts', async ({ page }) => {
  const mutations = await mockStore(page, { conflict: true })
  await page.goto('/products')
  await page.getByRole('button', { name: 'Disable ' + product.displayName }).click()
  expect(mutations).toEqual([])
  await page.getByRole('button', { name: 'Cancel', exact: true }).click()
  expect(mutations).toEqual([])
  await page.getByRole('button', { name: 'Disable ' + product.displayName }).click()
  await page.getByRole('button', { name: 'Disable product', exact: true }).click()
  await expect(page.getByRole('alert')).toHaveText('SKU has inventory and cannot be disabled')
  expect(mutations).toEqual(['POST /api/v1/sku/p1/disable'])
})
test('category delete requires confirmation and successful mutations refresh', async ({ page }) => {
  const mutations = await mockStore(page)
  await page.goto('/categories')
  await page.getByRole('button', { name: 'Delete Chocolates' }).click()
  expect(mutations).toEqual([])
  await page.getByRole('button', { name: 'Delete category', exact: true }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Category deleted.' })).toContainText('Category deleted.')
  expect(mutations).toEqual(['DELETE /api/v1/categories/c1'])
})
test('create and edit supplier submit the existing API contract', async ({ page }) => {
  const mutations = await mockStore(page)
  await page.goto('/suppliers')
  await page.getByRole('button', { name: 'Add supplier', exact: true }).click()
  await page.getByLabel('Name *', { exact: true }).fill('Candy Supplier')
  await page.getByLabel('Mobile number *').fill('+91 9876543210')
  await page.getByRole('button', { name: 'Add supplier', exact: true }).last().click()
  await expect(page.getByRole('status').filter({ hasText: 'Supplier saved.' })).toContainText('Supplier saved.')
  await page.getByRole('button', { name: 'Edit Sweet Supply Co.' }).click()
  await page.getByLabel('Company', { exact: true }).fill('Updated company')
  await page.getByRole('button', { name: 'Save changes' }).click()
  await expect(page.getByRole('dialog')).not.toBeVisible()
  expect(mutations).toEqual(['POST /api/v1/suppliers', 'PUT /api/v1/suppliers/s1'])
})
test('all placeholders work on direct URLs without API requests', async ({ page }) => {
  const calls: string[] = []
  page.on('request', req => { if (req.url().includes('/api/')) calls.push(req.url()) })
  for (const [path, title] of [['sales', 'Sales'], ['purchase-orders', 'Purchase orders'], ['inventory-lots', 'Inventory lots'], ['stock-movements', 'Stock movements'], ['users', 'Users & access']]) {
    await page.goto('/' + path)
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible()
    await expect(page.getByText('Coming soon', { exact: true })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Back to products' })).toHaveAttribute('href', '/products')
    expect(calls).toEqual([])
  }
})
test('connection errors can be retried', async ({ page }) => {
  await page.route('**/api/v1/**', route => route.fulfill({ status: 502, body: 'Bad Gateway' }))
  await page.goto('/products')
  await expect(page.getByRole('alert')).toContainText('502')
  await page.unroute('**/api/v1/**')
  await mockStore(page)
  await page.getByRole('button', { name: 'Try again' }).click()
  await expect(page.getByText(product.displayName, { exact: true })).toBeVisible()
})
test('mobile layout and keyboard dialog focus remain usable', async ({ page }) => {
  await mockStore(page)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/products')
  await expect(page.getByText(product.displayName, { exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.keyboard.press('Tab')
  await expect(page.getByRole('link', { name: 'Skip to content' })).toBeFocused()
  await page.getByRole('button', { name: 'Add product', exact: true }).click()
  await page.getByLabel('Product name *').focus()
  await page.keyboard.press('Shift+Tab')
  await expect(page.getByRole('button', { name: 'Close dialog' })).toBeFocused()
  await page.keyboard.press('Shift+Tab')
  await expect(page.getByRole('button', { name: 'Add product', exact: true }).last()).toBeFocused()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('button', { name: 'Add product', exact: true })).toBeFocused()
  await page.screenshot({ path: 'test-results/mobile-products.png', fullPage: true })
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.screenshot({ path: 'test-results/desktop-products.png', fullPage: true })
})


test('product create and edit use real category and unit IDs', async ({ page }) => {
  await mockStore(page)
  const submitted: { method: string; body: unknown }[] = []
  page.on('request', req => {
    if (req.url().includes('/api/v1/sku') && ['POST', 'PUT'].includes(req.method())) submitted.push({ method: req.method(), body: req.postDataJSON() })
  })
  await page.goto('/products')
  await page.getByRole('button', { name: 'Add product', exact: true }).click()
  await page.getByLabel('Product name *').fill('Caramel bonbons')
  await page.getByLabel('Category *').selectOption('c1')
  await page.getByLabel('Unit *').selectOption('u1')
  await page.getByLabel('MRP (₹) *').fill('0')
  await page.getByRole('dialog').getByRole('button', { name: 'Add product', exact: true }).click()
  await expect(page.getByRole('dialog')).not.toBeVisible()
  await page.getByRole('button', { name: 'Edit ' + product.displayName }).click()
  await page.getByLabel('MRP (₹) *').fill('150.50')
  await page.getByRole('button', { name: 'Save changes' }).click()
  await expect(page.getByRole('dialog')).not.toBeVisible()
  expect(submitted).toEqual([
    { method: 'POST', body: { displayName: 'Caramel bonbons', categoryId: 'c1', unitId: 'u1', mrp: 0, barcode: null } },
    { method: 'PUT', body: { displayName: product.displayName, categoryId: 'c1', unitId: 'u1', mrp: 150.5, barcode: null } },
  ])
})
test('pending save prevents repeat submissions and dialog dismissal', async ({ page }) => {
  await mockStore(page)
  let release: () => void = () => {}
  const pending = new Promise<void>(resolve => { release = resolve })
  let calls = 0
  await page.route('**/api/v1/categories', async route => {
    if (route.request().method() === 'GET') return route.fulfill({ json: [category] })
    calls++
    await pending
    await route.fulfill({ status: 201, json: category })
  })
  await page.goto('/categories')
  await page.getByRole('button', { name: 'Add category', exact: true }).click()
  await page.getByLabel('Name *', { exact: true }).fill('Cookies')
  await page.getByRole('dialog').getByRole('button', { name: 'Add category', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Saving…' })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Close dialog' })).toBeDisabled()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).toBeVisible()
  expect(calls).toBe(1)
  release()
  await expect(page.getByRole('dialog')).not.toBeVisible()
})
