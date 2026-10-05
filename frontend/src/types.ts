export interface Category { id: string; name: string; description: string }
export interface Unit { id: string; code: string; name: string }
export interface Product {
  id: string
  productCode: string
  displayName: string
  category: Pick<Category, 'id' | 'name'>
  unit: Unit
  mrp: number
  status: 'ACTIVE' | 'INACTIVE'
  barcode: string | null
  onHandQty: number
}
export interface Supplier { id: string; name: string; contact: string; corpName: string; pocName: string }
export interface ProductInput { displayName: string; categoryId: string; unitId: string; mrp: number; barcode: string | null }
export type CategoryInput = Omit<Category, 'id'>
export type SupplierInput = Omit<Supplier, 'id'>
export type Resource = 'products' | 'categories' | 'suppliers'
export type RecordItem = Product | Category | Supplier
