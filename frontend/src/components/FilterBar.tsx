import type { ReactNode } from 'react'

export function FilterBar({ children, label = '목록 필터' }: { children: ReactNode; label?: string }) {
  return <div className="filter-bar" aria-label={label}>{children}</div>
}
