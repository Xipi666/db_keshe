import type { AuthResponse, LoginRequest, RegisterRequest } from '../types/auth'

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  return post<AuthResponse>('/api/auth/login', payload)
}

export async function register(payload: RegisterRequest): Promise<AuthResponse> {
  return post<AuthResponse>('/api/auth/register', payload)
}

async function post<TResponse>(url: string, payload: LoginRequest | RegisterRequest): Promise<TResponse> {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }

  return response.json() as Promise<TResponse>
}

async function readErrorMessage(response: Response) {
  try {
    const body = (await response.json()) as { message?: string }
    return body.message || `请求失败：${response.status}`
  } catch {
    return `请求失败：${response.status}`
  }
}
