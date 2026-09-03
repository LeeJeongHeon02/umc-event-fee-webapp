import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { getNotifications, readAllNotifications, readNotification } from '../services/api'
import { formatDateTime } from '../services/format'

export function NotificationsPage() {
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['notifications'], queryFn: getNotifications })
  const read = useMutation({ mutationFn: readNotification, onSuccess: () => client.invalidateQueries({ queryKey: ['notifications'] }) })
  const readAll = useMutation({ mutationFn: readAllNotifications, onSuccess: () => client.invalidateQueries({ queryKey: ['notifications'] }) })
  if (query.isLoading) return <LoadingState label="알림을 불러오는 중" />
  if (query.isError) return <ErrorState />
  return <div className="page"><section className="section-heading"><div><span className="eyebrow">NOTIFICATIONS</span><h1>알림</h1></div>{query.data!.unreadCount > 0 && <button className="text-button" onClick={() => readAll.mutate()}>모두 읽음</button>}</section><div className="notification-list">{query.data!.items.map((item) => <Link key={item.id} className={item.readAt ? 'notification-item is-read' : 'notification-item'} to={item.linkUrl ?? '/home'} onClick={() => { if (!item.readAt) read.mutate(item.id) }}><div><strong>{item.title}</strong><p>{item.body}</p></div><small>{formatDateTime(item.createdAt)}</small></Link>)}{query.data!.items.length === 0 && <p className="empty-state">새 알림이 없습니다.</p>}</div></div>
}
