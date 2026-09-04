import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { App } from '../app/App'
import { activeMember } from '../mocks/fixtures'
import { server } from '../test/server'
import { renderWithProviders } from '../test/render'

describe('MyPage', () => {
  it('카카오 회원 정보와 운영진 메뉴를 보여준다', async () => {
    renderWithProviders(<App />, ['/mypage'])
    expect(await screen.findByRole('heading', { name: '마이페이지' })).toBeInTheDocument()
    expect(screen.getByText('카카오 로그인')).toBeInTheDocument()
    expect(screen.getByText('미등록')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '운영진 센터' })).toHaveAttribute('href', '/admin')
    expect(screen.getByRole('link', { name: '마이' })).toHaveAttribute('aria-current', 'page')
  })

  it('승인 대기 로컬 회원도 아이디·전화번호 확인 및 로그아웃이 가능하다', async () => {
    server.use(http.get(/\/api\/v1\/me$/, () => HttpResponse.json({ ...activeMember,
      role: 'MEMBER', status: 'PENDING', loginId: 'local.member', phoneNumber: '01012345678', kakaoProfileName: null,
    })))
    renderWithProviders(<App />, ['/mypage'])
    expect(await screen.findByText('local.member')).toBeInTheDocument()
    expect(screen.getByText('010-1234-5678')).toBeInTheDocument()
    expect(screen.getByText('승인 대기')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: '운영진 센터' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeEnabled()
  })

  it('로그아웃 성공 시 비공개 캐시를 지우고 로그인 화면으로 이동한다', async () => {
    const user = userEvent.setup()
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    client.setQueryData(['my-payments'], [{ id: 99, private: true }])
    render(<QueryClientProvider client={client}><MemoryRouter initialEntries={['/mypage']}><App /></MemoryRouter></QueryClientProvider>)
    await user.click(await screen.findByRole('button', { name: '로그아웃' }))
    expect(await screen.findByRole('button', { name: '아이디로 로그인' })).toBeInTheDocument()
    expect(client.getQueryData(['my-payments'])).toBeUndefined()
    expect(client.getQueryData(['me'])).toBeUndefined()
  })

  it('로그아웃 실패 시 화면을 유지하고 재시도를 안내한다', async () => {
    server.use(http.post(/\/api\/v1\/auth\/logout$/, () => HttpResponse.json({ detail: '오류' }, { status: 500 })))
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/mypage'])
    await user.click(await screen.findByRole('button', { name: '로그아웃' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('로그아웃하지 못했어요')
    expect(screen.getByRole('heading', { name: '마이페이지' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeEnabled()
  })

  it('이미 만료된 세션은 로그인 화면으로 정리한다', async () => {
    server.use(http.post(/\/api\/v1\/auth\/logout$/, () => new HttpResponse(null, { status: 401 })))
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/mypage'])
    await user.click(await screen.findByRole('button', { name: '로그아웃' }))
    expect(await screen.findByRole('button', { name: '아이디로 로그인' })).toBeInTheDocument()
  })

  it('비로그인 사용자는 마이페이지 대신 로그인 화면으로 이동한다', async () => {
    server.use(http.get(/\/api\/v1\/me$/, () => new HttpResponse(null, { status: 401 })))
    renderWithProviders(<App />, ['/mypage'])
    expect(await screen.findByRole('button', { name: '아이디로 로그인' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: '마이페이지' })).not.toBeInTheDocument()
  })
})
