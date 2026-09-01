import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AdminReviewActions } from '../components/AdminReviewActions'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { StatusBadge } from '../components/StatusBadge'
import { getAdminDuesPayments } from '../services/api'
import { formatDateTime, formatWon } from '../services/format'
import type { PaymentStatus } from '../services/types'

export function AdminFeePaymentsPage() {
  const duesRoundId = Number(useParams().duesRoundId)
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<'ALL' | PaymentStatus>('ALL')
  const paymentsQuery = useQuery({
    queryKey: ['admin', 'dues-payments', duesRoundId],
    queryFn: () => getAdminDuesPayments(duesRoundId),
    enabled: Number.isFinite(duesRoundId),
  })

  const filteredItems = useMemo(() => {
    const items = paymentsQuery.data?.items ?? []
    return items.filter((item) =>
      (status === 'ALL' || item.status === status) &&
      (item.nickname.toLowerCase().includes(query.toLowerCase()) || item.name.includes(query)),
    )
  }, [paymentsQuery.data?.items, query, status])

  if (paymentsQuery.isLoading) return <LoadingState label="회비 납부 내역을 불러오는 중" />
  if (paymentsQuery.isError) return <ErrorState />

  const { duesRound, items } = paymentsQuery.data!
  const collectionRate = Math.round((duesRound.confirmedCount / duesRound.targetCount) * 100)

  return (
    <div className="admin-page">
      <Link className="admin-back-link" to="/admin">← 대시보드</Link>
      <section className="admin-page-heading admin-page-heading--detail">
        <div><span className="eyebrow">MEMBERSHIP DUES</span><h1>{duesRound.title}</h1><p>{formatWon(duesRound.amount)} · 납부 기한 {formatDateTime(duesRound.dueAt)}</p></div>
        <button className="secondary-button" type="button" disabled>CSV 내보내기 · 준비 중</button>
      </section>

      <section className="admin-fee-summary">
        <div className="fee-rate"><span>현재 수납률</span><strong>{collectionRate}%</strong><div className="progress-track"><span style={{ width: `${collectionRate}%` }} /></div></div>
        <div><span>납부 완료</span><strong>{duesRound.confirmedCount}명</strong><small>{formatWon(duesRound.confirmedAmount)}</small></div>
        <div><span>확인 대기</span><strong>{duesRound.reportedCount}명</strong><small>입금 대조 필요</small></div>
        <div><span>미납</span><strong>{duesRound.unpaidCount}명</strong><small>안내 필요</small></div>
      </section>

      <section className="admin-list-panel">
        <div className="admin-toolbar">
          <div><span className="eyebrow">PAYMENTS</span><h2>부원별 납부 내역</h2></div>
          <div className="admin-filters">
            <label><span className="sr-only">부원 검색</span><input className="text-input" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="이름 또는 파트 검색" /></label>
            <label><span className="sr-only">납부 상태</span><select className="select-input" value={status} onChange={(event) => setStatus(event.target.value as typeof status)}><option value="ALL">전체 상태</option><option value="REPORTED">확인 대기</option><option value="UNPAID">미납</option><option value="CONFIRMED">납부 완료</option><option value="REJECTED">반려</option></select></label>
          </div>
        </div>
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead><tr><th>부원</th><th>입금자명</th><th>신고 시각</th><th>금액</th><th>상태</th><th>관리</th></tr></thead>
            <tbody>
              {filteredItems.map((item) => (
                <tr key={item.paymentId}>
                  <td><div className="member-cell"><span>{item.name.slice(0, 1)}</span><div><strong>{item.nickname}</strong><small>{item.part.replace('_', ' ')}</small></div></div></td>
                  <td>{item.latestReport?.senderName ?? '—'}</td>
                  <td>{item.latestReport ? formatDateTime(item.latestReport.reportedAt) : '—'}</td>
                  <td>{formatWon(item.amount)}</td>
                  <td><StatusBadge status={item.status} /></td>
                  <td>{item.status === 'REPORTED' ? <AdminReviewActions memberName={item.name} paymentId={item.paymentId} version={item.version} /> : <span className="table-muted">처리 없음</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {filteredItems.length === 0 && <p className="admin-empty">조건에 맞는 납부 내역이 없습니다.</p>}
        <p className="admin-list-footnote">목 데이터에는 전체 {duesRound.targetCount}명 중 대표 {items.length}명의 내역만 표시됩니다.</p>
      </section>
    </div>
  )
}

