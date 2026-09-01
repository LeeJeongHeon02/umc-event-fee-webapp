import { useQuery } from '@tanstack/react-query'
import { NavLink, Outlet } from 'react-router-dom'
import { getMe } from '../services/api'

const navItems = [
  { to: '/home', label: '홈', symbol: '⌂' },
  { to: '/home#events', label: '행사', symbol: '◇' },
  { to: '/home#payments', label: '납부', symbol: '₩' },
]

export function AppShell() {
  const meQuery = useQuery({ queryKey: ['me'], queryFn: getMe })
  const canManage = meQuery.data?.role === 'STAFF' || meQuery.data?.role === 'ADMIN'

  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink className="brand" to="/home" aria-label="D Club 홈">
          <span className="brand__mark">D:</span>
          <span>Club</span>
        </NavLink>
        <div className="topbar__actions">
          {canManage && <NavLink className="admin-mode-link" to="/admin">운영진</NavLink>}
          <button className="icon-button" type="button" aria-label="알림">
            <span aria-hidden="true">●</span>
            <span className="notification-dot" />
          </button>
          <div className="avatar" aria-label="PE Web 홍길동">홍</div>
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
