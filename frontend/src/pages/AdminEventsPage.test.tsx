import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../test/render'
import { AdminEventsPage } from './AdminEventsPage'

describe('AdminEventsPage', () => {
  it('운영진이 새 행사 초안을 생성한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<AdminEventsPage />, ['/admin/events'])

    expect(await screen.findByRole('heading', { name: '행사 관리' })).toBeInTheDocument()
    await user.type(screen.getByRole('textbox', { name: '행사명' }), '신규 네트워킹')
    await user.type(screen.getByRole('textbox', { name: '상세 내용' }), '부원 네트워킹 행사입니다.')
    await user.click(screen.getByRole('button', { name: '초안 저장' }))

    expect(await screen.findByText('행사 초안을 저장했습니다.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '신규 네트워킹' })).toBeInTheDocument()
    expect(screen.getAllByText('초안')).toHaveLength(2)
  })
})
