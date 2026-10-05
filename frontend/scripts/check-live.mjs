import { chromium } from '@playwright/test'
import { mkdir } from 'node:fs/promises'

// Read-only smoke check against the user's manually started backend.
const browser = await chromium.launch({ headless: true })
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
  const errors = []
  const failed = []
  page.on('pageerror', error => errors.push(error.message))
  page.on('response', response => { if (response.url().includes('/api/') && !response.ok()) failed.push(response.status()) })
  await page.route('**/api/**', route => route.request().method() === 'GET' ? route.continue() : route.abort())
  await mkdir('test-results', { recursive: true })
  for (const resource of ['products', 'categories', 'suppliers']) {
    await page.goto('http://127.0.0.1:5173/' + resource)
    await page.getByRole('button', { name: 'Refresh list' }).waitFor()
    await page.waitForFunction(() => {
      const button = document.querySelector('[aria-label="Refresh list"]')
      return button && !button.disabled
    })
    if (await page.getByRole('alert').count()) throw new Error(await page.getByRole('alert').innerText())
    const rows = await page.locator('tbody tr').count()
    console.log(resource + ': ' + rows + ' live records displayed')
    if (await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)) throw new Error('Desktop page overflow: ' + resource)
    await page.screenshot({ path: 'test-results/live-' + resource + '.png', fullPage: true })
  }
  await page.goto('http://127.0.0.1:5173/sales')
  await page.getByText('Coming soon', { exact: true }).waitFor()
  await page.screenshot({ path: 'test-results/live-placeholder.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('http://127.0.0.1:5173/products')
  await page.waitForFunction(() => !document.querySelector('[aria-label="Refresh list"]')?.disabled)
  await page.screenshot({ path: 'test-results/live-mobile.png', fullPage: true })
  if (await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)) throw new Error('Mobile page overflow')
  if (errors.length || failed.length) throw new Error(JSON.stringify({ errors, failed }))
  console.log('Live routes, API responses, desktop/mobile overflow, and browser console passed.')
} finally { await browser.close() }
