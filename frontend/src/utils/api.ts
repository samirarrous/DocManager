const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8081'

interface UserSession {
  token: string
}

export async function apiRequest(endpoint: string, options: RequestInit = {}): Promise<Response> {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`
  
  // Prepare headers
  const headers = new Headers(options.headers || {})
  
  // Get token from localStorage
  const sessionStr = localStorage.getItem('docmanager_session')
  if (sessionStr) {
    try {
      const session = JSON.parse(sessionStr) as UserSession
      if (session && session.token) {
        headers.set('Authorization', `Bearer ${session.token}`)
      }
    } catch (e) {
      // Ignored
    }
  }

  // Set default Content-Type to JSON if a body is present and is not FormData
  if (options.body && !(options.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(url, {
    ...options,
    headers
  })

  // Auto-handle 401 Unauthorized
  if (response.status === 401) {
    localStorage.removeItem('docmanager_session')
    window.dispatchEvent(new Event('unauthorized-logout'))
  }

  return response
}

/**
 * Extracts a user-friendly error message from an API response.
 * If the response is JSON, it pulls the "error" or "message" key.
 * Otherwise, it falls back to raw text or a default status code message.
 */
export async function getErrorMessage(res: Response): Promise<string> {
  try {
    // Clone response before reading it because body can only be read once
    const clonedRes = res.clone()
    const data = await clonedRes.json()
    return data.error || data.message || `Error status ${res.status}`
  } catch (e) {
    try {
      const text = await res.text()
      return text || `Error status ${res.status}`
    } catch (ex) {
      return `Error status ${res.status}`
    }
  }
}
