import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AdminReviewActions } from '../components/AdminReviewActions'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { FilterBar } from '../components/FilterBar'
import { StatusBadge } from '../components/StatusBadge'
import { getAdminEventParticipants } from '../services/api'
import { formatDateTime, formatWon } from '../services/format'
import type { PaymentStatus } from '../services/types'

export function AdminEventParticipantsPage() {
  const eventId = Number(useParams().eventId)
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<'ALL' | PaymentStatus>('ALL')
  const participantsQuery = useQuery({
    queryKey: ['admin', 'event-participants', eventId],
    queryFn: () => getAdminEventParticipants(eventId),
    enabled: Number.isFinite(eventId),
  })

  const filteredItems = useMemo(() => {
    const items = participantsQuery.data?.items ?? []
    return items.filter((item) =>
      (status === 'ALL' || item.paymentStatus === status) &&
      (item.nickname.toLowerCase().includes(query.toLowerCase()) || item.name.includes(query)),
    )
  }, [participantsQuery.data?.items, query, status])

  if (participantsQuery.isLoading) return <LoadingState label="행사 참가자를 불러오는 중" />
  if (participantsQuery.isError) return <ErrorState />

  const { event, items } = participantsQuery.data!

  return (
    <div className="admin-page">
      <Link className="admin-back-link" to="/admin">← 대시보드</Link>
      <section className="admin-page-heading admin-page-heading--detail">
        <div><span className="eyebrow">EVENT PARTICIPANTS</span><h1>{event.title}</h1><p>{formatDateTime(event.startsAt)} · 참가비 {formatWon(event.feeAmount)}</p></div>
        <Link className="primary-button" to={`/events/${event.id}`}>회원용 화면 보기</Link>
      </section>

      <section className="admin-detail-metrics" aria-label="행사 참가비 현황">
        <div><span>참가 신청</span><strong>{event.joinedCount}명</strong></div>
        <div><span>납부 완료</span><strong>{event.confirmedCount}명</strong></div>
        <div><span>확인 대기</span><strong>{event.reportedCount}명</strong></div>
        <div><span>미납</span><strong>{event.unpaidCount}명</strong></div>
      </section>

      <section className="admin-list-panel">
        <div className="admin-toolbar">
          <div><span className="eyebrow">MEMBERS</span><h2>참가 부원 {items.length}명</h2></div>
          <FilterBar>
            <label><span className="sr-only">부원 검색</span><input className="text-input" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="이름 또는 파트 검색" /></label>
            <label><span className="sr-only">납부 상태</span><select className="select-input" value={status} onChange={(event) => setStatus(event.target.value as typeof status)}><option value="ALL">전체 상태</option><option value="REPORTED">확인 대기</option><option value="UNPAID">미납</option><option value="CONFIRMED">납부 완료</option><option value="REJECTED">반려</option></select></label>
          </FilterBar>
        </div>
        <div className="admin-table-wrap">
          <table className="admin-table admin-table--payment-list">
            <thead><tr><th>참가 부원</th><th>신청일</th><th>입금자명</th><th>참가비</th><th>상태</th><th>관리</th></tr></thead>
            <tbody>
              {filteredItems.map((item) => (
                <tr key={item.participationId}>
                  <td data-label="참가 부원"><div className="member-cell"><span>{item.name.slice(0, 1)}</span><div><strong>{item.nickname}</strong><small>{item.part.replace('_', ' ')}</small></div></div></td>
                  <td data-label="신청일">{formatDateTime(item.joinedAt)}</td>
                  <td data-label="입금자명">{item.latestReport?.senderName ?? '—'}</td>
                  <td data-label="참가비">{formatWon(item.amount)}</td>
                  <td data-label="상태"><StatusBadge status={item.paymentStatus} /></td>
                  <td data-label="관리">{item.paymentStatus === 'REPORTED' && item.paymentId ? <AdminReviewActions memberName={item.name} paymentId={item.paymentId} version={item.version} /> : <span className="table-muted">처리 없음</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {filteredItems.length === 0 && <p className="admin-empty">조건에 맞는 참가자가 없습니다.</p>}
      </section>
    </div>
  )
}
