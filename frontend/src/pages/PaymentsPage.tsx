import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { PaymentCard } from '../components/PaymentCard'
import { getMyPayments } from '../services/api'
import '../styles/member-lists.css'

const statuses = [
  ['ALL', '전체 상태'], ['NEEDS_PAYMENT', '납부 필요'], ['UNPAID', '미납'], ['REJECTED', '반려'],
  ['REPORTED', '확인 대기'], ['CONFIRMED', '납부 완료'], ['REFUND_PENDING', '환불 대기'],
  ['REFUNDED', '환불 완료'], ['VOID', '납부 취소'], ['NOT_REQUIRED', '납부 불필요'],
]

export function PaymentsPage() {
  const payments = useQuery({ queryKey: ['my-payments'], queryFn: getMyPayments })
  const [params, setParams] = useSearchParams()
  const search = params.get('q') ?? ''
  const status = statuses.some(([key]) => key === params.get('status')) ? params.get('status')! : 'ALL'
  const type = ['EVENT_FEE', 'MEMBERSHIP_DUE'].includes(params.get('type') ?? '') ? params.get('type')! : 'ALL'
  function filter(key: string, value: string) {
    setParams((current) => { const next = new URLSearchParams(current); if (!value || value === 'ALL') next.delete(key); else next.set(key, value); return next }, { replace: true })
  }
  if (payments.isLoading) return <LoadingState label="납부 내역을 불러오는 중" />
  if (payments.isError) return <ErrorState title="납부 내역을 불러오지 못했어요" action={<button className="secondary-button" onClick={() => void payments.refetch()}>다시 불러오기</button>} />
  const items = payments.data?.items ?? []
  const filtered = items.filter((payment) =>
    (status === 'ALL' || (status === 'NEEDS_PAYMENT' ? ['UNPAID', 'REJECTED'].includes(payment.status) : payment.status === status)) &&
    (type === 'ALL' || payment.type === type) && payment.source.title.toLowerCase().includes(search.trim().toLowerCase()),
  ).sort((a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt))

  return <div className="page member-list-page">
    <header className="member-list-heading"><span className="eyebrow">PAYMENTS</span><h1>납부 내역</h1><p>회비와 행사 참가비의 납부·환불 내역을 확인하세요.</p></header>
    <section className="member-list-filters" aria-label="납부 필터">
      <label className="member-list-search">납부 검색<input className="text-input" type="search" value={search} onChange={(e) => filter('q', e.target.value)} placeholder="행사명 또는 회비명" /></label>
      <label>납부 구분<select className="select-input" value={type} onChange={(e) => filter('type', e.target.value)}>
        <option value="ALL">전체 구분</option><option value="EVENT_FEE">행사 참가비</option><option value="MEMBERSHIP_DUE">동아리 회비</option>
      </select></label>
      <label>납부 상태<select className="select-input" value={status} onChange={(e) => filter('status', e.target.value)}>
        {statuses.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select></label>
    </section>
    <section aria-labelledby="payment-list-title">
      <div className="section-heading"><h2 id="payment-list-title">내 납부 목록</h2><span className="section-meta" aria-live="polite">{filtered.length}건</span></div>
      <div className="stack-list">{filtered.map((payment) => <PaymentCard key={payment.id} payment={payment} />)}</div>
      {filtered.length === 0 && <div className="home-empty"><strong>{items.length === 0 ? '아직 납부 내역이 없어요.' : '조건에 맞는 납부 내역이 없어요.'}</strong><span>납부 구분과 상태를 바꾸어 확인해 보세요.</span></div>}
    </section>
  </div>
}
