import { Link } from 'react-router-dom'
import { formatDateTime, formatWon } from '../services/format'
import type { PaymentSummary } from '../services/types'
import { StatusBadge } from './StatusBadge'

export function PaymentCard({ payment }: { payment: PaymentSummary }) {
  return (
    <Link className="payment-card" to={`/payments/${payment.id}`}>
      <div>
        <div className="payment-card__label">
          <span>{payment.type === 'EVENT_FEE' ? '행사 참가비' : '정기 회비'}</span>
          <StatusBadge status={payment.status} />
        </div>
        <h3>{payment.source.title}</h3>
        <p>{payment.dueAt ? `${formatDateTime(payment.dueAt)}까지` : '납부 기한 없음'}</p>
      </div>
      <strong className="payment-card__amount">{formatWon(payment.amount)}</strong>
    </Link>
  )
}

