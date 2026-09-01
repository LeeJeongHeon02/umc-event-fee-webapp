import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { App } from '../app/App'
import { renderWithProviders } from '../test/render'

describe('PaymentPage', () => {
  it('송금 신고 후 운영진 확인 대기 상태를 보여준다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/payments/311'])

    expect(await screen.findByText('3333-12-3456789')).toBeInTheDocument()
    await user.click(screen.getByLabelText('실제로 송금을 완료했습니다.'))
    await user.click(screen.getByRole('button', { name: '송금했어요' }))

    expect(await screen.findByText('송금 신고가 접수됐어요.')).toBeInTheDocument()
    expect(screen.getByText('확인 대기')).toBeInTheDocument()
  })
})
