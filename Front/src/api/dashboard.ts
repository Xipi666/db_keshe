import { apiGet, apiPost } from './http'
import type { AuthSession } from '../types/auth'
import type {
  HistoryDataRow,
  HistoryQuery,
  MaintenanceTaskResponse,
  TransformerOptionResponse,
  MessageQuery,
  MessageResponse,
  RuntimeLogLevel,
  RuntimeLogResponse,
  SimulationStatusResponse,
  TaskQuery,
  TaskUpdatePayload,
} from '../types/dashboard'

export function fetchTransformers(session: AuthSession) {
  return apiGet<TransformerOptionResponse[]>('/api/metadata/transformers', session)
}

export function fetchMessages(session: AuthSession, query: MessageQuery) {
  return apiGet<MessageResponse[]>('/api/messages', session, query)
}

export function fetchHistory(session: AuthSession, query: HistoryQuery) {
  return apiGet<HistoryDataRow[]>('/api/history', session, query)
}

export function fetchSimulationStatus(session: AuthSession) {
  return apiGet<SimulationStatusResponse>('/api/simulation/status', session)
}

export function fetchRuntimeLogs(session: AuthSession, level: RuntimeLogLevel = 'INFO') {
  return apiGet<RuntimeLogResponse[]>('/api/runtime-logs', session, { level })
}

export function fetchTasks(session: AuthSession, query: TaskQuery) {
  return apiGet<MaintenanceTaskResponse[]>('/api/tasks', session, query)
}

export function updateTask(session: AuthSession, taskId: number, payload: TaskUpdatePayload) {
  return apiPost<MaintenanceTaskResponse>(`/api/tasks/${taskId}`, session, payload, 'PUT')
}

export function startSimulation(session: AuthSession) {
  return apiPost<SimulationStatusResponse>('/api/simulation/start', session)
}

export function stopSimulation(session: AuthSession) {
  return apiPost<SimulationStatusResponse>('/api/simulation/stop', session)
}

export function setSimulationAnomaly(session: AuthSession, enabled: boolean) {
  return apiPost<SimulationStatusResponse>('/api/simulation/anomaly', session, { enabled }, 'PUT')
}
