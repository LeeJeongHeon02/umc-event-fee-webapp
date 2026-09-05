import type { ReactNode } from 'react'

type Props = {
  title: string
  description: ReactNode
  confirmLabel: string
  tone?: 'default' | 'danger'
  onCancel: () => void
  onConfirm: () => void
  pending?: boolean
}

export function ConfirmActionDialog({
  title, description, confirmLabel, tone = 'default', onCancel, onConfirm, pending = false,
}: Props) {
  return (
    <div className="confirm-dialog-backdrop" role="presentation">
      <section className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title">
        <h2 id="confirm-dialog-title">{title}</h2>
        <div className="confirm-dialog__description">{description}</div>
        <div className="confirm-dialog__actions">
          <button className="secondary-button" type="button" onClick={onCancel} disabled={pending}>취소</button>
          <button className={tone === 'danger' ? 'danger-button' : 'primary-button'} type="button" onClick={onConfirm} disabled={pending}>
            {pending ? '처리 중…' : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  )
}
