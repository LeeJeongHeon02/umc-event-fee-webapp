import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { App } from '../app/App'
import { renderWithProviders } from '../test/render'

describe('OnboardingPage', () => {
  it('파트와 이름을 설정하면 승인 대기 화면으로 이동한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/onboarding'])

    await user.click(screen.getByLabelText(/Design/))
    const nameInput = screen.getByLabelText('이름')
    await user.clear(nameInput)
    await user.type(nameInput, '김디자인')
    await user.click(screen.getByRole('button', { name: '이 이름으로 시작하기' }))

    expect(await screen.findByRole('heading', { name: /가입 신청이/ })).toBeInTheDocument()
    expect(await screen.findByText('Design 김디자인')).toBeInTheDocument()
  })
})
