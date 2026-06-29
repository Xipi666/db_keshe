import type { RoleCode } from './auth'

export type MessageCategory = 'SAMPLE' | 'ALARM' | 'TASK'

export interface MeasurePointOptionResponse {
  id: number
  pointCode: string
  pointName: string
  pointGroup: string
  measureType: string
  phaseCode: string
  unit?: string
  minLimit?: number
  maxLimit?: number
  rateLimit?: number
}

export interface CircuitOptionResponse {
  circuitId: number
  circuitCode: string
  circuitName: string
  direction: 'INCOMING' | 'OUTGOING'
  ratedVoltageKv?: number
  ratedCurrentA?: number
  status: number
  points: MeasurePointOptionResponse[]
}

export interface TransformerOptionResponse {
  transformerId: number
  transformerCode: string
  transformerName: string
  transformerType: string
  ratedCapacityKva?: number
  ratedVoltageRatio?: string
  commissionDate?: string
  manufacturer?: string
  oilLevel?: number
  location?: string
  status: number
  circuits: CircuitOptionResponse[]
  points: MeasurePointOptionResponse[]
}

export interface MessageResponse {
  category: MessageCategory
  id: number
  transformerId: number
  transformerName: string
  circuitId?: number
  circuitName?: string
  pointId?: number
  pointName?: string
  pointCode?: string
  eventTime: string
  value?: number
  unit?: string
  qualityFlag?: number
  alarmType?: string
  alarmLevel?: string
  status?: number
  assignee?: string
  feedback?: string
}

export interface MessageQuery {
  category?: MessageCategory
  transformerId?: number
  circuitId?: number
  pointId?: number
  startTime?: string
  endTime?: string
  keyword?: string
}

export interface HistoryDataRow {
  id: number
  transformerId: number
  transformerName: string
  circuitId?: number
  circuitName?: string
  pointId?: number
  pointName?: string
  pointCode?: string
  unit?: string
  sampleTime: string
  rangeEndTime?: string
  value: number
  avgValue?: number
  minValue?: number
  maxValue?: number
  sampleCount?: number
  qualityFlag: number
  granularity?: string
  createdAt: string
}

export interface HistoryQuery {
  transformerId?: number
  circuitId?: number
  pointId?: number
  startTime?: string
  endTime?: string
}

export interface MaintenanceTaskResponse {
  taskId: number
  alarmId: number
  transformerId: number
  transformerName: string
  circuitId?: number
  circuitName?: string
  pointId?: number
  pointName?: string
  pointCode?: string
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
  transformerId?: number
  circuitId?: number
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
  normalIntervalMs: number
  anomalyIntervalMs: number
  currentIntervalMs: number
}

export type RuntimeLogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR'
export type RuntimeLogSource = 'DATABASE'

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
