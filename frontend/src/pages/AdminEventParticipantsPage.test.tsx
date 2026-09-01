import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { App } from '../app/App'
import { renderWithProviders } from '../test/render'

describe('AdminEventParticipantsPage', () => {
  it('행사 참가자의 송금 신고를 승인한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/admin/events/42/participants'])

    expect(await screen.findByRole('heading', { name: '2026 가을 해커톤' })).toBeInTheDocument()
    const memberRow = screen.getByText('Design 김민지').closest('tr')!
    expect(within(memberRow).getByText('확인 대기')).toBeInTheDocument()

    await user.click(within(memberRow).getByRole('button', { name: '김민지 납부 승인' }))

    await waitFor(() => {
      const updatedRow = screen.getByText('Design 김민지').closest('tr')!
      expect(within(updatedRow).getByText('납부 완료')).toBeInTheDocument()
    })
  })
})
