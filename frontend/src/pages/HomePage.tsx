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
  const actionablePayments = payments.filter((payment) => ['UNPAID', 'REJECTED', 'REPORTED'].includes(payment.status))

  return (
    <div className="page home-page">
      <section className="home-hero">
        <div>
          <span className="eyebrow">{me.displayNickname}</span>
          <h1>안녕하세요, {me.name}님.<br />이번 주도 같이 만들어봐요.</h1>
        </div>
        <div className="hero-shape" aria-hidden="true"><span>D:</span></div>
      </section>

      <section className="summary-strip" aria-label="내 활동 요약">
        <div><strong>{events.filter((event) => event.myParticipationStatus === 'JOINED').length}</strong><span>신청 행사</span></div>
        <div><strong>{payments.filter((payment) => payment.status === 'REPORTED').length}</strong><span>확인 대기</span></div>
        <div><strong>{payments.filter((payment) => payment.status === 'UNPAID').length}</strong><span>미납</span></div>
      </section>

      {actionablePayments.length > 0 && (
        <section className="page-section" id="payments">
          <div className="section-heading">
            <div><span className="eyebrow">PAYMENT</span><h2>확인이 필요한 납부</h2></div>
            <span className="section-count">{actionablePayments.length}</span>
          </div>
          <div className="stack-list">
            {actionablePayments.map((payment) => <PaymentCard key={payment.id} payment={payment} />)}
          </div>
        </section>
      )}

      <section className="page-section" id="events">
        <div className="section-heading">
          <div><span className="eyebrow">UPCOMING</span><h2>다가오는 행사</h2></div>
          <Link className="text-link" to="/home#events">전체보기</Link>
        </div>
        <div className="stack-list">
          {events.map((event) => <EventCard key={event.id} event={event} />)}
        </div>
      </section>
    </div>
  )
}
