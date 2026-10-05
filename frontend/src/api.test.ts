import { describe, expect, it, vi } from 'vitest'
import { api, request } from './api'
describe('API client', () => {
  it('passes product filters to the backend and supports abort signals', async () => {
    const fetcher = vi.fn().mockResolvedValue(new Response('[]'))
    vi.stubGlobal('fetch', fetcher)
    const controller = new AbortController()
    await api.products({ q: 'cream & sugar', status: 'ACTIVE', categoryId: 'c1' }, controller.signal)
    expect(fetcher).toHaveBeenCalledWith('/api/v1/sku?q=cream+%26+sugar&status=ACTIVE&categoryId=c1', expect.objectContaining({ signal: controller.signal }))
  })
  it('handles a 204 response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })))
    await expect(request('/categories/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })
  it('surfaces backend conflicts', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: 'Category is in use and cannot be deleted' }), { status: 409 })))
    await expect(request('/categories/1')).rejects.toThrow('Category is in use')
  })
  it('handles proxy errors and malformed successful responses', async () => {
    const fetcher = vi.fn().mockResolvedValueOnce(new Response('Bad Gateway', { status: 502 })).mockResolvedValueOnce(new Response('<html>'))
    vi.stubGlobal('fetch', fetcher)
    await expect(request('/sku')).rejects.toThrow('502')
    await expect(request('/sku')).rejects.toThrow('unreadable response')
  })
  it('explains connection failures while preserving aborts', async () => {
    const fetcher = vi.fn().mockRejectedValueOnce(new TypeError('Failed to fetch')).mockRejectedValueOnce(new DOMException('Aborted', 'AbortError'))
    vi.stubGlobal('fetch', fetcher)
    await expect(request('/sku')).rejects.toThrow('backend is running')
    await expect(request('/sku')).rejects.toMatchObject({ name: 'AbortError' })
  })
})

