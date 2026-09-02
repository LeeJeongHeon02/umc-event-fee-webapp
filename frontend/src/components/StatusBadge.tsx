import type { EventStatus, PaymentStatus } from '../services/types'

const statusMeta: Record<PaymentStatus | EventStatus, { label: string; tone: string }> = {
  NOT_REQUIRED: { label: '납부 없음', tone: 'neutral' },
  UNPAID: { label: '미납', tone: 'warning' },
  REPORTED: { label: '확인 대기', tone: 'info' },
  CONFIRMED: { label: '납부 완료', tone: 'success' },
  REJECTED: { label: '확인 필요', tone: 'danger' },
  VOID: { label: '부과 취소', tone: 'neutral' },
  REFUND_PENDING: { label: '환불 대기', tone: 'warning' },
  REFUNDED: { label: '환불 완료', tone: 'neutral' },
  DRAFT: { label: '초안', tone: 'neutral' },
  PUBLISHED: { label: '공개', tone: 'success' },
  CLOSED: { label: '마감', tone: 'warning' },
  CANCELED: { label: '취소', tone: 'danger' },
}

export function StatusBadge({ status }: { status: PaymentStatus | EventStatus }) {
  const meta = statusMeta[status]
  return <span className={`status-badge status-badge--${meta.tone}`}>{meta.label}</span>
}
