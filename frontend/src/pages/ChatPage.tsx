import { useEffect, useMemo, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import ReactMarkdown, { defaultUrlTransform } from 'react-markdown'
import { Link, useParams } from 'react-router-dom'
import { api, openBookPage, type Book, type Citation, type Message } from '../api'

const SUGGESTIONS = [
  'Summarize the main ideas of this book.',
  'Which key terms should I know, and what do they mean?',
  'Explain the hardest concept in simple terms.',
]

function AssistantMessage({
  message,
  onOpenPage,
}: {
  message: Message
  onOpenPage: (page: number) => void
}) {
  const byIndex = useMemo(() => new Map(message.citations.map((c) => [c.index, c])), [message.citations])
  // Turn [1] markers into links that react-markdown hands to our own renderer below.
  const text = useMemo(() => message.content.replace(/\[(\d{1,2})\]/g, '[$1](cite:$1)'), [message.content])

  return (
    <div>
      <div className="answer max-w-[68ch]">
        <ReactMarkdown
          urlTransform={(url) => (url.startsWith('cite:') ? url : defaultUrlTransform(url))}
          components={{
            a: ({ href, children }) => {
              if (href?.startsWith('cite:')) {
                const citation: Citation | undefined = byIndex.get(Number(href.slice(5)))
                if (!citation) return <span>{children}</span>
                return (
                  <button
                    type="button"
                    onClick={() => onOpenPage(citation.page)}
                    title={`Open page ${citation.page}`}
                    className="mark mx-0.5 rounded align-baseline font-sans text-xs font-bold hover:brightness-95"
                  >
                    {children}
                  </button>
                )
              }
              return (
                <a href={href} target="_blank" rel="noreferrer" className="text-signal underline">
                  {children}
                </a>
              )
            },
          }}
        >
          {text}
        </ReactMarkdown>
      </div>

      {message.citations.length > 0 && (
        <div className="mt-5 max-w-[68ch]">
          <h3 className="text-sm font-semibold text-slate">Sources</h3>
          <ul className="mt-2 space-y-3">
            {message.citations.map((c) => (
              <li key={c.index} className="flex items-start gap-3">
                <button
                  type="button"
                  onClick={() => onOpenPage(c.page)}
                  title="Open this page in the PDF"
                  className="shrink-0 rounded bg-ink px-2 py-0.5 text-xs font-semibold text-white hover:bg-signal"
                >
                  p. {c.page}
                </button>
                <p className="answer text-sm! leading-relaxed!">
                  <span className="mark">{c.snippet}</span>
                </p>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

export default function ChatPage() {
  const { bookId = '' } = useParams()
  const [book, setBook] = useState<Book | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [question, setQuestion] = useState('')
  const [asking, setAsking] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const bottom = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let cancelled = false
    Promise.all([api.getBook(bookId), api.messages(bookId)])
      .then(([b, m]) => {
        if (cancelled) return
        setBook(b)
        setMessages(m)
      })
      .catch((e) => {
        if (!cancelled) setLoadError(e instanceof Error ? e.message : 'Could not load this book.')
      })
    return () => {
      cancelled = true
    }
  }, [bookId])

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages.length, asking])

  async function ask(text: string) {
    const trimmed = text.trim()
    if (!trimmed || asking) return
    setError(null)
    setAsking(true)
    setQuestion('')
    const pending: Message = {
      id: `pending-${Date.now()}`,
      role: 'user',
      content: trimmed,
      citations: [],
      createdAt: new Date().toISOString(),
    }
    setMessages((prev) => [...prev, pending])
    try {
      const answer = await api.ask(bookId, trimmed)
      setMessages((prev) => [...prev, answer])
    } catch (e) {
      // Take the question back out of the thread and put it in the box so nothing is lost.
      setMessages((prev) => prev.filter((m) => m.id !== pending.id))
      setQuestion(trimmed)
      setError(e instanceof Error ? e.message : 'Could not get an answer. Please try again.')
    } finally {
      setAsking(false)
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    void ask(question)
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      void ask(question)
    }
  }

  async function openPage(page: number) {
    try {
      await openBookPage(bookId, page)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'The PDF could not be opened.')
    }
  }

  async function clearChat() {
    if (!window.confirm('Clear this conversation? The book stays in your library.')) return
    try {
      await api.clearMessages(bookId)
      setMessages([])
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not clear the conversation.')
    }
  }

  if (loadError) {
    return (
      <div className="mx-auto w-full max-w-3xl px-5 py-10">
        <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-danger">
          {loadError}
        </p>
        <Link to="/" className="mt-4 inline-block font-semibold text-signal hover:underline">
          Back to your library
        </Link>
      </div>
    )
  }
  if (!book) return <p className="p-8 text-slate">Loading…</p>

  const ready = book.status === 'READY'

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col px-5">
      <div className="flex items-start justify-between gap-4 py-6">
        <div className="min-w-0">
          <Link to="/" className="text-sm font-medium text-signal hover:underline">
            Your library
          </Link>
          <h1 className="mt-1 truncate text-2xl font-bold tracking-tight">{book.title}</h1>
          <p className="text-sm text-slate">{book.pageCount} pages</p>
        </div>
        {messages.length > 0 && (
          <button onClick={() => void clearChat()} className="shrink-0 text-sm font-medium text-slate hover:text-danger">
            Clear chat
          </button>
        )}
      </div>

      <div className="flex-1 space-y-8 pb-8">
        {messages.length === 0 && !asking && (
          <div>
            <p className="text-slate">Ask anything about this book. Answers come from its pages, with sources.</p>
            <div className="mt-4 flex flex-col items-start gap-2">
              {SUGGESTIONS.map((s) => (
                <button
                  key={s}
                  disabled={!ready}
                  onClick={() => void ask(s)}
                  className="rounded-full border border-line bg-white px-4 py-2 text-left text-sm font-medium hover:border-signal disabled:opacity-50"
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m) =>
          m.role === 'user' ? (
            <div key={m.id} className="flex justify-end">
              <p className="max-w-[85%] whitespace-pre-wrap rounded-2xl rounded-br-sm bg-ink px-4 py-2.5 text-white">
                {m.content}
              </p>
            </div>
          ) : (
            <AssistantMessage key={m.id} message={m} onOpenPage={(p) => void openPage(p)} />
          ),
        )}

        {asking && (
          <p className="text-slate" role="status">
            Searching the book and writing an answer…
          </p>
        )}
        <div ref={bottom} />
      </div>

      <div className="sticky bottom-0 -mx-5 border-t border-line bg-paper/95 px-5 pb-5 pt-3 backdrop-blur">
        {error && (
          <p role="alert" className="mb-2 rounded-md bg-red-50 px-3 py-2 text-sm text-danger">
            {error}
          </p>
        )}
        {!ready && (
          <p className="mb-2 text-sm text-slate">This book isn&apos;t ready for questions yet. Go back to your library.</p>
        )}
        <form onSubmit={onSubmit} className="flex items-end gap-2">
          <textarea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={onKeyDown}
            rows={2}
            maxLength={2000}
            disabled={!ready}
            placeholder="Ask a question about this book"
            aria-label="Your question"
            className="min-h-12 flex-1 resize-none rounded-md border border-line bg-white px-3 py-2 outline-none focus:border-signal disabled:opacity-60"
          />
          <button
            type="submit"
            disabled={!ready || asking || !question.trim()}
            className="rounded-md bg-signal px-4 py-2.5 font-semibold text-white hover:bg-signal-dark disabled:opacity-50"
          >
            Ask
          </button>
        </form>
      </div>
    </div>
  )
}
