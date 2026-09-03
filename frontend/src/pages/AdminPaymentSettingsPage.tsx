import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { createAdminPaymentSetting, getAdminPaymentSettings } from '../services/api'
import { formatDateTime } from '../services/format'

const empty = { bankName: '', accountNumber: '', accountHolder: '', kakaoPayReceiveUrl: '' }

export function AdminPaymentSettingsPage() {
  const client = useQueryClient()
  const settings = useQuery({ queryKey: ['admin', 'payment-settings'], queryFn: getAdminPaymentSettings })
  const [form, setForm] = useState(empty)
  const create = useMutation({ mutationFn: () => createAdminPaymentSetting(form), onSuccess: async () => { setForm(empty); await client.invalidateQueries({ queryKey: ['admin', 'payment-settings'] }) } })
  if (settings.isLoading) return <LoadingState label="송금정보를 불러오는 중" />
  if (settings.isError) return <ErrorState />
  return <div className="admin-page"><section className="admin-page-heading"><div><span className="eyebrow">PAYMENT SETTINGS</span><h1>송금정보 설정</h1><p>새 정보를 등록하면 기존 설정은 보존된 채 비활성화됩니다.</p></div></section>
    <section className="event-management-layout"><div className="admin-panel event-admin-list">{settings.data!.map((setting) => <div className="dues-admin-item" key={setting.id}><div><strong>{setting.bankName} {setting.accountNumber}</strong><small>{setting.accountHolder} · {formatDateTime(setting.createdAt)}</small></div><span className={setting.active ? 'form-success' : 'table-muted'}>{setting.active ? '현재 사용' : '이전 버전'}</span></div>)}</div>
      <form className="admin-panel event-admin-form" onSubmit={(event: FormEvent) => { event.preventDefault(); create.mutate() }}><div className="admin-panel-heading"><div><span className="eyebrow">NEW SETTING</span><h2>새 송금정보</h2></div></div><label>은행<input required value={form.bankName} onChange={(e) => setForm({ ...form, bankName: e.target.value })} /></label><label>계좌번호<input required value={form.accountNumber} onChange={(e) => setForm({ ...form, accountNumber: e.target.value })} /></label><label>예금주<input required value={form.accountHolder} onChange={(e) => setForm({ ...form, accountHolder: e.target.value })} /></label><label>카카오페이 코드송금 URL<input type="url" value={form.kakaoPayReceiveUrl} onChange={(e) => setForm({ ...form, kakaoPayReceiveUrl: e.target.value })} /></label>{create.error && <p className="form-error">{create.error.message}</p>}<button className="primary-button" disabled={create.isPending}>등록하고 활성화</button></form>
    </section></div>
}
