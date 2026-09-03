import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../test/render'
import { AdminDuesRoundsPage } from './AdminDuesRoundsPage'

describe('AdminDuesRoundsPage', () => {
  it('회비 차수 초안을 생성한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<AdminDuesRoundsPage />, ['/admin/dues'])
    expect(await screen.findByRole('heading', { name: '회비 관리' })).toBeInTheDocument()
    await user.type(screen.getByRole('textbox', { name: '제목' }), '겨울 회비')
    await user.click(screen.getByRole('button', { name: '초안 저장' }))
    expect(await screen.findByText('겨울 회비')).toBeInTheDocument()
  })
})
