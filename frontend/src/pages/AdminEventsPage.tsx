import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { ConfirmActionDialog } from '../components/ConfirmActionDialog'
import { StatusBadge } from '../components/StatusBadge'
import { MarkdownEditor } from '../components/MarkdownEditor'
import {
  cancelAdminEvent,
  closeAdminEvent,
  createAdminEvent,
  deleteAdminEvent,
  getAdminEvents,
  publishAdminEvent,
  updateAdminEvent,
} from '../services/api'
import { formatDateTime, formatWon } from '../services/format'
import type { AdminEventCreateRequest, AdminEventResponse } from '../services/types'

type EventForm = {
  title: string
  summary: string
  description: string
  location: string
  startsAt: string
  endsAt: string
  registrationDeadline: string
  capacity: string
  feeAmount: string
  allowLateCancellation: boolean
}

const emptyForm: EventForm = {
  title: '',
  summary: '',
  description: '',
  location: '',
  startsAt: '2026-09-15T19:00',
  endsAt: '2026-09-15T21:00',
  registrationDeadline: '2026-09-14T23:59',
  capacity: '',
  feeAmount: '0',
  allowLateCancellation: false,
}

function toLocalInput(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}

function eventToForm(event: AdminEventResponse): EventForm {
  return {
    title: event.title,
    summary: event.summary ?? '',
    description: event.description,
    location: event.location ?? '',
    startsAt: toLocalInput(event.startsAt),
    endsAt: toLocalInput(event.endsAt),
    registrationDeadline: toLocalInput(event.registrationDeadline),
    capacity: event.capacity?.toString() ?? '',
    feeAmount: event.feeAmount.toString(),
    allowLateCancellation: event.allowLateCancellation,
  }
}

function toRequest(form: EventForm): AdminEventCreateRequest {
  return {
    title: form.title.trim(),
    summary: form.summary.trim() || undefined,
    description: form.description.trim(),
    location: form.location.trim() || undefined,
    startsAt: new Date(form.startsAt).toISOString(),
    endsAt: form.endsAt ? new Date(form.endsAt).toISOString() : undefined,
    registrationDeadline: new Date(form.registrationDeadline).toISOString(),
    capacity: form.capacity ? Number(form.capacity) : undefined,
    feeAmount: Number(form.feeAmount),
    allowLateCancellation: form.allowLateCancellation,
  }
}

