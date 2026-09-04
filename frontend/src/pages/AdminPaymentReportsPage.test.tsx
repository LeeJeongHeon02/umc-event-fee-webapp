import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { App } from '../app/App'
import { adminDuesPayments, createAdminDashboard } from '../mocks/fixtures'
import { renderWithProviders } from '../test/render'
import { server } from '../test/server'

describe('AdminPaymentReportsPage', () => {
  it('회비 차수 7이 없어도 대시보드에서 행사 신고로 이동한다', async () => {
    let legacyRequests = 0
    const report = { ...adminDuesPayments.find((item) => item.status === 'REPORTED')!,
      source: { type: 'EVENT', id: 91, title: '실제 운영 행사' } }
    server.use(
      http.get(/\/api\/v1\/admin\/dashboard$/, () => HttpResponse.json({ ...createAdminDashboard(), activeDuesRounds: [], reportedCount: 1 })),
      http.get(/\/api\/v1\/admin\/payment-reports$/, () => HttpResponse.json([report])),
      http.get(/\/api\/v1\/admin\/dues-rounds\/7\/payments$/, () => { legacyRequests++; return new HttpResponse(null, { status: 404 }) }),
    )
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/admin'])
    await user.click(await screen.findByRole('link', { name: '확인하러 가기 →' }))
    expect(await screen.findByRole('heading', { name: '송금 신고 확인' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '행사비 · 실제 운영 행사' })).toHaveAttribute('href', '/admin/events/91/participants')
    expect(legacyRequests).toBe(0)
  })

  it('행사비와 회비 신고를 함께 표시하고 승인·반려 후 목록을 갱신한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<App />, ['/admin/payment-reports'])
    await screen.findByRole('heading', { name: '송금 신고 확인' })
    expect(screen.getAllByRole('link', { name: /행사비 ·/ }).length).toBeGreaterThan(0)
    expect(screen.getAllByRole('link', { name: /회비 ·/ }).length).toBeGreaterThan(0)
    const row = screen.getAllByRole('row').find((item) => within(item).queryByRole('button', { name: /납부 승인/ }))!
    const name = within(row).getByRole('button', { name: /납부 승인/ }).getAttribute('aria-label')!
    await user.click(within(row).getByRole('button', { name }))
    await waitFor(() => expect(row).not.toBeInTheDocument())
    const secondRow = screen.getAllByRole('row').find((item) => within(item).queryByRole('button', { name: /납부 반려/ }))!
    await user.click(within(secondRow).getByRole('button', { name: /납부 반려/ }))
    await waitFor(() => expect(secondRow).not.toBeInTheDocument())
  })

  it('신고가 없으면 빈 상태를 표시한다', async () => {
    server.use(http.get(/\/api\/v1\/admin\/payment-reports$/, () => HttpResponse.json([])))
    renderWithProviders(<App />, ['/admin/payment-reports'])
    expect(await screen.findByText('확인이 필요한 송금 신고가 없습니다.')).toBeInTheDocument()
  })

  it('예전 회비 주소의 404는 원인과 복귀 링크를 안내한다', async () => {
    server.use(http.get(/\/api\/v1\/admin\/dues-rounds\/7\/payments$/, () => HttpResponse.json({ code: 'RESOURCE_NOT_FOUND' }, { status: 404 })))
    renderWithProviders(<App />, ['/admin/fees/7/payments'])
    expect(await screen.findByText('회비 차수를 찾을 수 없어요')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '송금 신고 목록으로' })).toHaveAttribute('href', '/admin/payment-reports')
  })
})
