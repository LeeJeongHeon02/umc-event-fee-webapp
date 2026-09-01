import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { App } from '../app/App'
import { renderWithProviders } from '../test/render'

describe('AdminFeePaymentsPage', () => {
  it('회비 납부 내역에서 신고를 승인하고 상태를 갱신한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/admin/fees/7/payments'])

    expect(await screen.findByRole('heading', { name: '2026년 2학기 회비' })).toBeInTheDocument()
    const memberRow = screen.getByText('PE(Web) 홍길동').closest('tr')!
    expect(within(memberRow).getByText('확인 대기')).toBeInTheDocument()

    await user.click(within(memberRow).getByRole('button', { name: '홍길동 납부 승인' }))

    await waitFor(() => {
      const updatedRow = screen.getByText('PE(Web) 홍길동').closest('tr')!
      expect(within(updatedRow).getByText('납부 완료')).toBeInTheDocument()
    })
  })
})
