import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { EventCard } from '../components/EventCard'
import { PaymentCard } from '../components/PaymentCard'
import { getEvents, getMe, getMyPayments } from '../services/api'

export function HomePage() {
  const meQuery = useQuery({ queryKey: ['me'], queryFn: getMe })
  const eventsQuery = useQuery({ queryKey: ['events'], queryFn: getEvents })
  const paymentsQuery = useQuery({ queryKey: ['my-payments'], queryFn: getMyPayments })

  if (meQuery.isLoading || eventsQuery.isLoading || paymentsQuery.isLoading) {
    return <LoadingState label="동아리 소식을 불러오는 중" />
  }
  if (meQuery.isError || eventsQuery.isError || paymentsQuery.isError) {
    return <ErrorState action={<button className="secondary-button" onClick={() => window.location.reload()}>다시 시도</button>} />
  }

  const me = meQuery.data!
  const events = eventsQuery.data!.items
  const payments = paymentsQuery.data!.items
  const upcomingEvents = events.filter((event) => Date.parse(event.endsAt ?? event.startsAt) >= Date.now())
    .sort((a, b) => Date.parse(a.startsAt) - Date.parse(b.startsAt))
  const priorityPayments = payments.filter((payment) => ['UNPAID', 'REJECTED'].includes(payment.status))
  const reportedPayments = payments.filter((payment) => payment.status === 'REPORTED')
  const joinedEventCount = events.filter((event) => event.myParticipationStatus === 'JOINED').length
  const reportedPaymentCount = payments.filter((payment) => payment.status === 'REPORTED').length
  const today = new Date()
  const todayLabel = today.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })

  return (
    <div className="page home-page">
      <section className="home-welcome" aria-labelledby="home-title">
        <div className="home-welcome__copy">
          <span className="member-label">{me.displayNickname}</span>
          <h1 id="home-title">안녕하세요, <strong>{me.name}</strong>님.</h1>
          <p>행사 일정과 납부 현황을 한눈에 확인하세요.</p>
        </div>
        <time className="home-date" dateTime={today.toISOString().slice(0, 10)}>
          <span>오늘</span>
          <strong>{todayLabel}</strong>
        </time>
      </section>

      {priorityPayments.length > 0 && (
        <section className="home-priority" aria-labelledby="priority-payment-title">
          <div>
            <span className="eyebrow">납부 필요</span>
            <h2 id="priority-payment-title">{priorityPayments.length}건의 납부를 확인해 주세요.</h2>
            <p>{priorityPayments[0].source.title} 외 {priorityPayments.length - 1}건이 납부 또는 재확인을 기다리고 있어요.</p>
          </div>
          <Link className="brand-button" to="/payments?status=NEEDS_PAYMENT">납부 내역 보기</Link>
        </section>
      )}

      <section className="page-section" id="events">
        <div className="section-heading">
          <div><span className="eyebrow">일정</span><h2>다가오는 행사</h2></div>
          <Link className="section-meta" to="/events">전체보기</Link>
        </div>
        {upcomingEvents.length > 0 ? (
          <div className="stack-list">
            {upcomingEvents.slice(0, 3).map((event) => <EventCard key={event.id} event={event} />)}
          </div>
        ) : (
          <div className="home-empty"><strong>예정된 행사가 없어요.</strong><span>새 행사가 등록되면 여기에서 알려드릴게요.</span></div>
        )}
      </section>

      <section className="home-overview" aria-label="내 활동 요약">
        <Link to="/events?participation=JOINED" className="overview-item overview-item--events">
          <span className="overview-item__label">참여 중인 행사</span>
          <strong>{joinedEventCount}<small>건</small></strong>
          <span className="overview-item__hint">일정 확인</span>
        </Link>
        <Link to="/payments?status=REPORTED" className="overview-item">
          <span className="overview-item__label">입금 확인 대기</span>
          <strong>{reportedPaymentCount}<small>건</small></strong>
          <span className="overview-item__hint">처리 중</span>
        </Link>
      </section>

      <section className="page-section" id="payments">
        <div className="section-heading">
          <div><span className="eyebrow">납부</span><h2>확인이 필요한 내역</h2></div>
          <Link className="section-meta" to="/payments">전체보기</Link>
        </div>
        {reportedPayments.length > 0 ? (
          <div className="stack-list">
            {reportedPayments.slice(0, 3).map((payment) => <PaymentCard key={payment.id} payment={payment} />)}
          </div>
        ) : (
          <div className="home-empty"><strong>확인 대기 중인 납부가 없어요.</strong><span>송금 신고를 하면 여기에서 진행 상태를 알려드릴게요.</span></div>
        )}
      </section>

    </div>
  )
}
