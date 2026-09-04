import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { EventCard } from '../components/EventCard'
import { getEvents } from '../services/api'
import '../styles/member-lists.css'

export function EventsPage() {
  const events = useQuery({ queryKey: ['events'], queryFn: getEvents })
  const [params, setParams] = useSearchParams()
  const search = params.get('q') ?? ''
  const requested = params.get('participation') ?? 'ALL'
  const participation = ['JOINED', 'NOT_JOINED', 'CANCELED'].includes(requested) ? requested : 'ALL'
  function filter(key: string, value: string) {
    setParams((current) => { const next = new URLSearchParams(current); if (!value || value === 'ALL') next.delete(key); else next.set(key, value); return next }, { replace: true })
  }
  if (events.isLoading) return <LoadingState label="행사 목록을 불러오는 중" />
  if (events.isError) return <ErrorState title="행사 목록을 불러오지 못했어요" action={<button className="secondary-button" onClick={() => void events.refetch()}>다시 불러오기</button>} />
  const items = events.data?.items ?? []
  const filtered = items.filter((event) =>
    (participation === 'ALL' || (participation === 'NOT_JOINED' ? !event.myParticipationStatus : event.myParticipationStatus === participation)) &&
    [event.title, event.summary, event.location].some((value) => value?.toLowerCase().includes(search.trim().toLowerCase())),
  ).sort((a, b) => Date.parse(a.startsAt) - Date.parse(b.startsAt))

  return <div className="page member-list-page">
    <header className="member-list-heading"><span className="eyebrow">EVENTS</span><h1>행사</h1><p>공개된 행사와 나의 참여 여부를 확인하세요.</p></header>
    <section className="member-list-filters" aria-label="행사 필터">
      <label className="member-list-search">행사 검색<input className="text-input" type="search" value={search} onChange={(e) => filter('q', e.target.value)} placeholder="행사명 또는 장소" /></label>
      <label>참여 상태<select className="select-input" value={participation} onChange={(e) => filter('participation', e.target.value)}>
        <option value="ALL">전체 행사</option><option value="JOINED">신청한 행사</option><option value="NOT_JOINED">미신청</option><option value="CANCELED">참가 취소</option>
      </select></label>
    </section>
    <section aria-labelledby="event-list-title">
      <div className="section-heading"><h2 id="event-list-title">행사 목록</h2><span className="section-meta" aria-live="polite">{filtered.length}건</span></div>
      <div className="stack-list">{filtered.map((event) => <article className="member-event-item" key={event.id}>
        <EventCard event={event} />
        <p className="member-event-participation">{event.myParticipationStatus === 'JOINED' ? '참가 신청 완료' : event.myParticipationStatus === 'CANCELED' ? '참가 취소' : '미신청'}</p>
      </article>)}</div>
      {filtered.length === 0 && <div className="home-empty"><strong>{items.length === 0 ? '등록된 행사가 없어요.' : '조건에 맞는 행사가 없어요.'}</strong><span>새 행사가 공개되면 여기에서 확인할 수 있어요.</span></div>}
    </section>
  </div>
}
