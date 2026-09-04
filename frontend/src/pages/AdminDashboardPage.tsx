import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { StatusBadge } from '../components/StatusBadge'
import { getAdminDashboard } from '../services/api'
import { formatDateTime, formatWon } from '../services/format'

export function AdminDashboardPage() {
  const dashboardQuery = useQuery({ queryKey: ['admin', 'dashboard'], queryFn: getAdminDashboard })

  if (dashboardQuery.isLoading) return <LoadingState label="운영 현황을 불러오는 중" />
  if (dashboardQuery.isError) return <ErrorState />

  const dashboard = dashboardQuery.data!

  return (
    <div className="admin-page">
      <section className="admin-page-heading">
        <div><span className="eyebrow">OVERVIEW</span><h1>오늘의 운영 현황</h1><p>확인이 필요한 입금과 다가오는 행사를 한눈에 확인하세요.</p></div>
        <div className="admin-date">2026. 09. 01</div>
      </section>

      <section className="admin-metric-grid" aria-label="운영 현황 요약">
        <article><span>활동 부원</span><strong>{dashboard.memberCount}</strong><small>승인 대기 {dashboard.pendingMemberCount}명</small></article>
        <article className="admin-metric--dark"><span>확인 대기 송금</span><strong>{dashboard.reportedCount}</strong><small>지금 처리가 필요해요</small></article>
        <article><span>전체 미납</span><strong>{dashboard.unpaidCount}</strong><small>{formatWon(dashboard.expectedAmount - dashboard.confirmedAmount)} 미수금</small></article>
        <article><span>예정 행사</span><strong>{dashboard.upcomingEventCount}</strong><small>이번 달 기준</small></article>
      </section>

      <section className="admin-grid-section">
        <article className="admin-panel collection-panel">
          <div className="admin-panel-heading"><div><span className="eyebrow">COLLECTION</span><h2>전체 수납률</h2></div><strong>{dashboard.collectionRate}%</strong></div>
          <div className="progress-track"><span style={{ width: `${dashboard.collectionRate}%` }} /></div>
          <dl><div><dt>확인 완료</dt><dd>{formatWon(dashboard.confirmedAmount)}</dd></div><div><dt>부과 예정</dt><dd>{formatWon(dashboard.expectedAmount)}</dd></div></dl>
        </article>

        <article className="admin-panel attention-panel">
          <span className="attention-icon" aria-hidden="true">!</span>
          <div><span className="eyebrow">NEEDS ACTION</span><h2>송금 신고 {dashboard.reportedCount}건</h2><p>입금자명과 계좌 내역을 대조한 후 승인해 주세요.</p></div>
          <Link to="/admin/payment-reports">확인하러 가기 →</Link>
        </article>
      </section>

      <section className="admin-content-section">
        <div className="admin-section-heading"><div><span className="eyebrow">EVENTS</span><h2>다가오는 행사</h2></div></div>
        <div className="admin-event-grid">
          {dashboard.upcomingEvents.map((event) => (
            <Link className="admin-event-card" key={event.id} to={`/admin/events/${event.id}/participants`}>
              <div><span>{formatDateTime(event.startsAt)}</span><strong>{event.title}</strong></div>
              <dl><div><dt>참가</dt><dd>{event.joinedCount}{event.capacity ? ` / ${event.capacity}` : ''}명</dd></div><div><dt>미납</dt><dd>{event.unpaidCount}명</dd></div><div><dt>확인 대기</dt><dd>{event.reportedCount}명</dd></div></dl>
              <span className="card-link">참가자 보기 →</span>
            </Link>
          ))}
        </div>
      </section>

      <section className="admin-content-section">
        <div className="admin-section-heading"><div><span className="eyebrow">RECENT REPORTS</span><h2>최근 송금 신고</h2></div><Link to="/admin/payment-reports">전체보기</Link></div>
        <div className="admin-compact-list">
          {dashboard.recentReports.slice(0, 4).map((payment) => (
            <div key={payment.paymentId}>
              <div className="member-cell"><span>{payment.name.slice(0, 1)}</span><div><strong>{payment.nickname}</strong><small>입금자 {payment.latestReport?.senderName}</small></div></div>
              <strong>{formatWon(payment.amount)}</strong>
              <StatusBadge status={payment.status} />
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
