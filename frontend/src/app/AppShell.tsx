import { NavLink, Outlet } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getNotifications } from '../services/api'
import { useCurrentMember } from '../hooks/useCurrentMember'

const navItems = [
  { to: '/home', label: '홈', symbol: '⌂' },
  { to: '/home#events', label: '행사', symbol: '◇' },
  { to: '/home#payments', label: '납부', symbol: '₩' },
]

export function AppShell() {
  const meQuery = useCurrentMember()
  const canManage = meQuery.data?.role === 'STAFF' || meQuery.data?.role === 'ADMIN'
  const memberName = meQuery.data?.name ?? meQuery.data?.kakaoProfileName ?? '회원'
  const memberLabel = meQuery.data?.displayNickname ?? memberName
  const notifications = useQuery({ queryKey: ['notifications'], queryFn: getNotifications, enabled: meQuery.data?.status === 'ACTIVE' })

  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink className="brand" to="/home" aria-label="D Club 홈">
          <span className="brand__mark">D:</span>
          <span>Club</span>
        </NavLink>
        <div className="topbar__actions">
          {canManage && <NavLink className="admin-mode-link" to="/admin">운영진</NavLink>}
          <NavLink className="icon-button" to="/notifications" aria-label={`알림 ${notifications.data?.unreadCount ?? 0}개`}>
            <span aria-hidden="true">●</span>
            {(notifications.data?.unreadCount ?? 0) > 0 && <span className="notification-dot" />}
          </NavLink>
          <div className="avatar" aria-label={memberLabel}>{memberName.slice(0, 1)}</div>
        </div>
      </header>

      <main className="app-main">
        <Outlet />
      </main>

      <nav className="bottom-nav" aria-label="주요 메뉴">
        {navItems.map((item) => (
          <NavLink key={item.label} to={item.to}>
            <span aria-hidden="true">{item.symbol}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
