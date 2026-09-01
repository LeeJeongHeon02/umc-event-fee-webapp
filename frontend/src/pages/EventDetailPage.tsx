import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { StatusBadge } from '../components/StatusBadge'
import { getEvent, joinEvent } from '../services/api'
import { formatDateTime, formatWon } from '../services/format'

export function EventDetailPage() {
  const eventId = Number(useParams().eventId)
  const queryClient = useQueryClient()
  const eventQuery = useQuery({
    queryKey: ['event', eventId],
    queryFn: () => getEvent(eventId),
    enabled: Number.isFinite(eventId),
  })
  const joinMutation = useMutation({
    mutationFn: () => joinEvent(eventId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['event', eventId] })
      void queryClient.invalidateQueries({ queryKey: ['events'] })
      void queryClient.invalidateQueries({ queryKey: ['my-payments'] })
    },
  })

  if (eventQuery.isLoading) return <LoadingState label="행사 정보를 불러오는 중" />
  if (eventQuery.isError || !eventQuery.data) return <ErrorState title="행사를 찾지 못했어요" />

  const event = eventQuery.data

  return (
    <div className="page detail-page">
      <Link className="back-link" to="/home">← 홈으로</Link>
      <section className="event-hero">
        <div className="event-hero__meta">
          <span className="eyebrow">CLUB EVENT</span>
          {event.myPaymentStatus && <StatusBadge status={event.myPaymentStatus} />}
        </div>
        <h1>{event.title}</h1>
        <p>{event.summary}</p>
      </section>

      <section className="info-grid" aria-label="행사 정보">
        <div><span>일시</span><strong>{formatDateTime(event.startsAt)}</strong></div>
        <div><span>장소</span><strong>{event.location ?? '추후 안내'}</strong></div>
        <div><span>참가비</span><strong>{formatWon(event.feeAmount)}</strong></div>
        <div><span>참가 현황</span><strong>{event.joinedCount}{event.capacity ? ` / ${event.capacity}` : ''}명</strong></div>
      </section>

      <section className="content-section">
        <span className="eyebrow">ABOUT</span>
        <h2>행사 안내</h2>
        <p>{event.description}</p>
      </section>

      <section className="deadline-callout">
        <div><span>신청 마감</span><strong>{formatDateTime(event.registrationDeadline)}</strong></div>
        <span aria-hidden="true">⌛</span>
      </section>

      {joinMutation.isError && <p className="form-error" role="alert">참가 신청을 처리하지 못했어요. 다시 시도해 주세요.</p>}

      <div className="sticky-action">
        {event.myParticipation?.status === 'JOINED' ? (
          event.myPayment && event.myPayment.status !== 'NOT_REQUIRED' ? (
            <Link className="primary-button primary-button--block" to={`/payments/${event.myPayment.id}`}>
              {event.myPayment.status === 'UNPAID' ? '참가비 송금하기' : '납부 상태 확인하기'}
            </Link>
          ) : (
            <button className="secondary-button secondary-button--block" type="button" disabled>참가 신청 완료</button>
          )
        ) : (
          <button
            className="primary-button primary-button--block"
            type="button"
            disabled={!event.canJoin || joinMutation.isPending}
            onClick={() => joinMutation.mutate()}
          >
            {joinMutation.isPending ? '신청 중…' : '참가 신청하기'}
          </button>
        )}
      </div>
    </div>
  )
}

