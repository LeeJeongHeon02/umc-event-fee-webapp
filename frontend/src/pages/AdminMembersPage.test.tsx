import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../test/render'
import { AdminMembersPage } from './AdminMembersPage'

describe('AdminMembersPage', () => {
  it('회원 상태와 역할을 조회한다', async () => {
    renderWithProviders(<AdminMembersPage />, ['/admin/members'])
    expect(await screen.findByRole('heading', { name: '회원 관리' })).toBeInTheDocument()
    expect(screen.getByText('PE(Web) 홍길동')).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'PE(Web) 홍길동 역할' })).toHaveValue('STAFF')
  })
})