export function AdminEventsPage() {
  const queryClient = useQueryClient()
  const eventsQuery = useQuery({ queryKey: ['admin', 'events'], queryFn: getAdminEvents })
  const [selected, setSelected] = useState<AdminEventResponse | null>(null)
  const [form, setForm] = useState<EventForm>(emptyForm)
  const [message, setMessage] = useState('')
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [confirmingCancellation, setConfirmingCancellation] = useState(false)

  const saveMutation = useMutation({
    mutationFn: () => selected
      ? updateAdminEvent(selected.id, { ...toRequest(form), version: selected.version })
      : createAdminEvent(toRequest(form)),
    onSuccess: async (event) => {
      setSelected(event)
      setForm(eventToForm(event))
      setMessage('행사 초안을 저장했습니다.')
      await queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })
  const publishMutation = useMutation({
    mutationFn: () => publishAdminEvent(selected!.id, selected!.version),
    onSuccess: async (event) => {
      setSelected(event)
      setMessage('동아리원에게 행사를 공개했습니다.')
      await queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })
  const deleteMutation = useMutation({
    mutationFn: () => deleteAdminEvent(selected!.id, selected!.version),
    onSuccess: async () => {
      setSelected(null)
      setForm(emptyForm)
      setConfirmingDelete(false)
      setMessage('행사 초안을 삭제했습니다.')
      await queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })
  const closeMutation = useMutation({
    mutationFn: () => closeAdminEvent(selected!.id, selected!.version),
    onSuccess: async (event) => {
      setSelected(event)
      setMessage('행사를 종료했습니다.')
      await queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })
  const cancelMutation = useMutation({
    mutationFn: () => cancelAdminEvent(selected!.id, selected!.version, '운영진 행사 취소'),
    onSuccess: async () => {
      setConfirmingCancellation(false)
      setMessage('행사를 취소하고 납부 항목을 정리했습니다.')
      const refreshed = await queryClient.fetchQuery({ queryKey: ['admin', 'events'], queryFn: getAdminEvents })
      const event = refreshed.find((item) => item.id === selected!.id)
      if (event) setSelected(event)
      await queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })

  const selectEvent = (event: AdminEventResponse) => {
    setSelected(event)
    setForm(eventToForm(event))
    setMessage('')
    setConfirmingDelete(false)
    setConfirmingCancellation(false)
  }

  const startNew = () => {
    setSelected(null)
    setForm(emptyForm)
    setMessage('')
    setConfirmingDelete(false)
    setConfirmingCancellation(false)
  }

  const updateField = <K extends keyof EventForm>(key: K, value: EventForm[K]) => {
    setForm((current) => ({ ...current, [key]: value }))
  }

  if (eventsQuery.isLoading) return <LoadingState label="행사 목록을 불러오는 중" />
  if (eventsQuery.isError) return <ErrorState />

  const isDraft = !selected || selected.status === 'DRAFT'
  const mutationError = saveMutation.error ?? publishMutation.error ?? deleteMutation.error ?? closeMutation.error ?? cancelMutation.error

  return (
    <div className="admin-page">
      <section className="admin-page-heading">
        <div><span className="eyebrow">EVENT MANAGEMENT</span><h1>행사 관리</h1><p>행사를 초안으로 저장하고 검토한 뒤 공개하세요.</p></div>
        <button className="primary-button" type="button" onClick={startNew}>+ 새 행사</button>
      </section>

      <section className="event-management-layout">
        <div className="admin-panel event-admin-list" aria-label="행사 목록">
          {eventsQuery.data!.map((event) => (
            <button type="button" key={event.id} className={selected?.id === event.id ? 'is-selected' : ''} onClick={() => selectEvent(event)}>
              <span><StatusBadge status={event.status} /><small>{formatDateTime(event.startsAt)}</small></span>
              <strong>{event.title}</strong>
              <small>{event.joinedCount}명 참가 · {formatWon(event.feeAmount)}</small>
            </button>
          ))}
          {eventsQuery.data!.length === 0 && <p className="admin-empty">아직 행사가 없습니다.</p>}
        </div>

        <form className="admin-panel event-admin-form" onSubmit={(event: FormEvent) => { event.preventDefault(); saveMutation.mutate() }}>
          <div className="admin-panel-heading"><div><span className="eyebrow">{selected ? 'EDIT EVENT' : 'NEW EVENT'}</span><h2>{selected ? selected.title : '새 행사 초안'}</h2></div>{selected && <StatusBadge status={selected.status} />}</div>
          <label>행사명<input required maxLength={200} value={form.title} onChange={(e) => updateField('title', e.target.value)} disabled={!isDraft} /></label>
          <label>한 줄 소개<input maxLength={500} value={form.summary} onChange={(e) => updateField('summary', e.target.value)} disabled={!isDraft} /></label>
          <MarkdownEditor key={selected?.id ?? 'new'} value={form.description} onChange={(value) => updateField('description', value)} disabled={!isDraft} />
          <label>장소<input maxLength={200} value={form.location} onChange={(e) => updateField('location', e.target.value)} disabled={!isDraft} /></label>
          <div className="event-form-grid">
            <label>시작 일시<input required type="datetime-local" value={form.startsAt} onChange={(e) => updateField('startsAt', e.target.value)} disabled={!isDraft} /></label>
            <label>종료 일시<input type="datetime-local" value={form.endsAt} onChange={(e) => updateField('endsAt', e.target.value)} disabled={!isDraft} /></label>
            <label>신청 마감<input required type="datetime-local" value={form.registrationDeadline} onChange={(e) => updateField('registrationDeadline', e.target.value)} disabled={!isDraft} /></label>
            <label>정원<input min="1" type="number" value={form.capacity} onChange={(e) => updateField('capacity', e.target.value)} disabled={!isDraft} placeholder="제한 없음" /></label>
            <label>참가비<input required min="0" type="number" value={form.feeAmount} onChange={(e) => updateField('feeAmount', e.target.value)} disabled={!isDraft} /></label>
          </div>
          <label className="event-checkbox"><input type="checkbox" checked={form.allowLateCancellation} onChange={(e) => updateField('allowLateCancellation', e.target.checked)} disabled={!isDraft} /> 신청 마감 후 취소 허용</label>
          {message && <p className="form-success" role="status">{message}</p>}
          {mutationError && <p className="form-error" role="alert">{mutationError.message}</p>}
          <div className="event-form-actions">
            {isDraft && <button className="primary-button" type="submit" disabled={saveMutation.isPending}>{saveMutation.isPending ? '저장 중…' : '초안 저장'}</button>}
            {selected?.status === 'DRAFT' && <button className="secondary-button" type="button" onClick={() => publishMutation.mutate()} disabled={publishMutation.isPending}>행사 공개</button>}
            {selected?.status === 'PUBLISHED' && <button className="secondary-button" type="button" onClick={() => closeMutation.mutate()} disabled={closeMutation.isPending}>행사 종료</button>}
            {selected && selected.status !== 'CANCELED' && <button className="danger-text-button" type="button" onClick={() => setConfirmingCancellation(true)} disabled={cancelMutation.isPending}>행사 취소</button>}
            {selected?.status === 'DRAFT' && !confirmingDelete && <button className="danger-text-button" type="button" onClick={() => setConfirmingDelete(true)}>초안 삭제</button>}
            {selected?.status === 'DRAFT' && confirmingDelete && (
              <>
                <button className="danger-button" type="button" onClick={() => deleteMutation.mutate()} disabled={deleteMutation.isPending}>삭제 확정</button>
                <button className="text-button" type="button" onClick={() => setConfirmingDelete(false)}>삭제 취소</button>
              </>
            )}
          </div>
        </form>
      </section>
      {confirmingCancellation && selected && <ConfirmActionDialog title="행사를 취소할까요?" description={<p><strong>{selected.title}</strong>의 참가 신청과 납부 상태가 취소 또는 환불 대기 상태로 정리됩니다.</p>} confirmLabel="행사 취소" tone="danger" pending={cancelMutation.isPending} onCancel={() => setConfirmingCancellation(false)} onConfirm={() => cancelMutation.mutate()} />}
    </div>
  )
}
