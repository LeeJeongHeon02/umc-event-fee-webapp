import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import remarkBreaks from 'remark-breaks'
import '../styles/markdown.css'

// Only explicit web/mail links are interactive. Raw HTML and remote images are
// deliberately unsupported: previews must not execute code or load tracking images.
function safeLink(url: string) {
  return /^(https?:\/\/|mailto:)/i.test(url) ? url : undefined
}

export function MarkdownContent({ children }: { children: string }) {
  return (
    <div className="markdown-content">
      <ReactMarkdown
        skipHtml
        remarkPlugins={[remarkGfm, remarkBreaks]}
        urlTransform={safeLink}
        components={{
          h1: ({ children }) => <h3>{children}</h3>,
          h2: ({ children }) => <h3>{children}</h3>,
          h3: ({ children }) => <h4>{children}</h4>,
          a: ({ href, children }) => href
            ? <a href={href} target="_blank" rel="noopener noreferrer">{children}</a>
            : <span>{children}</span>,
          img: ({ alt }) => <span className="markdown-image-note">[이미지 미지원{alt ? `: ${alt}` : ''}]</span>,
          table: ({ children }) => (
            <div className="markdown-table-scroll" role="region" aria-label="행사 안내 표 (가로 스크롤)" tabIndex={0}>
              <table>{children}</table>
            </div>
          ),
        }}
      >{children}</ReactMarkdown>
    </div>
  )
}
