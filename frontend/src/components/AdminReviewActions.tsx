import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { reviewAdminPayment } from '../services/api'
import { ConfirmActionDialog } from './ConfirmActionDialog'

interface AdminReviewActionsProps {
  memberName: string
  paymentId: number
  version: number
}

export function AdminReviewActions({ memberName, paymentId, version }: AdminReviewActionsProps) {
  const queryClient = useQueryClient()
  const [decision, setDecision] = useState<'confirm' | 'reject' | null>(null)
  const mutation = useMutation({
    mutationFn: (decision: 'confirm' | 'reject') =>
      reviewAdminPayment(paymentId, decision, { version }),
    onSuccess: async () => {
      setDecision(null)
      await queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })

  return (
    <div className="review-actions">
      <button
        className="table-button table-button--confirm"
        type="button"
        aria-label={`${memberName} 납부 승인`}
        disabled={mutation.isPending}
        onClick={() => setDecision('confirm')}
      >
        승인
      </button>
      <button
        className="table-button"
        type="button"
        aria-label={`${memberName} 납부 반려`}
        disabled={mutation.isPending}
        onClick={() => setDecision('reject')}
      >
        반려
      </button>
      {mutation.isError && <span className="inline-error" role="alert">다시 시도해 주세요.</span>}
      {decision && (
        <ConfirmActionDialog
          title={decision === 'confirm' ? '송금 신고를 승인할까요?' : '송금 신고를 반려할까요?'}
          description={<p><strong>{memberName}</strong>님의 신고 상태를 {decision === 'confirm' ? '납부 완료' : '반려'}로 변경합니다.</p>}
          confirmLabel={decision === 'confirm' ? '승인하기' : '반려하기'}
          tone={decision === 'reject' ? 'danger' : 'default'}
          pending={mutation.isPending}
          onCancel={() => setDecision(null)}
          onConfirm={() => mutation.mutate(decision)}
        />
      )}
    </div>
  )
}
