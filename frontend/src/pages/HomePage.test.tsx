import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { HomePage } from './HomePage'
import { renderWithProviders } from '../test/render'

describe('HomePage', () => {
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
  })
})
