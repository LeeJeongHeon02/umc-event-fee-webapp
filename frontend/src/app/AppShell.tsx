import type { ReactNode } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getNotifications } from '../services/api'
import { useCurrentMember } from '../hooks/useCurrentMember'
import { MemberScrollReset } from './MemberNavigation'

const navItems = [
  { to: '/home', label: '홈', icon: <HomeIcon /> },
  { to: '/events', label: '행사', icon: <CalendarIcon /> },
  { to: '/payments', label: '납부', icon: <WalletIcon /> },
  { to: '/mypage', label: '마이', icon: <UserIcon /> },
]

export function AppShell() {
  const location = useLocation()
  const meQuery = useCurrentMember()
  const canManage = meQuery.data?.status === 'ACTIVE' && (meQuery.data?.role === 'STAFF' || meQuery.data?.role === 'ADMIN')
  const memberName = meQuery.data?.name ?? meQuery.data?.kakaoProfileName ?? '회원'
  const memberLabel = meQuery.data?.displayNickname ?? memberName
  const notifications = useQuery({ queryKey: ['notifications'], queryFn: getNotifications, enabled: meQuery.data?.status === 'ACTIVE' })

  return (
    <div className="app-shell">
      <MemberScrollReset />
      <header className="topbar">
        <NavLink className="brand" to="/home" aria-label="D Club 홈">
          <span className="brand__mark">D:</span>
          <span>Club</span>
        </NavLink>
        <div className="topbar__actions">
          {canManage && <NavLink className="admin-mode-link" to="/admin">운영진</NavLink>}
          <NavLink className="icon-button" to="/notifications" aria-label={`알림 ${notifications.data?.unreadCount ?? 0}개`}>
            <BellIcon />
            {(notifications.data?.unreadCount ?? 0) > 0 && <span className="notification-dot" />}
          </NavLink>
          <NavLink to="/mypage" className="avatar" aria-label={`${memberLabel} 마이페이지`}>{memberName.slice(0, 1)}</NavLink>
        </div>
      </header>

      <main className="app-main">
        <Outlet />
      </main>

      <nav className="bottom-nav" aria-label="주요 메뉴">
        {navItems.map((item) => {
          const isActive = location.pathname === item.to || location.pathname.startsWith(item.to + '/')
          return (
            <Link key={item.label} to={item.to} className={isActive ? 'active' : undefined} aria-current={isActive ? 'page' : undefined}>
              {item.icon}
              <span>{item.label}</span>
            </Link>
          )
        })}
      </nav>
    </div>
  )
}

function Icon({ children }: { children: ReactNode }) {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{children}</svg>
}

function HomeIcon() {
  return <Icon><path d="m3.5 10.5 8.5-7 8.5 7" /><path d="M5.5 9.5V20h13V9.5" /><path d="M9.5 20v-6h5v6" /></Icon>
}

function CalendarIcon() {
  return <Icon><rect x="3.5" y="5.5" width="17" height="15" rx="2" /><path d="M8 3v5M16 3v5M3.5 10h17" /></Icon>
}

function WalletIcon() {
  return <Icon><path d="M4 6.5h13.5a2.5 2.5 0 0 1 2.5 2.5v9H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h11" /><path d="M15 11h5v4h-5a2 2 0 0 1 0-4Z" /></Icon>
}

function BellIcon() {
  return <Icon><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></Icon>
}

function UserIcon() {
  return <Icon><circle cx="12" cy="8" r="4" /><path d="M4 21v-2a8 8 0 0 1 16 0v2" /></Icon>
}
