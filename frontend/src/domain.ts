import type { Product } from './types'

export const money = (value: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value)
export const quantity = (value: number) => new Intl.NumberFormat('en-IN', { maximumFractionDigits: 6 }).format(value)
export const unitsFromProducts = (products: Product[]) => [...new Map(products.map(p => [p.unit.id, p.unit])).values()].sort((a, b) => a.name.localeCompare(b.name))
export function normalizePhone(value: string) {
  let compact = value.trim().replace(/[\s()-]/g, '')
  if (compact.startsWith('+91')) compact = compact.slice(3)
  else if (compact.startsWith('91') && compact.length === 12) compact = compact.slice(2)
  return compact
}
export const validPhone = (value: string) => /^[6-9]\d{9}$/.test(normalizePhone(value))
