import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { App } from '../app/App'
import { renderWithProviders } from '../test/render'

describe('LoginPage', () => {
  it('로컬 계정으로 로그인하면 신규 회원을 온보딩 화면으로 보낸다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/login'])

    await user.type(screen.getByLabelText('아이디'), 'local.member')
    await user.type(screen.getByLabelText('비밀번호'), 'clubpass123!')
    await user.click(screen.getByRole('button', { name: '아이디로 로그인' }))

    expect(await screen.findByRole('heading', { name: /어떤 파트에서/ })).toBeInTheDocument()
  })

  it('회원가입 후 생성한 아이디로 로그인할 수 있게 전환한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/login'])

    await user.click(screen.getByRole('button', { name: '회원가입 화면 열기' }))
    await user.type(screen.getByLabelText('아이디'), 'new.member')
    await user.type(screen.getByLabelText('비밀번호'), 'clubpass123!')
    await user.type(screen.getByLabelText('전화번호'), '010-1234-5678')
    await user.click(screen.getByRole('button', { name: '회원가입' }))

    expect(await screen.findByRole('status')).toHaveTextContent('회원가입이 완료됐어요')
    expect(screen.getByLabelText('아이디')).toHaveValue('new.member')
    expect(screen.getByRole('button', { name: '아이디로 로그인' })).toBeInTheDocument()
  })
})
