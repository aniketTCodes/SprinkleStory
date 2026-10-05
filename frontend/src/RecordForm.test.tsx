import { expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import RecordForm from './RecordForm'
it('validates required fields and preserves supplier input on server error', async () => {
  const user = userEvent.setup()
  const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: 'Supplier already exist!' }), { status: 409 }))
  vi.stubGlobal('fetch', fetcher)
  render(<RecordForm resource="suppliers" categories={[]} units={[]} onClose={vi.fn()} onSaved={vi.fn()} />)
  await user.click(screen.getByRole('button', { name: 'Add supplier' }))
  expect(screen.getByRole('alert')).toHaveTextContent('enter a name')
  await user.type(screen.getByLabelText('Name *'), 'Sweet Supply')
  await user.type(screen.getByLabelText('Mobile number *'), '123')
  await user.click(screen.getByRole('button', { name: 'Add supplier' }))
  expect(fetcher).not.toHaveBeenCalled()
  await user.clear(screen.getByLabelText('Mobile number *'))
  await user.type(screen.getByLabelText('Mobile number *'), '+91 9876543210')
  await user.click(screen.getByRole('button', { name: 'Add supplier' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Supplier already exist!')
  expect(screen.getByLabelText('Name *')).toHaveValue('Sweet Supply')
  expect(fetcher).toHaveBeenCalledWith('/api/v1/suppliers', expect.objectContaining({ body: JSON.stringify({ name: 'Sweet Supply', contact: '9876543210', corpName: '', pocName: '' }) }))
})
it('requires product category, unit, and a nonnegative MRP', async () => {
  const user = userEvent.setup()
  const fetcher = vi.fn()
  vi.stubGlobal('fetch', fetcher)
  render(<RecordForm resource="products" categories={[{ id: 'c', name: 'Chocolate', description: '' }]} units={[{ id: 'u', code: 'PCS', name: 'Piece' }]} onClose={vi.fn()} onSaved={vi.fn()} />)
  await user.type(screen.getByLabelText('Product name *'), 'Truffle')
  await user.selectOptions(screen.getByLabelText('Category *'), 'c')
  await user.selectOptions(screen.getByLabelText('Unit *'), 'u')
  await user.type(screen.getByLabelText('MRP (₹) *'), '-1')
  await user.click(screen.getByRole('button', { name: 'Add product' }))
  expect(screen.getByRole('alert')).toHaveTextContent('zero or more')
  expect(fetcher).not.toHaveBeenCalled()
})

