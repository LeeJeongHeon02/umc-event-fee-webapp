import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { AdminReviewActions } from '../components/AdminReviewActions'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { FilterBar } from '../components/FilterBar'
import { StatusBadge } from '../components/StatusBadge'
import { getAdminPaymentReports } from '../services/api'
import { formatDateTime, formatWon } from '../services/format'

export function AdminPaymentReportsPage() {
  const [search, setSearch] = useState('')
  const reports = useQuery({ queryKey: ['admin', 'payment-reports'], queryFn: getAdminPaymentReports })
  if (reports.isLoading) return <LoadingState label="송금 신고를 불러오는 중" />
  if (reports.isError) return <ErrorState action={<button className="secondary-button" onClick={() => void reports.refetch()}>다시 불러오기</button>} />
  const items = reports.data ?? []
  const filtered = items.filter((item) => [item.nickname, item.source?.title, item.latestReport?.senderName]
    .some((value) => value?.toLowerCase().includes(search.trim().toLowerCase())))

  return <div className="admin-page">
    <Link className="admin-back-link" to="/admin">← 대시보드</Link>
    <section className="admin-page-heading"><div><span className="eyebrow">PAYMENT REPORTS</span><h1>송금 신고 확인</h1>
      <p>행사 참가비와 회비의 확인 대기 신고 {items.length}건입니다. 실제 입금 내역을 대조한 후 승인해 주세요.</p></div></section>
    <section className="admin-list-panel">
      <div className="admin-toolbar"><h2>확인 대기 목록</h2><FilterBar label="송금 신고 필터"><label><span className="sr-only">송금 신고 검색</span>
        <input className="text-input" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="부원, 입금자 또는 행사·회비명 검색" /></label></FilterBar></div>
      <div className="admin-table-wrap"><table className="admin-table admin-table--payment-reports">
        <thead><tr><th>부원</th><th>납부 항목</th><th>입금자명</th><th>신고 시각</th><th>금액</th><th>상태</th><th>관리</th></tr></thead>
        <tbody>{filtered.map((item) => <tr key={item.paymentId}>
          <td data-label="부원">{item.nickname}</td>
          <td data-label="납부 항목">{item.source ? <Link to={item.source.type === 'EVENT'
            ? `/admin/events/${item.source.id}/participants` : `/admin/fees/${item.source.id}/payments`}>
            {item.source.type === 'EVENT' ? '행사비' : '회비'} · {item.source.title}</Link> : '납부 항목'}</td>
          <td data-label="입금자명">{item.latestReport?.senderName ?? '—'}</td>
          <td data-label="신고 시각">{item.latestReport ? formatDateTime(item.latestReport.reportedAt) : '—'}</td>
          <td data-label="금액">{formatWon(item.amount)}</td><td data-label="상태"><StatusBadge status={item.status} /></td>
          <td data-label="관리"><AdminReviewActions memberName={item.name} paymentId={item.paymentId} version={item.version} /></td>
        </tr>)}</tbody>
      </table></div>
      {filtered.length === 0 && <p className="admin-empty">{items.length === 0 ? '확인이 필요한 송금 신고가 없습니다.' : '검색 조건에 맞는 송금 신고가 없습니다.'}</p>}
    </section>
  </div>
}
