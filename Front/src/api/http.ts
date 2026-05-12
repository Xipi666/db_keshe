import type { AuthSession } from '../types/auth'
import { appendFrontendLog } from '../utils/runtimeLog'

export async function apiGet<TResponse>(
  url: string,
  session: AuthSession,
  params?: object,
): Promise<TResponse> {
  const query = new URLSearchParams()
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value))
    }
  })

  const target = query.size > 0 ? `${url}?${query.toString()}` : url
  const response = await fetch(target, {
    headers: authHeaders(session),
  })

  if (!response.ok) {
    const message = await readErrorMessage(response)
    appendFrontendLog('ERROR', `GET ${url} 请求失败`, `${response.status} ${message}`)
    throw new Error(message)
  }

  appendFrontendLog('INFO', `GET ${url} 请求成功`)
  return response.json() as Promise<TResponse>
}

export async function apiPost<TResponse>(
  url: string,
  session: AuthSession,
  body?: unknown,
  method = 'POST',
): Promise<TResponse> {
  const response = await fetch(url, {
    method,
    headers: {
      ...authHeaders(session),
      'Content-Type': 'application/json',
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    const message = await readErrorMessage(response)
    appendFrontendLog('ERROR', `${method} ${url} 请求失败`, `${response.status} ${message}`)
    throw new Error(message)
  }

  appendFrontendLog('INFO', `${method} ${url} 请求成功`)
  return response.json() as Promise<TResponse>
}

function authHeaders(session: AuthSession) {
  return {
    'X-User-Id': String(session.id),
    'X-Role-Code': session.roleCode,
  }
}

async function readErrorMessage(response: Response) {
  const contentType = response.headers.get('Content-Type') ?? ''
  if (contentType.includes('application/json')) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null
    return body?.message ?? '请求失败'
  }

  return response.text().catch(() => '请求失败')
}
