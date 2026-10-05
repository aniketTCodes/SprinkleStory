import { describe, expect, it } from 'vitest'
import { unitsFromProducts, validPhone, normalizePhone, money, quantity } from './domain'
import type { Product } from './types'
describe('store formatting and validation', () => {
  it('deduplicates real units without inventing defaults', () => {
    const unit = { id: 'real-unit', code: 'PCS', name: 'Piece' }
    expect(unitsFromProducts([{ unit }, { unit }] as Product[])).toEqual([unit])
    expect(unitsFromProducts([])).toEqual([])
  })
  it('accepts supported Indian mobile formats and rejects invalid numbers', () => {
    for (const phone of ['9876543210', '+91 (98765) 43210', '91-9876543210']) expect(validPhone(phone)).toBe(true)
    for (const phone of ['', '1234567890', '+1 9876543210', '98765']) expect(validPhone(phone)).toBe(false)
    expect(normalizePhone('+91 98765-43210')).toBe('9876543210')
  })
  it('formats INR and fractional quantities', () => {
    expect(money(125)).toContain('₹125.00')
    expect(quantity(2.125)).toBe('2.125')
  })
})

