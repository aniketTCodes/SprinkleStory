import { useEffect, useRef, type ReactNode, type KeyboardEvent } from 'react'
import { X } from 'lucide-react'

export default function Dialog({ title, children, onClose, busy = false }: { title: string; children: ReactNode; onClose: () => void; busy?: boolean }) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const dialog = ref.current!
    const previous = document.activeElement as HTMLElement | null
    dialog.showModal()
    return () => { dialog.close(); previous?.focus() }
  }, [])
  function containFocus(event: KeyboardEvent<HTMLDialogElement>) {
    if (event.key !== 'Tab') return
    const elements = [...event.currentTarget.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href], [tabindex="0"]')]
    const first = elements[0]
    const last = elements[elements.length - 1]
    if (!first) { event.preventDefault(); ref.current?.focus(); return }
    if (event.shiftKey && (document.activeElement === first || document.activeElement === ref.current)) {
      event.preventDefault(); last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault(); first.focus()
    }
  }
  return <dialog ref={ref} aria-labelledby="dialog-title" onKeyDown={containFocus} onCancel={event => { event.preventDefault(); if (!busy) onClose() }}>
    <div className="dialog-header"><h2 id="dialog-title">{title}</h2><button className="icon-button" type="button" onClick={onClose} disabled={busy} aria-label="Close dialog"><X size={20} /></button></div>
    {children}
  </dialog>
}
