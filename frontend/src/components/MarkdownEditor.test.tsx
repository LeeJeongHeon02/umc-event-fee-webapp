import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { MarkdownEditor } from './MarkdownEditor'

function Editor({ initial = '', disabled = false }) {
  const [value, setValue] = useState(initial)
  return <MarkdownEditor value={value} onChange={setValue} disabled={disabled} />
}

describe('MarkdownEditor', () => {
  it('선택한 글자에 서식을 적용하고 미리보기 전환 후 원문을 유지한다', async () => {
    const user = userEvent.setup()
    render(<Editor initial="노트북 준비" />)
    const input = screen.getByRole('textbox', { name: '상세 내용' }) as HTMLTextAreaElement
    input.focus()
    input.setSelectionRange(0, 3)
    await user.click(screen.getByRole('button', { name: '굵게' }))
    expect(input).toHaveValue('**노트북** 준비')
    await user.click(screen.getByRole('button', { name: '미리보기' }))
    expect(screen.getByText('노트북').tagName).toBe('STRONG')
    await user.click(screen.getByRole('button', { name: '작성' }))
    expect(input).toHaveValue('**노트북** 준비')
  })

  it('미리보기의 빈 필드 검증 실패 시 작성 화면으로 돌아가 포커스한다', async () => {
    const user = userEvent.setup()
    render(<Editor />)
    const input = screen.getByRole('textbox', { name: '상세 내용' })
    await user.click(screen.getByRole('button', { name: '미리보기' }))
    expect(screen.getByText('작성한 행사 안내가 여기에 표시됩니다.')).toBeVisible()
    fireEvent.invalid(input)
    await waitFor(() => expect(input).toHaveFocus())
    expect(input).toBeVisible()
  })

  it('공개된 행사는 서식 변경이 불가하지만 미리보기는 가능하다', async () => {
    const user = userEvent.setup()
    render(<Editor initial="## 공개 안내" disabled />)
    expect(screen.getByRole('textbox', { name: '상세 내용' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '제목' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: '미리보기' }))
    expect(screen.getByRole('heading', { name: '공개 안내' })).toBeVisible()
  })
})
