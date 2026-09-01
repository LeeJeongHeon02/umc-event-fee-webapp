import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../test/render'
import { AdminDashboardPage } from './AdminDashboardPage'

describe('AdminDashboardPage', () => {
  it('운영진에게 행사와 회비 현황을 요약해서 보여준다', async () => {
    renderWithProviders(<AdminDashboardPage />, ['/admin'])

    expect(await screen.findByRole('heading', { name: '오늘의 운영 현황' })).toBeInTheDocument()
    expect(screen.getByText('확인 대기 송금')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /2026 가을 해커톤/ })).toHaveAttribute('href', '/admin/events/42/participants')
    expect(screen.getByRole('link', { name: '확인하러 가기 →' })).toHaveAttribute('href', '/admin/fees/7/payments')
  })
})

