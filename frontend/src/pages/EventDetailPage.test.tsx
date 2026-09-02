import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { App } from '../app/App'
import { renderWithProviders } from '../test/render'

describe('EventDetailPage', () => {
  it('참가 신청을 취소하고 참가비 부과 취소 상태를 보여준다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/events/42'])

    await user.click(await screen.findByRole('button', { name: '참가 취소' }))
    expect(screen.getByText(/납부 완료된 참가비는 환불 대기로 전환/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '참가 취소 확정' }))

    expect(await screen.findByRole('heading', { name: '참가 신청이 취소됐어요.' })).toBeInTheDocument()
    expect(screen.getByText('부과 취소')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '참가 취소 완료' })).toBeDisabled()
  })
})
