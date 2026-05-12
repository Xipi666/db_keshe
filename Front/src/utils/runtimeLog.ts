import type { RuntimeLogLevel, RuntimeLogResponse } from '../types/dashboard'

const STORAGE_KEY = 'psm_frontend_logs'
const MAX_LOGS = 200

let nextId = Date.now()

export function appendFrontendLog(
  level: RuntimeLogLevel,
  message: string,
  context?: string,
): RuntimeLogResponse {
  const entry: RuntimeLogResponse = {
    id: nextId++,
    source: 'FRONTEND',
    level,
    message,
    context,
    createdAt: new Date().toISOString(),
  }

  const logs = readFrontendLogs()
  logs.unshift(entry)
  writeFrontendLogs(logs.slice(0, MAX_LOGS))
  return entry
}

export function readFrontendLogs(): RuntimeLogResponse[] {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw) as RuntimeLogResponse[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return []
  }
}

export function clearFrontendLogs() {
  localStorage.removeItem(STORAGE_KEY)
}

function writeFrontendLogs(logs: RuntimeLogResponse[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(logs))
}
