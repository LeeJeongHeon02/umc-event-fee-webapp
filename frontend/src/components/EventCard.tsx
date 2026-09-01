import { Link } from 'react-router-dom'
import { formatShortDate, formatWon } from '../services/format'
import type { EventListItem } from '../services/types'
import { StatusBadge } from './StatusBadge'

export function EventCard({ event }: { event: EventListItem }) {
  return (
    <Link className="event-card" to={`/events/${event.id}`}>
      <div className="event-card__date" aria-label={formatShortDate(event.startsAt)}>
        <span>{new Date(event.startsAt).toLocaleDateString('ko-KR', { month: 'short', timeZone: 'Asia/Seoul' })}</span>
        <strong>{new Date(event.startsAt).toLocaleDateString('ko-KR', { day: '2-digit', timeZone: 'Asia/Seoul' })}</strong>
      </div>
      <div className="event-card__content">
        <div className="event-card__topline">
          <span className="event-card__fee">{formatWon(event.feeAmount)}</span>
          {event.myPaymentStatus && <StatusBadge status={event.myPaymentStatus} />}
        </div>
        <h3>{event.title}</h3>
        <p>{event.location ?? '장소 추후 안내'} · {event.joinedCount}명 참가</p>
      </div>
      <span className="event-card__arrow" aria-hidden="true">→</span>
    </Link>
  )
}

