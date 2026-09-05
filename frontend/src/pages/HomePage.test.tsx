import { screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { eventFixtures, paymentFixture } from '../mocks/fixtures'
import { server } from '../test/server'
import { HomePage } from './HomePage'
import { renderWithProviders } from '../test/render'

describe('HomePage', () => {
  beforeEach(() => { vi.useFakeTimers({ toFake: ['Date'] }); vi.setSystemTime(new Date('2026-09-05T00:00:00Z')) })
  afterEach(() => vi.useRealTimers())
  it('회원의 행사와 확인이 필요한 납부 항목을 보여준다', async () => {
    renderWithProviders(<HomePage />, ['/home'])

    expect(await screen.findByRole('heading', { name: /안녕하세요,\s*홍길동\s*님/ })).toBeInTheDocument()
    expect(screen.getAllByText('2026 가을 해커톤').length).toBeGreaterThan(0)
    expect(screen.getByText('2026년 2학기 회비')).toBeInTheDocument()
    expect(screen.getByText('다가오는 행사')).toBeInTheDocument()
    expect(screen.queryByText('이번 주도 같이 만들어봐요.')).not.toBeInTheDocument()
    const eventSection = screen.getByRole('heading', { name: '다가오는 행사' }).closest('section')!
    const overview = screen.getByLabelText('내 활동 요약')
    expect(eventSection.compareDocumentPosition(overview) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(screen.getAllByRole('link', { name: '전체보기' }).map((link) => link.getAttribute('href'))).toEqual(['/events', '/payments'])
  })

  it('홈 요약은 지난 행사를 제외하고 행사·납부를 3건씩만 보여준다', async () => {
    server.use(
      http.get(/\/api\/v1\/events$/, () => HttpResponse.json({ items: [
        ...Array.from({ length: 5 }, (_, i) => ({ ...eventFixtures[0], id: 100 + i, title: '행사 ' + i })),
        { ...eventFixtures[0], id: 200, title: '지난 행사', startsAt: '2020-01-01T00:00:00Z', endsAt: '2020-01-01T01:00:00Z' },
      ] })),
      http.get(/\/api\/v1\/me\/payment-obligations$/, () => HttpResponse.json({ items:
        Array.from({ length: 5 }, (_, i) => ({ ...paymentFixture, id: 100 + i, status: 'REPORTED', source: { ...paymentFixture.source, title: '납부 ' + i } })),
      })),
    )
    renderWithProviders(<HomePage />, ['/home'])
    await screen.findByRole('heading', { name: '다가오는 행사' })
    expect(screen.getAllByRole('link').filter((link) => link.getAttribute('href')?.match(/^\/events\/\d+$/))).toHaveLength(3)
    expect(screen.getAllByRole('link').filter((link) => link.getAttribute('href')?.match(/^\/payments\/\d+$/))).toHaveLength(3)
    expect(screen.queryByText('지난 행사')).not.toBeInTheDocument()
  })

  it('미납 또는 반려 납부는 보조 지표보다 먼저 행동 카드로 안내한다', async () => {
    renderWithProviders(<HomePage />, ['/home'])

    const priorityTitle = await screen.findByRole('heading', { name: /납부를 확인해 주세요/ })
    const overview = screen.getByLabelText('내 활동 요약')
    expect(priorityTitle.closest('section')!.compareDocumentPosition(overview) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(screen.getByRole('link', { name: '납부 내역 보기' })).toHaveAttribute('href', '/payments?status=NEEDS_PAYMENT')
    expect(screen.getByRole('link', { name: /참여 중인 행사/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /입금 확인 대기/ })).toBeInTheDocument()
  })
})
