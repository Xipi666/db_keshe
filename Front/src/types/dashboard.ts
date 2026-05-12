import type { RoleCode } from './auth'

export type MessageCategory = 'SAMPLE' | 'ALARM' | 'TASK'

export interface TagOptionResponse {
  id: number
  tagCode: string
  tagName: string
  unit?: string
  warnLimit?: number
  rateLimit?: number
}

export interface DeviceOptionResponse {
  stationId: number
  stationName: string
  bayId: number
  bayName: string
  deviceId: number
  deviceName: string
  deviceType: string
  status: number
  currentLimit?: number
  tempLimit?: number
  tempRateLimit?: number
  tags: TagOptionResponse[]
}

export interface MessageResponse {
  category: MessageCategory
  id: number
  deviceId: number
  deviceName: string
  tagId?: number
  tagName?: string
  tagCode?: string
  eventTime: string
  value?: number
  unit?: string
  freqFlag?: number
  qualityFlag?: number
  alarmType?: string
  alarmLevel?: string
  status?: number
  assignee?: string
  feedback?: string
}

export interface MessageQuery {
  category?: MessageCategory
  deviceId?: number
  tagId?: number
  startTime?: string
  endTime?: string
  keyword?: string
}

export interface HistoryDataRow {
  id: number
  deviceId: number
  deviceName: string
  tagId?: number
  tagName?: string
  tagCode?: string
  unit?: string
  sampleTime: string
  value: number
  freqFlag: number
  qualityFlag: number
  createdAt: string
}

export interface HistoryQuery {
  deviceId?: number
  tagId?: number
  startTime?: string
  endTime?: string
  freqFlag?: number
}

export interface MaintenanceTaskResponse {
  taskId: number
  alarmId: number
  deviceId: number
  deviceName: string
  tagId?: number
  tagName?: string
  tagCode?: string
  unit?: string
  alarmType: string
  alarmLevel: string
  alarmValue?: number
  alarmTime: string
  status: number
  assignee?: string
  feedback?: string
  createdAt: string
  updatedAt?: string
  finishedAt?: string
}

export interface TaskQuery {
  status?: number
  deviceId?: number
  startTime?: string
  endTime?: string
  keyword?: string
}

export interface TaskUpdatePayload {
  status: number
  assignee?: string
  feedback?: string
}

export interface SimulationStatusResponse {
  running: boolean
  anomalyEnabled: boolean
  startedAt?: string
  lastWriteAt?: string
  writeCount: number
  alarmCount: number
  taskCount: number
  normalIntervalSeconds: number
  burstIntervalSeconds: number
}

export type RuntimeLogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'
export type RuntimeLogSource = 'FRONTEND' | 'BACKEND'

export interface RuntimeLogResponse {
  id: number
  source: RuntimeLogSource
  level: RuntimeLogLevel
  message: string
  context?: string
  createdAt: string
}

export const categoryLabels: Record<MessageCategory, string> = {
  SAMPLE: '采样数据',
  ALARM: '告警记录',
  TASK: '维保工单',
}

export const runtimeLogLevelLabels: Record<RuntimeLogLevel, string> = {
  DEBUG: '调试',
  INFO: '信息',
  WARN: '警告',
  ERROR: '错误',
}

export const runtimeLogLevelOptions: RuntimeLogLevel[] = ['INFO', 'WARN', 'ERROR', 'DEBUG']

export function allowedCategories(roleCode: RoleCode): MessageCategory[] {
  if (roleCode === 'OPERATOR') {
    return ['SAMPLE', 'ALARM']
  }

  if (roleCode === 'ENGINEER') {
    return ['ALARM', 'TASK']
  }

  return ['SAMPLE', 'ALARM', 'TASK']
}
