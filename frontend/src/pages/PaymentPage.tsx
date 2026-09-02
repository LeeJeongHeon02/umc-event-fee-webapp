import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useParams } from 'react-router-dom'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { StatusBadge } from '../components/StatusBadge'
import { useCurrentMember } from '../hooks/useCurrentMember'
import { getPayment, reportPayment } from '../services/api'
import { formatDateTime, formatWon } from '../services/format'
import type { PaymentMethod } from '../services/types'

type ReportForm = {
  method: PaymentMethod
  senderName: string
  transferConfirmed: boolean
}

export function PaymentPage() {
  const paymentId = Number(useParams().paymentId)
  const queryClient = useQueryClient()
  const [copied, setCopied] = useState<'account' | 'amount' | null>(null)
  const meQuery = useCurrentMember()
  const paymentQuery = useQuery({
    queryKey: ['payment', paymentId],
    queryFn: () => getPayment(paymentId),
    enabled: Number.isFinite(paymentId),
  })
  const { register, handleSubmit, setValue, formState: { errors } } = useForm<ReportForm>({
    defaultValues: { method: 'BANK_TRANSFER', senderName: '', transferConfirmed: false },
  })

  useEffect(() => {
    if (meQuery.data?.name) setValue('senderName', meQuery.data.name)
  }, [meQuery.data?.name, setValue])
  const reportMutation = useMutation({
    mutationFn: (values: ReportForm) => reportPayment(paymentId, {
      method: values.method,
      senderName: values.senderName,
      transferConfirmed: true,
      transferredAt: new Date().toISOString(),
      version: paymentQuery.data?.version ?? 0,
    }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['payment', paymentId] })
      await queryClient.invalidateQueries({ queryKey: ['my-payments'] })
      await queryClient.invalidateQueries({ queryKey: ['events'] })
    },
  })

  async function copyValue(value: string, kind: 'account' | 'amount') {
    try {
      await navigator.clipboard.writeText(value)
      setCopied(kind)
      window.setTimeout(() => setCopied(null), 1800)
    } catch {
      setCopied(null)
    }
  }

  if (paymentQuery.isLoading) return <LoadingState label="납부 정보를 불러오는 중" />
  if (paymentQuery.isError || !paymentQuery.data) return <ErrorState title="납부 항목을 찾지 못했어요" />

  const payment = paymentQuery.data
  const destination = payment.paymentDestination
  const canReport = ['UNPAID', 'REJECTED'].includes(payment.status)

  return (
    <div className="page payment-page">
      <Link className="back-link" to="/home">← 홈으로</Link>
      <header className="payment-heading">
        <div><span className="eyebrow">PAYMENT</span><StatusBadge status={payment.status} /></div>
        <h1>{payment.source.title}</h1>
        <p>{payment.type === 'EVENT_FEE' ? '행사 참가비' : '정기 회비'} · {formatWon(payment.amount)}</p>
      </header>

      {payment.status === 'REPORTED' && (
        <section className="waiting-card" aria-live="polite">
          <div className="waiting-card__icon" aria-hidden="true">✓</div>
          <div>
            <span className="eyebrow">TRANSFER REPORTED</span>
            <h2>송금 신고가 접수됐어요.</h2>
            <p>총무가 실제 입금 내역을 확인하고 있습니다. 확인이 끝나면 납부 완료로 바뀌어요.</p>
          </div>
          {payment.latestReport && (
            <dl>
              <div><dt>송금자명</dt><dd>{payment.latestReport.senderName}</dd></div>
              <div><dt>신고 시각</dt><dd>{formatDateTime(payment.latestReport.reportedAt)}</dd></div>
            </dl>
          )}
        </section>
      )}

      {canReport && destination && (
        <>
          <section className="transfer-card">
            <span className="eyebrow">TRANSFER TO</span>
            <div className="transfer-card__account">
              <div><span>{destination.bankName} · {destination.accountHolder}</span><strong>{destination.accountNumber}</strong></div>
              <button type="button" onClick={() => copyValue(destination.accountNumber, 'account')}>
                {copied === 'account' ? '복사됨' : '계좌 복사'}
              </button>
            </div>
            <div className="transfer-card__amount">
              <div><span>보낼 금액</span><strong>{formatWon(payment.amount)}</strong></div>
              <button type="button" onClick={() => copyValue(String(payment.amount), 'amount')}>
                {copied === 'amount' ? '복사됨' : '금액 복사'}
              </button>
            </div>
            {destination.kakaoPayReceiveUrl && (
              <a className="kakao-pay-link" href={destination.kakaoPayReceiveUrl} target="_blank" rel="noreferrer">
                카카오페이로 송금하기 <span aria-hidden="true">↗</span>
              </a>
            )}
            <p className="transfer-help">코드송금에서는 금액이 자동 입력되지 않을 수 있어요. 위 금액을 확인해 주세요.</p>
          </section>

          <section className="report-section">
            <span className="eyebrow">AFTER TRANSFER</span>
            <h2>송금 후 알려주세요.</h2>
            <form onSubmit={handleSubmit((values) => reportMutation.mutate(values))}>
              <label className="field-label" htmlFor="method">송금 방법</label>
              <select className="select-input" id="method" {...register('method')}>
                <option value="BANK_TRANSFER">계좌 직접송금</option>
                <option value="KAKAO_PAY_CODE">카카오페이 코드송금</option>
              </select>

              <label className="field-label" htmlFor="senderName">송금자명</label>
              <input
                className="text-input"
                id="senderName"
                {...register('senderName', { required: '송금자명을 입력해 주세요.' })}
              />
              {errors.senderName && <p className="field-error">{errors.senderName.message}</p>}

              <label className="check-row">
                <input type="checkbox" {...register('transferConfirmed', { required: true })} />
                <span>실제로 송금을 완료했습니다.</span>
              </label>
              {errors.transferConfirmed && <p className="field-error">송금 완료 여부를 확인해 주세요.</p>}

              {reportMutation.isError && <p className="form-error" role="alert">신고를 접수하지 못했어요. 납부 상태를 확인한 후 다시 시도해 주세요.</p>}
              <button className="primary-button primary-button--block" type="submit" disabled={reportMutation.isPending}>
                {reportMutation.isPending ? '접수 중…' : '송금했어요'}
              </button>
            </form>
          </section>
        </>
      )}

      {payment.status === 'CONFIRMED' && (
        <section className="success-card"><span aria-hidden="true">✓</span><h2>납부가 확인됐어요.</h2><p>운영진이 실제 입금 내역을 확인했습니다.</p></section>
      )}
    </div>
  )
}
