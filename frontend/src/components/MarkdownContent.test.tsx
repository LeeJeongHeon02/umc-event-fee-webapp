import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { MarkdownContent } from './MarkdownContent'

describe('MarkdownContent', () => {
  it('제목·강조·목록·표·코드를 렌더링하고 기존 일반 텍스트의 줄바꿈을 유지한다', () => {
    const { container } = render(<MarkdownContent>{'# 준비 안내\n\n**노트북**\n충전기\n\n- 준비물\n\n| 시간 | 내용 |\n| --- | --- |\n| 18:00 | 모임 |\n\n`npm test`'}</MarkdownContent>)
    expect(screen.getByRole('heading', { name: '준비 안내', level: 3 })).toBeInTheDocument()
    expect(screen.getByText('노트북').tagName).toBe('STRONG')
    expect(screen.getByRole('listitem')).toHaveTextContent('준비물')
    expect(screen.getByRole('table')).toHaveTextContent('18:00')
    expect(container.querySelector('br')).toBeInTheDocument()
    expect(screen.getByText('npm test').tagName).toBe('CODE')
  })

  it('HTML·위험한 링크·원격 이미지를 차단하고 안전한 링크만 새 창으로 연다', () => {
    const { container } = render(<MarkdownContent>{'<script>alert(1)</script>\n\n<img src="x" onerror="alert(1)">\n\n[위험](javascript:alert%281%29)\n\n[데이터](data:text/html,test)\n\n[상대 경로](/admin)\n\n[인코딩](jav&#x61;script:alert%281%29)\n\n![추적 이미지](https://example.com/tracker.png)\n\n[안내](https://example.com)\n\n[문의](mailto:club@example.com)'}</MarkdownContent>)
    expect(container.querySelector('script, img, iframe')).toBeNull()
    expect(screen.getAllByRole('link')).toHaveLength(2)
    expect(screen.getByRole('link', { name: '안내' })).toHaveAttribute('target', '_blank')
    expect(screen.getByRole('link', { name: '안내' })).toHaveAttribute('rel', 'noopener noreferrer')
    expect(screen.getByRole('link', { name: '문의' })).toHaveAttribute('href', 'mailto:club@example.com')
    expect(screen.getByText('[이미지 미지원: 추적 이미지]')).toBeInTheDocument()
  })
})
