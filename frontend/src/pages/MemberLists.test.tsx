import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { App } from '../app/App'
import { eventFixtures, paymentFixture, duesFixture } from '../mocks/fixtures'
import { server } from '../test/server'
import { renderWithProviders } from '../test/render'
import type { PaymentStatus } from '../services/types'

function mockPayments() {
  const states: PaymentStatus[] = ['UNPAID', 'REJECTED', 'REPORTED', 'CONFIRMED', 'REFUND_PENDING', 'REFUNDED', 'VOID', 'NOT_REQUIRED']
  server.use(http.get(/\/api\/v1\/me\/payment-obligations$/, () => HttpResponse.json({
    items: states.map((status, i) => ({ ...paymentFixture, id: 900 + i, status, source: { ...paymentFixture.source, title: status + ' 행사' } })),
    page: 0, size: 8, totalElements: 8, totalPages: 1,
  })))
}

describe('회원 행사·납부 페이지', () => {
  it('하단 메뉴가 다른 페이지를 열고 행사 상세에서도 행사 메뉴를 선택한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/events'])
    await screen.findByRole('heading', { name: '행사', level: 1 })
    expect(screen.queryByRole('heading', { name: /안녕하세요/ })).not.toBeInTheDocument()
    const menu = screen.getByRole('navigation', { name: '주요 메뉴' })
    expect(within(menu).getByRole('link', { name: '행사' })).toHaveAttribute('aria-current', 'page')
    await user.click(screen.getByRole('link', { name: /2026 가을 해커톤/ }))
    expect(await screen.findByRole('link', { name: '← 행사 목록' })).toHaveAttribute('href', '/events')
    expect(within(menu).getByRole('link', { name: '행사' })).toHaveAttribute('aria-current', 'page')
    await user.click(within(menu).getByRole('link', { name: '납부' }))
    expect(await screen.findByRole('heading', { name: '납부 내역', level: 1 })).toBeInTheDocument()
    expect(within(menu).getByRole('link', { name: '납부' })).toHaveAttribute('aria-current', 'page')
  })

  it('행사를 검색하고 신청·미신청·취소 여부로 필터링한다', async () => {
    server.use(http.get(/\/api\/v1\/events$/, () => HttpResponse.json({
      items: [...eventFixtures, { ...eventFixtures[0], id: 99, title: '취소한 행사', myParticipationStatus: 'CANCELED' }],
      page: 0, size: 4, totalElements: 4, totalPages: 1,
    })))
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/events?participation=JOINED'])
    await screen.findByRole('heading', { name: '행사 목록' })
    expect(screen.getByText('참가 신청 완료')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /신입 부원/ })).not.toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('참여 상태'), 'CANCELED')
    expect(screen.getByRole('link', { name: /취소한 행사/ })).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('참여 상태'), 'NOT_JOINED')
    await user.type(screen.getByLabelText('행사 검색'), 'Discord')
    expect(screen.getByRole('link', { name: /사이드 프로젝트/ })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /신입 부원/ })).not.toBeInTheDocument()
  })

  it('전체 납부에 완료·환불·취소도 포함하고 상태별로 필터링한다', async () => {
    mockPayments()
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/payments'])
    await screen.findByRole('heading', { name: '납부 내역', level: 1 })
    expect(screen.getByRole('link', { name: /CONFIRMED 행사/ })).toBeInTheDocument()
    for (const status of ['CONFIRMED', 'REFUNDED', 'VOID', 'NOT_REQUIRED']) {
      await user.selectOptions(screen.getByLabelText('납부 상태'), status)
      expect(screen.getByRole('link', { name: new RegExp(status + ' 행사') })).toBeInTheDocument()
      expect(screen.queryByRole('link', { name: /UNPAID 행사/ })).not.toBeInTheDocument()
    }
    await user.selectOptions(screen.getByLabelText('납부 상태'), 'NEEDS_PAYMENT')
    expect(screen.getByRole('link', { name: /UNPAID 행사/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /REJECTED 행사/ })).toBeInTheDocument()
  })

  it('회비 구분과 검색을 함께 적용한다', async () => {
    server.use(http.get(/\/api\/v1\/me\/payment-obligations$/, () => HttpResponse.json({
      items: [paymentFixture, duesFixture], page: 0, size: 2, totalElements: 2, totalPages: 1,
    })))
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/payments?type=MEMBERSHIP_DUE&status=REPORTED'])
    await screen.findByRole('heading', { name: '내 납부 목록' })
    expect(screen.getByRole('link', { name: /2026년 2학기 회비/ })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /2026 가을 해커톤/ })).not.toBeInTheDocument()
    await user.type(screen.getByLabelText('납부 검색'), '없는 이름')
    expect(screen.getByText('조건에 맞는 납부 내역이 없어요.')).toBeInTheDocument()
  })

  it.each([['#events', '행사'], ['#payments', '납부 내역']])('기존 홈 %s 주소를 새 페이지로 연결한다', async (hash, heading) => {
    renderWithProviders(<App />, ['/home' + hash])
    expect(await screen.findByRole('heading', { name: heading, level: 1 })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: /안녕하세요/ })).not.toBeInTheDocument()
  })

  it('없는 필터 값은 전체 상태로 처리한다', async () => {
    mockPayments()
    renderWithProviders(<App />, ['/payments?type=unknown&status=unknown'])
    await screen.findByRole('heading', { name: '납부 내역', level: 1 })
    expect(screen.getByLabelText('납부 상태')).toHaveValue('ALL')
    expect(screen.getByLabelText('납부 구분')).toHaveValue('ALL')
    expect(screen.getByRole('link', { name: /CONFIRMED 행사/ })).toBeInTheDocument()
  })

  it.each(['/events', '/payments'])('비로그인 사용자는 %s에 접근할 수 없다', async (path) => {
    server.use(http.get(/\/api\/v1\/me$/, () => new HttpResponse(null, { status: 401 })))
    renderWithProviders(<App />, [path])
    expect(await screen.findByRole('button', { name: '아이디로 로그인' })).toBeInTheDocument()
  })

  it('납부 조회 실패를 안내하고 재시도할 수 있다', async () => {
    let requests = 0
    server.use(http.get(/\/api\/v1\/me\/payment-obligations$/, () => {
      requests++
      return requests === 1 ? new HttpResponse(null, { status: 500 }) : HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
    }))
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/payments'])
    await user.click(await screen.findByRole('button', { name: '다시 불러오기' }))
    expect(await screen.findByText('아직 납부 내역이 없어요.')).toBeInTheDocument()
  })
})
