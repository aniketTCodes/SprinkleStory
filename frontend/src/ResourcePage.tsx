import { useEffect, useRef, useState } from 'react'
import { AlertCircle, ArrowDown, Ban, CircleCheck, Package, Pencil, Plus, RefreshCw, Search, Tags, Trash2, Truck } from 'lucide-react'
import { api, paths, request } from './api'
import { money, quantity, unitsFromProducts } from './domain'
import type { Category, Product, Supplier, Resource, RecordItem } from './types'
import RecordForm from './RecordForm'
import Dialog from './Dialog'

const details = {
  products: { title: 'Products', singular: 'product', description: 'Every sweet thing, accounted for.', icon: Package },
  categories: { title: 'Categories', singular: 'category', description: 'A place for everything on your shelves.', icon: Tags },
  suppliers: { title: 'Suppliers', singular: 'supplier', description: 'The people who keep your shelves full.', icon: Truck },
}
type Data = { rows: RecordItem[]; products: Product[]; categories: Category[] }
export default function ResourcePage({ resource }: { resource: Resource }) {
  const meta = details[resource]
  const Icon = meta.icon
  const [data, setData] = useState<Data>({ rows: [], products: [], categories: [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [refresh, setRefresh] = useState(0)
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [edit, setEdit] = useState<{ record?: RecordItem } | null>(null)
  const [confirm, setConfirm] = useState<RecordItem | null>(null)
  const [actionError, setActionError] = useState('')
  const [busy, setBusy] = useState(false)
  const acting = useRef(false)
  useEffect(() => {
    const controller = new AbortController()
    const timer = window.setTimeout(async () => {
      setLoading(true); setError('')
      try {
        if (resource === 'products') {
          const all = api.products({}, controller.signal)
          const filtered = q.trim() || status || categoryId ? api.products({ q: q.trim(), status, categoryId }, controller.signal) : all
          const [rows, products, categories] = await Promise.all([filtered, all, api.categories(controller.signal)])
          if (!controller.signal.aborted) setData({ rows, products, categories })
        } else {
          const rows = await api[resource](controller.signal)
          if (!controller.signal.aborted) setData({ rows, products: [], categories: [] })
        }
      } catch (cause) {
        if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : 'Could not load your store data.')
      } finally { if (!controller.signal.aborted) setLoading(false) }
    }, q ? 250 : 0)
    return () => { window.clearTimeout(timer); controller.abort() }
  }, [resource, q, status, categoryId, refresh])
  const units = unitsFromProducts(data.products)
  const createReason = resource === 'products'
    ? !data.categories.length ? 'Add a category before creating a product.'
    : !units.length ? 'No units are available from existing products. Product creation will be available when the catalog contains a unit.' : ''
    : ''
  const rows = resource === 'products' ? data.rows : data.rows.filter(row => Object.values(row).some(value => typeof value === 'string' && value.toLowerCase().includes(q.trim().toLowerCase())))
  function saved(message: string) { setEdit(null); setConfirm(null); setNotice(message); setRefresh(value => value + 1) }
  async function destroy() {
    if (!confirm || acting.current) return
    acting.current = true; setBusy(true); setActionError('')
    try {
      await request(paths[resource] + '/' + confirm.id + (resource === 'products' ? '/disable' : ''), { method: resource === 'products' ? 'POST' : 'DELETE' })
      saved(meta.singular[0].toUpperCase() + meta.singular.slice(1) + (resource === 'products' ? ' disabled.' : ' deleted.'))
    } catch (cause) { setActionError(cause instanceof Error ? cause.message : 'Could not complete this action.') }
    finally { acting.current = false; setBusy(false) }
  }
  const active = data.products.filter(p => p.status === 'ACTIVE')
  const nameOf = (record: RecordItem) => 'displayName' in record ? record.displayName : record.name
  return <div className="page">
    <div className="eyebrow">YOUR STORE, AT A GLANCE</div>
    <header className="page-header"><div><h1>{meta.title}<span className="title-dot">.</span></h1><p>{meta.description}</p></div><button className="button primary" onClick={() => { setNotice(''); setEdit({}) }} disabled={loading || Boolean(error) || Boolean(createReason)}><Plus size={18} />Add {meta.singular}</button></header>
    {notice && <div className="alert success" role="status"><CircleCheck size={18} />{notice}<button className="text-button" onClick={() => setNotice('')}>Dismiss</button></div>}
    {resource === 'products' && <section className="stats" aria-label="Catalog overview">
      <div className="stat"><span className="stat-label">TOTAL PRODUCTS<Package size={18} /></span><strong>{loading || error ? '—' : data.products.length}</strong><small>Your complete catalog</small></div>
      <div className="stat"><span className="stat-label">ACTIVE PRODUCTS<CircleCheck size={18} /></span><strong>{loading || error ? '—' : active.length}</strong><small>Available in your catalog</small></div>
      <div className="stat"><span className="stat-label">OUT OF STOCK<Package size={18} /></span><strong>{loading || error ? '—' : active.filter(p => p.onHandQty <= 0).length}</strong><small>Active products with no stock</small></div>
      <div className="stat"><span className="stat-label">CATEGORIES<Tags size={18} /></span><strong>{loading || error ? '—' : data.categories.length}</strong><small>A little organization goes a long way</small></div>
    </section>}
    {createReason && !loading && !error && <div className="alert info"><AlertCircle size={18} />{createReason}</div>}
    <section className="panel catalog" aria-label={meta.title + ' list'}>
      <div className="panel-heading"><h2>{resource === 'products' ? 'Your product catalog' : 'Your ' + resource}</h2><span className="panel-caption">{resource === 'products' ? 'Small details. Smooth days.' : 'Keep the essentials close.'}</span></div>
      <div className="toolbar">
        <div className="search-field"><Search size={18} /><input aria-label={'Search ' + resource} value={q} onChange={e => setQ(e.target.value)} placeholder={resource === 'products' ? 'Search name, product code or barcode…' : 'Search ' + resource + '…'} /></div>
        {resource === 'products' && <><select aria-label="Filter by category" value={categoryId} onChange={e => setCategoryId(e.target.value)}><option value="">All categories</option>{data.categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select><select aria-label="Filter by status" value={status} onChange={e => setStatus(e.target.value)}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select></>}
        <button className="button secondary refresh-button" onClick={() => setRefresh(v => v + 1)} disabled={loading} aria-label="Refresh list"><RefreshCw size={17} /><span>Refresh</span></button>
      </div>
      {loading ? <div className="empty-state" role="status"><span className="loader" /><h3>Gathering your {resource}…</h3><p>Just a moment while we check the shelves.</p></div>
      : error ? <div className="empty-state"><div className="empty-icon"><AlertCircle size={28} /></div><h3>We couldn’t load your {resource}</h3><p role="alert">{error}</p><button className="button secondary" onClick={() => setRefresh(v => v + 1)}><RefreshCw size={16} />Try again</button></div>
      : !rows.length ? <div className="empty-state"><div className="empty-icon"><Icon size={29} /></div><h3>{q || status || categoryId ? 'No matches on these shelves' : 'A fresh start for your ' + resource}</h3><p>{q || status || categoryId ? 'Try another search or clear your filters.' : 'Your ' + resource + ' will appear here once added.'}</p>{q || status || categoryId ? <button className="button secondary" onClick={() => { setQ(''); setStatus(''); setCategoryId('') }}>Clear filters</button> : <button className="button secondary" disabled={Boolean(createReason)} onClick={() => setEdit({})}><Plus size={16} />Add {meta.singular}</button>}</div>
      : <div className="table-scroll" tabIndex={0} role="region" aria-label={meta.title + ' table, scroll horizontally on small screens'}><table>
        <thead><tr>{resource === 'products' ? <><th scope="col">PRODUCT <ArrowDown size={12} /></th><th scope="col">CATEGORY</th><th scope="col">UNIT</th><th scope="col" className="numeric">MRP</th><th scope="col" className="numeric">ON HAND</th><th scope="col">STATUS</th></> : resource === 'categories' ? <><th scope="col">CATEGORY</th><th scope="col">DESCRIPTION</th></> : <><th scope="col">SUPPLIER</th><th scope="col">COMPANY</th><th scope="col">CONTACT PERSON</th><th scope="col">MOBILE</th></>}<th scope="col" className="actions-heading">ACTIONS</th></tr></thead>
        <tbody>{rows.map(row => <tr key={row.id}>
          {resource === 'products' ? (() => { const p = row as Product; return <><td><div className="product-cell"><span className="product-avatar">{p.displayName.slice(0, 1).toUpperCase()}</span><div><strong>{p.displayName}</strong><small>{p.productCode}{p.barcode ? ' · ' + p.barcode : ''}</small></div></div></td><td><span className="category-chip">{p.category.name}</span></td><td>{p.unit.name}<small className="cell-sub">{p.unit.code}</small></td><td className="numeric">{money(p.mrp)}</td><td className={'numeric stock ' + (p.onHandQty <= 0 ? 'zero-stock' : '')}>{quantity(p.onHandQty)} <span>{p.unit.code}</span></td><td><span className={'badge ' + (p.status === 'ACTIVE' ? 'active-badge' : 'inactive-badge')}><span className="badge-dot" />{p.status === 'ACTIVE' ? 'Active' : 'Inactive'}</span></td></> })()
          : resource === 'categories' ? <><td><strong>{(row as Category).name}</strong></td><td className="description-cell">{(row as Category).description || '—'}</td></>
          : <><td><strong>{(row as Supplier).name}</strong></td><td>{(row as Supplier).corpName || '—'}</td><td>{(row as Supplier).pocName || '—'}</td><td>{(row as Supplier).contact}</td></>}
          <td><div className="row-actions"><button className="icon-button" aria-label={'Edit ' + nameOf(row)} onClick={() => { setNotice(''); setEdit({ record: row }) }}><Pencil size={16} /></button><button className="icon-button danger-icon" aria-label={(resource === 'products' ? 'Disable ' : 'Delete ') + nameOf(row)} disabled={resource === 'products' && (row as Product).status === 'INACTIVE'} onClick={() => { setActionError(''); setConfirm(row) }}>{resource === 'products' ? <Ban size={16} /> : <Trash2 size={16} />}</button></div></td>
        </tr>)}</tbody>
      </table></div>}
      {!loading && !error && <div className="table-footer"><span>{rows.length} {rows.length === 1 ? meta.singular : resource}{resource === 'products' && (q || status || categoryId) ? ' matching your filters' : ' in your list'}</span><span>{resource === 'products' ? 'Stock is shown in each product’s unit' : 'A well-kept store starts here'}</span></div>}
    </section>
    {resource === 'products' && <div className="page-note"><span className="note-line" />Good things come in small batches. Keep yours in check.</div>}
    {edit && <RecordForm resource={resource} record={edit.record} categories={data.categories} units={units} onClose={() => setEdit(null)} onSaved={() => saved(meta.singular[0].toUpperCase() + meta.singular.slice(1) + ' saved.')} />}
    {confirm && <Dialog title={(resource === 'products' ? 'Disable ' : 'Delete ') + meta.singular + '?'} onClose={() => setConfirm(null)} busy={busy}>
      <p className="confirm-copy">{resource === 'products' ? <>Disable <strong>{nameOf(confirm)}</strong>? Products with remaining stock cannot be disabled. There is currently no reactivation action.</> : <>Delete <strong>{nameOf(confirm)}</strong>? This cannot be undone. Records in use cannot be deleted.</>}</p>
      {actionError && <div className="alert error" role="alert">{actionError}</div>}
      <div className="dialog-actions"><button autoFocus className="button secondary" onClick={() => setConfirm(null)} disabled={busy}>Cancel</button><button className="button danger" onClick={destroy} disabled={busy}>{busy ? 'Working…' : (resource === 'products' ? 'Disable ' : 'Delete ') + meta.singular}</button></div>
    </Dialog>}
  </div>
}

