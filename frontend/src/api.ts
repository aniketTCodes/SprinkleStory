import type { Category, Product, Supplier } from './types'

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(`/api/v1${path}`, {
      ...options,
      headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
    })
  } catch (error) {
    if (error && typeof error === 'object' && 'name' in error && error.name === 'AbortError') throw error
    throw new Error('Cannot connect to Sprinkle Story. Check that the backend is running, then retry.')
  }
  const text = await response.text()
  if (!response.ok) {
    let message = `Request failed (${response.status}). Please try again.`
    try { const body = JSON.parse(text); if (typeof body.message === 'string') message = body.message } catch { /* Proxy errors may not be JSON. */ }
    throw new Error(message)
  }
  if (!text.trim()) return undefined as T
  try { return JSON.parse(text) as T } catch { throw new Error('The server returned an unreadable response. Please retry.') }
}

export const paths = { products: '/sku', categories: '/categories', suppliers: '/suppliers' } as const
export const api = {
  products: (filters: { q?: string; status?: string; categoryId?: string } = {}, signal?: AbortSignal) => {
    const params = new URLSearchParams(Object.entries(filters).filter(([, value]) => Boolean(value)))
    return request<Product[]>(`/sku${params.size ? `?${params}` : ''}`, { signal })
  },
  categories: (signal?: AbortSignal) => request<Category[]>('/categories', { signal }),
  suppliers: (signal?: AbortSignal) => request<Supplier[]>('/suppliers', { signal }),
}

