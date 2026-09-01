import type { ReactNode } from 'react'

export function LoadingState({ label = '불러오는 중' }: { label?: string }) {
  return (
    <div className="state-box" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <p>{label}</p>
    </div>
  )
}

export function ErrorState({
  title = '정보를 불러오지 못했어요',
  description = '잠시 후 다시 시도해 주세요.',
  action,
}: {
  title?: string
  description?: string
  action?: ReactNode
}) {
  return (
    <div className="state-box state-box--error" role="alert">
      <div className="state-icon" aria-hidden="true">!</div>
      <strong>{title}</strong>
      <p>{description}</p>
      {action}
    </div>
  )
}

