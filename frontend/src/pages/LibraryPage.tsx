import { useCallback, useEffect, useRef, useState, type DragEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, formatBytes, type Book } from '../api'

const POLL_MS = 3000

function StatusBadge({ book }: { book: Book }) {
  if (book.status === 'PROCESSING') {
    return (
      <span className="inline-flex items-center gap-1.5 text-sm font-medium text-slate">
        <span className="h-2 w-2 animate-pulse rounded-full bg-signal" aria-hidden />
        Indexing…
      </span>
    )
  }
  if (book.status === 'FAILED') {
    return <span className="text-sm font-medium text-danger">Failed</span>
  }
  return <span className="text-sm font-medium text-ok">Ready</span>
}

export default function LibraryPage() {
  const [books, setBooks] = useState<Book[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const [dragging, setDragging] = useState(false)
  const fileInput = useRef<HTMLInputElement>(null)

  const load = useCallback(async () => {
    try {
      setBooks(await api.listBooks())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load your library.')
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  // Keep refreshing while any book is still being indexed.
  const anyProcessing = books?.some((b) => b.status === 'PROCESSING') ?? false
  useEffect(() => {
    if (!anyProcessing) return
    const timer = setInterval(() => void load(), POLL_MS)
    return () => clearInterval(timer)
  }, [anyProcessing, load])

  async function upload(file: File | undefined) {
    if (!file) return
    setError(null)
    if (!file.name.toLowerCase().endsWith('.pdf') && file.type !== 'application/pdf') {
      setError('Only PDF files can be uploaded.')
      return
    }
    setUploading(true)
    try {
      await api.uploadBook(file)
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'The upload failed. Please try again.')
    } finally {
      setUploading(false)
      if (fileInput.current) fileInput.current.value = ''
    }
  }

  async function remove(book: Book) {
    if (!window.confirm(`Delete "${book.title}"? Its index and chat history will be removed.`)) return
    setError(null)
    try {
      await api.deleteBook(book.id)
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not delete this book.')
    }
  }

  function onDrop(e: DragEvent) {
    e.preventDefault()
    setDragging(false)
    void upload(e.dataTransfer.files[0])
  }

  return (
    <div className="mx-auto w-full max-w-4xl px-5 py-10">
      <h1 className="text-3xl font-bold tracking-tight">Your library</h1>

      <div
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={`mt-6 rounded-lg border-2 border-dashed p-8 transition-colors ${
          dragging ? 'border-signal bg-white' : 'border-line bg-white/60'
        }`}
      >
        <input
          ref={fileInput}
          type="file"
          accept="application/pdf,.pdf"
          className="sr-only"
          id="pdf-input"
          onChange={(e) => void upload(e.target.files?.[0])}
        />
        <p className="font-semibold">{uploading ? 'Uploading…' : 'Drop a PDF here'}</p>
        <p className="mt-1 text-sm text-slate">
          Books, papers, notes or documentation. Up to 50 MB. Scanned PDFs without selectable text aren&apos;t supported
          yet.
        </p>
        <button
          type="button"
          disabled={uploading}
          onClick={() => fileInput.current?.click()}
          className="mt-4 rounded-md bg-signal px-4 py-2 text-sm font-semibold text-white hover:bg-signal-dark disabled:opacity-60"
        >
          Choose a PDF
        </button>
      </div>

      {error && (
        <p role="alert" className="mt-4 rounded-md bg-red-50 px-3 py-2 text-sm text-danger">
          {error}
        </p>
      )}

      <section className="mt-8" aria-live="polite">
        {books === null ? (
          <p className="text-slate">Loading your books…</p>
        ) : books.length === 0 ? (
          <p className="text-slate">No books yet. Upload a PDF above and you can start asking questions about it.</p>
        ) : (
          <ul className="divide-y divide-line border-y border-line bg-white">
            {books.map((book) => (
              <li key={book.id} className="flex flex-wrap items-center gap-x-6 gap-y-2 px-4 py-4">
                <div className="min-w-0 flex-1">
                  {book.status === 'READY' ? (
                    <Link to={`/books/${book.id}`} className="block truncate text-lg font-semibold hover:text-signal">
                      {book.title}
                    </Link>
                  ) : (
                    <p className="truncate text-lg font-semibold">{book.title}</p>
                  )}
                  <p className="text-sm text-slate">
                    {book.pageCount} pages · {formatBytes(book.sizeBytes)}
                  </p>
                  {book.status === 'FAILED' && book.errorMessage && (
                    <p className="mt-1 text-sm text-danger">{book.errorMessage}</p>
                  )}
                </div>
                <StatusBadge book={book} />
                <div className="flex gap-2">
                  {book.status === 'READY' && (
                    <Link
                      to={`/books/${book.id}`}
                      className="rounded-md bg-ink px-3 py-1.5 text-sm font-semibold text-white hover:bg-ink/90"
                    >
                      Ask questions
                    </Link>
                  )}
                  <button
                    onClick={() => void remove(book)}
                    disabled={book.status === 'PROCESSING'}
                    className="rounded-md px-3 py-1.5 text-sm font-medium text-slate hover:bg-paper hover:text-danger disabled:opacity-40"
                  >
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
