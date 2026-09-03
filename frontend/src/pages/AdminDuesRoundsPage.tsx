import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { StatusBadge } from '../components/StatusBadge'
import { createAdminDuesRound, deleteAdminDuesRound, getAdminDuesRounds, publishAdminDuesRound } from '../services/api'
import { formatDateTime, formatWon } from '../services/format'
import { useCurrentMember } from '../hooks/useCurrentMember'

const initial = { title: '', amount: '10000', dueAt: '2026-09-30T23:59', bankName: '', accountNumber: '', accountHolder: '', kakaoPayReceiveUrl: '' }

export function AdminDuesRoundsPage() {
  const client = useQueryClient()
  const me = useCurrentMember()
  const rounds = useQuery({ queryKey: ['admin', 'dues-rounds'], queryFn: getAdminDuesRounds })
  const [form, setForm] = useState(initial)
  const refresh = () => client.invalidateQueries({ queryKey: ['admin'] })
  const create = useMutation({ mutationFn: () => createAdminDuesRound({ ...form, amount: Number(form.amount), dueAt: new Date(form.dueAt).toISOString(), version: 0 }), onSuccess: async () => { setForm(initial); await refresh() } })
  const publish = useMutation({ mutationFn: ({ id, version }: { id: number; version: number }) => publishAdminDuesRound(id, version), onSuccess: refresh })
  const remove = useMutation({ mutationFn: ({ id, version }: { id: number; version: number }) => deleteAdminDuesRound(id, version), onSuccess: refresh })
  if (rounds.isLoading) return <LoadingState label="회비 차수를 불러오는 중" />
  if (rounds.isError) return <ErrorState />
  const error = create.error ?? publish.error ?? remove.error
  return <div className="admin-page">
    <section className="admin-page-heading"><div><span className="eyebrow">DUES ROUNDS</span><h1>회비 관리</h1><p>회비 차수를 공개하면 현재 활성 부원 전체의 납부 항목이 생성됩니다.</p></div>{me.data?.role === 'ADMIN' && <Link className="secondary-button" to="/admin/settings">송금정보 설정</Link>}</section>
    <section className="event-management-layout">
      <div className="admin-panel event-admin-list">{rounds.data!.map((round) => <div className="dues-admin-item" key={round.id}><div><StatusBadge status={round.status} /><strong>{round.title}</strong><small>{formatWon(round.amount)} · {formatDateTime(round.dueAt)} · {round.targetCount}명</small></div><div className="event-form-actions">{round.status === 'DRAFT' && <button className="table-button table-button--confirm" onClick={() => publish.mutate({ id: round.id, version: round.version })}>공개</button>}{round.status === 'DRAFT' && <button className="table-button" onClick={() => remove.mutate({ id: round.id, version: round.version })}>삭제</button>}{round.status !== 'DRAFT' && <Link className="table-button" to={`/admin/fees/${round.id}/payments`}>납부 내역</Link>}</div></div>)}{rounds.data!.length === 0 && <p className="admin-empty">아직 회비 차수가 없습니다.</p>}</div>
      <form className="admin-panel event-admin-form" onSubmit={(event: FormEvent) => { event.preventDefault(); create.mutate() }}><div className="admin-panel-heading"><div><span className="eyebrow">NEW ROUND</span><h2>새 회비 차수</h2></div></div>
        <label>제목<input required value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label>
        <div className="event-form-grid"><label>금액<input required min="0" type="number" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></label><label>납부 기한<input required type="datetime-local" value={form.dueAt} onChange={(e) => setForm({ ...form, dueAt: e.target.value })} /></label></div>
        <div className="event-form-grid"><label>은행<input value={form.bankName} onChange={(e) => setForm({ ...form, bankName: e.target.value })} /></label><label>예금주<input value={form.accountHolder} onChange={(e) => setForm({ ...form, accountHolder: e.target.value })} /></label></div>
        <label>계좌번호<input value={form.accountNumber} onChange={(e) => setForm({ ...form, accountNumber: e.target.value })} /></label><label>카카오페이 코드송금 URL<input type="url" value={form.kakaoPayReceiveUrl} onChange={(e) => setForm({ ...form, kakaoPayReceiveUrl: e.target.value })} /></label>
        {error && <p className="form-error" role="alert">{error.message}</p>}<button className="primary-button" disabled={create.isPending}>초안 저장</button>
      </form>
    </section>
  </div>
}
