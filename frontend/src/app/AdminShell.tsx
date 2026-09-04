import { NavLink, Outlet } from 'react-router-dom'
import { useCurrentMember } from '../hooks/useCurrentMember'

const adminNavItems = [
  { to: '/admin', label: '대시보드', symbol: '▦', end: true },
  { to: '/admin/payment-reports', label: '송금 신고', symbol: '₩' },
  { to: '/admin/events', label: '행사 관리', symbol: '+' },
  { to: '/admin/dues', label: '회비 관리', symbol: '₩' },
  { to: '/admin/members', label: '회원 관리', symbol: '◎', adminOnly: true },
  { to: '/admin/refunds', label: '환불 관리', symbol: '↺' },
]

export function AdminShell() {
  const meQuery = useCurrentMember()
  const memberName = meQuery.data?.name ?? meQuery.data?.kakaoProfileName ?? '운영진'

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <NavLink className="admin-brand" to="/admin" aria-label="D Club 운영진 홈">
          <span>D:</span>
          <div><strong>Club</strong><small>운영진 센터</small></div>
        </NavLink>
        <nav className="admin-nav" aria-label="운영진 메뉴">
          {adminNavItems.filter((item) => !item.adminOnly || meQuery.data?.role === 'ADMIN').map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end}>
              <span aria-hidden="true">{item.symbol}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <NavLink className="member-mode-link" to="/home">← 동아리원 화면</NavLink>
      </aside>
      <div className="admin-workspace">
        <header className="admin-topbar">
          <div><span className="admin-mobile-brand">D: Admin</span></div>
          <NavLink to="/mypage" className="admin-profile" aria-label="마이페이지">
            <span>{meQuery.data?.role ?? 'STAFF'}</span>
            <strong>{memberName}</strong>
            <div>{memberName.slice(0, 1)}</div>
          </NavLink>
        </header>
        <main className="admin-main"><Outlet /></main>
      </div>
    </div>
  )
}
