import { useRef, useState, type FormEvent } from 'react'
import Dialog from './Dialog'
import { request, paths } from './api'
import { normalizePhone, validPhone } from './domain'
import type { Category, Product, Supplier, Resource, RecordItem, Unit } from './types'

export default function RecordForm({ resource, record, categories, units, onClose, onSaved }: {
  resource: Resource; record?: RecordItem; categories: Category[]; units: Unit[]; onClose: () => void; onSaved: () => void
}) {
  const product = resource === 'products' ? record as Product | undefined : undefined
  const supplier = resource === 'suppliers' ? record as Supplier | undefined : undefined
  const [fields, setFields] = useState({
    name: product?.displayName ?? (record as Category | Supplier | undefined)?.name ?? '',
    description: (resource === 'categories' ? (record as Category | undefined)?.description : '') ?? '',
    categoryId: product?.category.id ?? '', unitId: product?.unit.id ?? '',
    mrp: product ? String(product.mrp) : '', barcode: product?.barcode ?? '',
    contact: supplier?.contact ?? '', corpName: supplier?.corpName ?? '', pocName: supplier?.pocName ?? '',
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const submitting = useRef(false)
  const singular = { products: 'product', categories: 'category', suppliers: 'supplier' }[resource]
  const update = (key: keyof typeof fields, value: string) => setFields(old => ({ ...old, [key]: value }))
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (submitting.current) return
    if (!fields.name.trim()) { setError('Please enter a name.'); return }
    if (resource === 'products' && (!fields.categoryId || !fields.unitId || fields.mrp.trim() === '' || !Number.isFinite(Number(fields.mrp)) || Number(fields.mrp) < 0)) {
      setError('Choose a category and unit, and enter an MRP of zero or more.'); return
    }
    if (resource === 'suppliers' && !validPhone(fields.contact)) { setError('Enter a valid Indian mobile number (10 digits starting with 6–9).'); return }
    const body = resource === 'products'
      ? { displayName: fields.name.trim(), categoryId: fields.categoryId, unitId: fields.unitId, mrp: Number(fields.mrp), barcode: fields.barcode.trim() || null }
      : resource === 'categories'
      ? { name: fields.name.trim(), description: fields.description.trim() }
      : { name: fields.name.trim(), contact: normalizePhone(fields.contact), corpName: fields.corpName.trim(), pocName: fields.pocName.trim() }
    submitting.current = true
    setBusy(true); setError('')
    try {
      await request(paths[resource] + (record ? '/' + record.id : ''), { method: record ? 'PUT' : 'POST', body: JSON.stringify(body) })
      onSaved()
    } catch (cause) { setError(cause instanceof Error ? cause.message : 'Could not save. Please retry.') }
    finally { submitting.current = false; setBusy(false) }
  }
  return <Dialog title={(record ? 'Edit ' : 'Add ') + singular} onClose={onClose} busy={busy}>
    <form onSubmit={submit} noValidate>
      <p className="form-intro">Keep your {singular} details up to date. Fields marked * are required.</p>
      {error && <div className="alert error" role="alert">{error}</div>}
      <fieldset disabled={busy}>
        <label>{resource === 'products' ? 'Product name' : 'Name'} *<input autoFocus required value={fields.name} onChange={e => update('name', e.target.value)} placeholder={resource === 'products' ? 'e.g. Dark chocolate truffles' : singular === 'category' ? 'e.g. Chocolates' : 'e.g. Sweet Supply Co.'} /></label>
        {resource === 'products' && <>
          <div className="form-grid">
            <label>Category *<select required value={fields.categoryId} onChange={e => update('categoryId', e.target.value)}><option value="">Choose category</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
            <label>Unit *<select required value={fields.unitId} onChange={e => update('unitId', e.target.value)}><option value="">Choose unit</option>{units.map(u => <option key={u.id} value={u.id}>{u.name} ({u.code})</option>)}</select></label>
          </div>
          <div className="form-grid">
            <label>MRP (₹) *<input type="number" inputMode="decimal" min="0" step="any" required value={fields.mrp} onChange={e => update('mrp', e.target.value)} placeholder="0.00" /></label>
            <label>Barcode<input value={fields.barcode} onChange={e => update('barcode', e.target.value)} placeholder="Optional" /></label>
          </div>
          <p className="field-note">Product codes are assigned automatically. Stock quantities are read-only.</p>
        </>}
        {resource === 'categories' && <label>Description<textarea rows={3} value={fields.description} onChange={e => update('description', e.target.value)} placeholder="A short description (optional)" /></label>}
        {resource === 'suppliers' && <>
          <label>Mobile number *<input type="tel" required value={fields.contact} onChange={e => update('contact', e.target.value)} placeholder="e.g. +91 98765 43210" /></label>
          <div className="form-grid"><label>Company<input value={fields.corpName} onChange={e => update('corpName', e.target.value)} placeholder="Optional" /></label><label>Contact person<input value={fields.pocName} onChange={e => update('pocName', e.target.value)} placeholder="Optional" /></label></div>
        </>}
      </fieldset>
      <div className="dialog-actions"><button type="button" className="button secondary" onClick={onClose} disabled={busy}>Cancel</button><button className="button primary" disabled={busy}>{busy ? 'Saving…' : record ? 'Save changes' : 'Add ' + singular}</button></div>
    </form>
  </Dialog>
}

