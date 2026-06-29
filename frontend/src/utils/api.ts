const API_BASE = 'http://localhost:8081'

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
