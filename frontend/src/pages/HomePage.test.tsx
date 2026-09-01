import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { HomePage } from './HomePage'
import { renderWithProviders } from '../test/render'

describe('HomePage', () => {
  it('회원의 행사와 확인이 필요한 납부 항목을 보여준다', async () => {
    renderWithProviders(<HomePage />, ['/home'])

    expect(await screen.findByRole('heading', { name: /안녕하세요, 홍길동님/ })).toBeInTheDocument()
    expect(screen.getAllByText('2026 가을 해커톤').length).toBeGreaterThan(0)
    expect(screen.getByText('2026년 2학기 회비')).toBeInTheDocument()
    expect(screen.getByText('다가오는 행사')).toBeInTheDocument()
  })
})
