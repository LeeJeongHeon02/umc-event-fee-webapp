import { useMutation, useQueryClient } from '@tanstack/react-query'
import { reviewAdminPayment } from '../services/api'

interface AdminReviewActionsProps {
  memberName: string
  paymentId: number
  version: number
}

export function AdminReviewActions({ memberName, paymentId, version }: AdminReviewActionsProps) {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: (decision: 'confirm' | 'reject') =>
      reviewAdminPayment(paymentId, decision, { version }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin'] }),
  })

  return (
    <div className="review-actions">
      <button
        className="table-button table-button--confirm"
        type="button"
        aria-label={`${memberName} 납부 승인`}
        disabled={mutation.isPending}
        onClick={() => mutation.mutate('confirm')}
      >
        승인
      </button>
      <button
        className="table-button"
        type="button"
        aria-label={`${memberName} 납부 반려`}
        disabled={mutation.isPending}
        onClick={() => mutation.mutate('reject')}
      >
        반려
      </button>
      {mutation.isError && <span className="inline-error" role="alert">다시 시도해 주세요.</span>}
    </div>
  )
}

