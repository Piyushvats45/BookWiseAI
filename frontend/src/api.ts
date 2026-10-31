export interface User {
  id: string
  email: string
  displayName: string
}

export type BookStatus = 'PROCESSING' | 'READY' | 'FAILED'

export interface Book {
  id: string
  title: string
  originalFilename: string
  sizeBytes: number
  pageCount: number
  chunkCount: number
  status: BookStatus
  errorMessage: string | null
  createdAt: string
}

export interface Citation {
  index: number
  bookId: string
  bookTitle: string
  page: number
  snippet: string
  score: number | null
}

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  citations: Citation[]
  createdAt: string
}

const TOKEN_KEY = 'bookwise.token'

export const tokenStore = {
  get: () => {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set: (token: string) => {
    try {
      localStorage.setItem(TOKEN_KEY, token)
    } catch {
      /* storage unavailable: the session just won't survive a reload */
    }
  },
  clear: () => {
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* nothing to clear */
    }
  },
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

/** Called when the server says the token is no longer valid. */
let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')

  let res: Response
  try {
    res = await fetch(`/api${path}`, { ...init, headers })
  } catch {
    throw new ApiError(0, "Can't reach the server. Check your connection and try again.")
  }

  if (!res.ok) {
    let detail = 'Something went wrong. Please try again.'
    try {
      const body = await res.json()
      if (typeof body.detail === 'string' && body.detail) detail = body.detail
    } catch {
      /* non-JSON error body */
    }
    if (res.status === 401 && token && !path.startsWith('/auth/')) onUnauthorized()
    throw new ApiError(res.status, detail)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const api = {
  register: (displayName: string, email: string, password: string) =>
    request<{ token: string; user: User }>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ displayName, email, password }),
    }),
  login: (email: string, password: string) =>
    request<{ token: string; user: User }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),
  me: () => request<User>('/auth/me'),

  listBooks: () => request<Book[]>('/books'),
  getBook: (id: string) => request<Book>(`/books/${id}`),
  uploadBook: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<Book>('/books', { method: 'POST', body: form })
  },
  deleteBook: (id: string) => request<void>(`/books/${id}`, { method: 'DELETE' }),

  messages: (bookId: string) => request<Message[]>(`/books/${bookId}/messages`),
  ask: (bookId: string, question: string) =>
    request<Message>(`/books/${bookId}/chat`, { method: 'POST', body: JSON.stringify({ question }) }),
  clearMessages: (bookId: string) => request<void>(`/books/${bookId}/messages`, { method: 'DELETE' }),
}

/**
 * Opens the original PDF at a given page. The file endpoint needs the auth header, so the PDF is
 * fetched as a blob and opened from an object URL. The tab is opened first to avoid popup blockers.
 */
export async function openBookPage(bookId: string, page: number): Promise<void> {
  const tab = window.open('', '_blank')
  try {
    const token = tokenStore.get()
    const res = await fetch(`/api/books/${bookId}/file`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    if (!res.ok) throw new Error('file unavailable')
    const url = URL.createObjectURL(await res.blob())
    if (tab) tab.location.href = `${url}#page=${page}`
    else window.location.href = `${url}#page=${page}`
  } catch {
    tab?.close()
    throw new ApiError(0, 'The original PDF could not be opened.')
  }
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
