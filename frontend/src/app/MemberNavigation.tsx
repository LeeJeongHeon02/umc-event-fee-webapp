import { useEffect } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { HomePage } from '../pages/HomePage'

export function HomeEntry() {
  const { hash } = useLocation()
  // Preserve bookmarks made before the sections became independent pages.
  if (hash === '#events') return <Navigate to="/events" replace />
  if (hash === '#payments') return <Navigate to="/payments" replace />
  return <HomePage />
}

export function MemberScrollReset() {
  const { pathname } = useLocation()
  useEffect(() => { window.scrollTo(0, 0) }, [pathname])
  return null
}
