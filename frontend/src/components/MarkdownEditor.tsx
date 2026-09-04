import { useId, useRef, useState } from 'react'
import { MarkdownContent } from './MarkdownContent'

type Props = { value: string; onChange: (value: string) => void; disabled?: boolean }

export function MarkdownEditor({ value, onChange, disabled = false }: Props) {
  const id = useId()
  const input = useRef<HTMLTextAreaElement>(null)
  const [preview, setPreview] = useState(false)
  const [invalid, setInvalid] = useState(false)

  const insert = (before: string, after: string, placeholder: string, line = false) => {
    const field = input.current
    if (!field || disabled) return
    const start = field.selectionStart
    const end = field.selectionEnd
    const text = value.slice(start, end) || placeholder
    const prefix = line && start > 0 && value[start - 1] !== '\n' ? `\n${before}` : before
    onChange(value.slice(0, start) + prefix + text + after + value.slice(end))
    requestAnimationFrame(() => {
      field.focus()
      field.setSelectionRange(start + prefix.length, start + prefix.length + text.length)
    })
  }

  return (
    <div className="markdown-editor">
      <div className="markdown-editor-heading">
        <label htmlFor={id}>상세 내용</label>
        <div className="markdown-mode" role="group" aria-label="상세 내용 편집 모드">
          <button type="button" aria-pressed={!preview} onClick={() => setPreview(false)}>작성</button>
          <button type="button" aria-pressed={preview} onClick={() => setPreview(true)}>미리보기</button>
        </div>
      </div>
      <div hidden={preview}>
        <div className="markdown-toolbar" role="group" aria-label="Markdown 서식">
          <button type="button" disabled={disabled} onClick={() => insert('## ', '', '제목', true)}>제목</button>
          <button type="button" disabled={disabled} onClick={() => insert('**', '**', '강조할 내용')}>굵게</button>
          <button type="button" disabled={disabled} onClick={() => insert('- ', '', '목록 항목', true)}>목록</button>
          <button type="button" disabled={disabled} onClick={() => insert('[', '](https://example.com)', '링크 이름')}>링크</button>
        </div>
        <textarea
          ref={input} id={id} required rows={10} value={value} disabled={disabled}
          aria-describedby={`${id}-help`} aria-invalid={invalid || undefined}
          onChange={(event) => { setInvalid(false); onChange(event.target.value) }}
          placeholder={'## 행사 안내\n\n- 준비물: 노트북\n- 참가비: **15,000원**'}
          onInvalid={(event) => {
            // A hidden required field must become focusable when saving a blank preview.
            event.preventDefault()
            setInvalid(true)
            setPreview(false)
            requestAnimationFrame(() => input.current?.focus())
          }}
        />
      </div>
      {preview && (
        <section className="markdown-preview" aria-label="행사 안내 미리보기">
          {value.trim() ? <MarkdownContent>{value}</MarkdownContent> : <p className="markdown-empty">작성한 행사 안내가 여기에 표시됩니다.</p>}
        </section>
      )}
      {invalid && <p className="form-error" role="alert">행사 상세 내용을 작성해 주세요.</p>}
      <p className="markdown-help" id={`${id}-help`}>Markdown으로 작성할 수 있어요. HTML과 이미지는 지원하지 않습니다.</p>
      <details className="markdown-guide">
        <summary>작성 문법 보기</summary>
        <ul>
          <li><code>## 제목</code> · 소제목</li>
          <li><code>**중요한 내용**</code> · 굵게</li>
          <li><code>- 준비물</code> · 목록</li>
          <li><code>[안내 링크](https://example.com)</code> · 새 창으로 열기</li>
        </ul>
        <p>줄바꿈은 그대로 표시됩니다. 표는 아래처럼 작성하세요.</p>
        <pre>{'| 시간 | 내용 |\n| --- | --- |\n| 18:00 | 모임 시작 |'}</pre>
      </details>
    </div>
  )
}
