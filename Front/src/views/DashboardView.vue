<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import {
  fetchHistory,
  fetchMessages,
  fetchRuntimeLogs,
  fetchSimulationStatus,
  fetchTasks,
  fetchTransformers,
  setSimulationAnomaly,
  startSimulation,
  stopSimulation,
  updateTask,
} from '../api/dashboard'
import { roleLabels } from '../types/auth'
import {
  allowedCategories,
  categoryLabels,
  runtimeLogLevelLabels,
  runtimeLogLevelOptions,
  type CircuitOptionResponse,
  type HistoryDataRow,
  type MaintenanceTaskResponse,
  type MeasurePointOptionResponse,
  type MessageCategory,
  type MessageResponse,
  type RuntimeLogLevel,
  type RuntimeLogResponse,
  type SimulationStatusResponse,
  type TransformerOptionResponse,
} from '../types/dashboard'
import type { AuthSession } from '../types/auth'
import { appendFrontendLog, clearFrontendLogs, readFrontendLogs } from '../utils/runtimeLog'

const props = defineProps<{
  session: AuthSession
}>()

const emit = defineEmits<{
  logout: []
}>()

type TabName = 'messages' | 'history' | 'tasks' | 'simulation' | 'logs'
type TransformerStatusFilter = 'all' | 'normal' | 'warning' | 'offline'
type HistoryGroupStatus = 'normal' | 'suspect' | 'invalid'

interface HistoryTimeGroup {
  sampleTime: string
  rows: HistoryDataRow[]
  transformerNames: string
  pointSummary: string
  highestQualityFlag: number
  status: HistoryGroupStatus
  valueSummary: string
}

const activeTab = ref<TabName>('messages')
const transformers = ref<TransformerOptionResponse[]>([])
const messages = ref<MessageResponse[]>([])
const historyRows = ref<HistoryDataRow[]>([])
const tasks = ref<MaintenanceTaskResponse[]>([])
const simulation = ref<SimulationStatusResponse | null>(null)
const backendLogs = ref<RuntimeLogResponse[]>([])
const frontendLogs = ref<RuntimeLogResponse[]>(readFrontendLogs())
const isLoadingMessages = ref(false)
const isLoadingHistory = ref(false)
const isLoadingTasks = ref(false)
const isLoadingMetadata = ref(false)
const isSimulationBusy = ref(false)
const isLoadingRuntimeLogs = ref(false)
const errorMessage = ref('')
const selectedTransformerStatus = ref<TransformerStatusFilter | null>(null)
const selectedHistoryGroup = ref<HistoryTimeGroup | null>(null)
const selectedTask = ref<MaintenanceTaskResponse | null>(null)
const runtimeLogLevel = ref<RuntimeLogLevel>('INFO')
const chartEl = ref<HTMLElement>()
let simulationPollTimer: number | undefined
let chart: echarts.ECharts | null = null

const messageForm = reactive({
  category: '' as '' | MessageCategory,
  transformerId: undefined as number | undefined,
  circuitId: undefined as number | undefined,
  pointId: undefined as number | undefined,
  startTime: '' as string,
  endTime: '' as string,
  keyword: '',
})

const historyForm = reactive({
  transformerId: undefined as number | undefined,
  circuitId: undefined as number | undefined,
  pointId: undefined as number | undefined,
  startTime: '' as string,
  endTime: '' as string,
})

const taskForm = reactive({
  status: '' as '' | 0 | 1 | 2,
  transformerId: undefined as number | undefined,
  circuitId: undefined as number | undefined,
  startTime: '' as string,
  endTime: '' as string,
  keyword: '',
})

const taskEditForm = reactive({
  status: 0,
  assignee: '',
  feedback: '',
})

const categoryOptions = computed(() => allowedCategories(props.session.roleCode))
const isAdmin = computed(() => props.session.roleCode === 'ADMIN')
const canQueryTasks = computed(() => ['ADMIN', 'ENGINEER', 'MANAGER'].includes(props.session.roleCode))
const canEditTasks = computed(() => ['ADMIN', 'ENGINEER'].includes(props.session.roleCode))

const activeTabTitle = computed(() => {
  const titles: Record<TabName, string> = {
    messages: '消息查询',
    history: '历史数据',
    tasks: '工单管理',
    simulation: '模拟测试',
    logs: '运行日志',
  }
  return titles[activeTab.value]
})

const messageCircuitOptions = computed(() => circuitsForTransformer(messageForm.transformerId))
const historyCircuitOptions = computed(() => circuitsForTransformer(historyForm.transformerId))
const taskCircuitOptions = computed(() => circuitsForTransformer(taskForm.transformerId))
const messagePointOptions = computed(() => pointsForScope(messageForm.transformerId, messageForm.circuitId))
const historyPointOptions = computed(() => pointsForScope(historyForm.transformerId, historyForm.circuitId))

const transformerStatusCounts = computed(() => {
  const normal = transformers.value.filter((transformer) => transformer.status === 0).length
  const warning = transformers.value.filter((transformer) => transformer.status === 1).length
  const offline = transformers.value.filter((transformer) => transformer.status === 2).length
  return { normal, warning, offline, total: transformers.value.length }
})

const selectedTransformerStatusLabel = computed(() => {
  if (selectedTransformerStatus.value === 'normal') {
    return '正常箱变'
  }

  if (selectedTransformerStatus.value === 'warning') {
    return '告警箱变'
  }

  if (selectedTransformerStatus.value === 'offline') {
    return '停运箱变'
  }

  return '全部箱变'
})

const transformerStatusDialogVisible = computed({
  get: () => selectedTransformerStatus.value !== null,
  set: (visible: boolean) => {
    if (!visible) {
      selectedTransformerStatus.value = null
    }
  },
})

const filteredStatusTransformers = computed(() => {
  if (selectedTransformerStatus.value === 'normal') {
    return transformers.value.filter((transformer) => transformer.status === 0)
  }

  if (selectedTransformerStatus.value === 'warning') {
    return transformers.value.filter((transformer) => transformer.status === 1)
  }

  if (selectedTransformerStatus.value === 'offline') {
    return transformers.value.filter((transformer) => transformer.status === 2)
  }

  return transformers.value
})

const visibleRuntimeLogs = computed(() => {
  const minWeight = runtimeLogWeight(runtimeLogLevel.value)
  return [...backendLogs.value, ...frontendLogs.value]
    .filter((row) => runtimeLogWeight(row.level) >= minWeight)
    .sort((left, right) => right.createdAt.localeCompare(left.createdAt))
    .slice(0, 300)
})

const groupedHistoryRows = computed<HistoryTimeGroup[]>(() => {
  const groups = new Map<string, HistoryDataRow[]>()
  historyRows.value.forEach((row) => {
    groups.set(row.sampleTime, [...(groups.get(row.sampleTime) ?? []), row])
  })

  return [...groups.entries()]
    .map(([sampleTime, rows]) => {
      const orderedRows = [...rows].sort((left, right) => (left.pointName ?? '').localeCompare(right.pointName ?? ''))
      const highestQualityFlag = Math.max(...orderedRows.map((row) => row.qualityFlag ?? 0))
      const transformerNames = [...new Set(orderedRows.map((row) => row.transformerName))].join('、')
      const pointNames = orderedRows.map((row) => row.pointName ?? row.pointCode ?? '未知测点')
      return {
        sampleTime,
        rows: orderedRows,
        transformerNames,
        pointSummary: summarizeText(pointNames, 3),
        highestQualityFlag,
        status: historyGroupStatus(highestQualityFlag),
        valueSummary: summarizeText(orderedRows.map((row) => `${row.pointName ?? row.pointCode}: ${row.value}${row.unit ? ` ${row.unit}` : ''}`), 2),
      }
    })
    .sort((left, right) => right.sampleTime.localeCompare(left.sampleTime))
})

const historyDetailDialogVisible = computed({
  get: () => selectedHistoryGroup.value !== null,
  set: (visible: boolean) => {
    if (!visible) {
      selectedHistoryGroup.value = null
    }
  },
})

const taskEditDialogVisible = computed({
  get: () => selectedTask.value !== null,
  set: (visible: boolean) => {
    if (!visible) {
      selectedTask.value = null
    }
  },
})

onMounted(async () => {
  appendFrontendLog('INFO', '进入工作台', `${props.session.displayName} / ${props.session.roleCode}`)
  frontendLogs.value = readFrontendLogs()
  resetHistoryRange()
  await loadMetadata()
  await Promise.all([queryMessages(), queryHistory(), queryTasks(), loadSimulationStatus(), loadRuntimeLogs()])
  startSimulationPolling()
})

onBeforeUnmount(() => {
  if (simulationPollTimer !== undefined) {
    window.clearInterval(simulationPollTimer)
  }
})

watch(
  () => messageForm.transformerId,
  () => {
    messageForm.circuitId = undefined
    messageForm.pointId = undefined
  },
)

watch(
  () => messageForm.circuitId,
  () => {
    messageForm.pointId = undefined
  },
)

watch(
  () => historyForm.transformerId,
  () => {
    historyForm.circuitId = undefined
    historyForm.pointId = undefined
  },
)

watch(
  () => historyForm.circuitId,
  () => {
    historyForm.pointId = undefined
  },
)

watch(
  () => taskForm.transformerId,
  () => {
    taskForm.circuitId = undefined
  },
)

watch(historyRows, () => {
  void nextTick(renderChart)
})

function circuitsForTransformer(transformerId?: number): CircuitOptionResponse[] {
  if (!transformerId) {
    return transformers.value.flatMap((transformer) => transformer.circuits)
  }

  return transformers.value.find((transformer) => transformer.transformerId === transformerId)?.circuits ?? []
}

function pointsForScope(transformerId?: number, circuitId?: number): MeasurePointOptionResponse[] {
  const selectedTransformers = transformerId
    ? transformers.value.filter((transformer) => transformer.transformerId === transformerId)
    : transformers.value

  return selectedTransformers.flatMap((transformer) => {
    if (circuitId) {
      return transformer.circuits.find((circuit) => circuit.circuitId === circuitId)?.points ?? []
    }

    return [
      ...transformer.points,
      ...transformer.circuits.flatMap((circuit) => circuit.points),
    ]
  })
}

async function loadMetadata() {
  isLoadingMetadata.value = true
  errorMessage.value = ''
  try {
    transformers.value = await fetchTransformers(props.session)
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isLoadingMetadata.value = false
  }
}

async function queryMessages() {
  isLoadingMessages.value = true
  errorMessage.value = ''
  try {
    messages.value = await fetchMessages(props.session, {
      category: messageForm.category || undefined,
      transformerId: messageForm.transformerId,
      circuitId: messageForm.circuitId,
      pointId: messageForm.pointId,
      startTime: toIsoValue(messageForm.startTime),
      endTime: toIsoValue(messageForm.endTime),
      keyword: messageForm.keyword.trim() || undefined,
    })
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isLoadingMessages.value = false
  }
}

async function queryHistory() {
  isLoadingHistory.value = true
  errorMessage.value = ''
  try {
    historyRows.value = await fetchHistory(props.session, {
      transformerId: historyForm.transformerId,
      circuitId: historyForm.circuitId,
      pointId: historyForm.pointId,
      startTime: toIsoValue(historyForm.startTime),
      endTime: toIsoValue(historyForm.endTime),
    })
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isLoadingHistory.value = false
  }
}

async function queryTasks() {
  if (!canQueryTasks.value) {
    return
  }

  isLoadingTasks.value = true
  errorMessage.value = ''
  try {
    tasks.value = await fetchTasks(props.session, {
      status: taskForm.status === '' ? undefined : taskForm.status,
      transformerId: taskForm.transformerId,
      circuitId: taskForm.circuitId,
      startTime: toIsoValue(taskForm.startTime),
      endTime: toIsoValue(taskForm.endTime),
      keyword: taskForm.keyword.trim() || undefined,
    })
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isLoadingTasks.value = false
  }
}

async function loadSimulationStatus() {
  if (!isAdmin.value) {
    return
  }

  try {
    simulation.value = await fetchSimulationStatus(props.session)
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  }
}

function startSimulationPolling() {
  if (!isAdmin.value || simulationPollTimer !== undefined) {
    return
  }

  simulationPollTimer = window.setInterval(() => {
    if (simulation.value?.running) {
      void loadSimulationStatus()
      void queryMessages()
      void queryHistory()
      if (canQueryTasks.value) {
        void queryTasks()
      }
    }
  }, 1000)
}

async function loadRuntimeLogs() {
  if (!isAdmin.value) {
    return
  }

  isLoadingRuntimeLogs.value = true
  try {
    backendLogs.value = await fetchRuntimeLogs(props.session, runtimeLogLevel.value)
    frontendLogs.value = readFrontendLogs()
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isLoadingRuntimeLogs.value = false
  }
}

async function handleStartSimulation() {
  isSimulationBusy.value = true
  try {
    simulation.value = await startSimulation(props.session)
    startSimulationPolling()
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isSimulationBusy.value = false
  }
}

async function handleStopSimulation() {
  isSimulationBusy.value = true
  try {
    simulation.value = await stopSimulation(props.session)
    await Promise.all([queryMessages(), queryHistory(), queryTasks()])
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isSimulationBusy.value = false
  }
}

function openHistoryGroup(group: HistoryTimeGroup) {
  selectedHistoryGroup.value = group
}

function openTaskEditor(task: MaintenanceTaskResponse) {
  selectedTask.value = task
  taskEditForm.status = task.status as 0 | 1 | 2
  taskEditForm.assignee = task.assignee || props.session.displayName
  taskEditForm.feedback = task.feedback || ''
}

async function submitTaskUpdate() {
  if (!selectedTask.value) {
    return
  }

  isLoadingTasks.value = true
  errorMessage.value = ''
  try {
    const updatedTask = await updateTask(props.session, selectedTask.value.taskId, {
      status: taskEditForm.status,
      assignee: taskEditForm.assignee.trim() || undefined,
      feedback: taskEditForm.feedback.trim() || undefined,
    })
    tasks.value = tasks.value.map((task) => (task.taskId === updatedTask.taskId ? updatedTask : task))
    selectedTask.value = null
    await queryMessages()
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isLoadingTasks.value = false
  }
}

async function handleAnomalyChange(value: string | number | boolean) {
  isSimulationBusy.value = true
  try {
    simulation.value = await setSimulationAnomaly(props.session, Boolean(value))
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isSimulationBusy.value = false
  }
}

function renderChart() {
  if (!chartEl.value) {
    return
  }

  chart ??= echarts.init(chartEl.value)
  const orderedRows = [...historyRows.value].reverse()
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 20, top: 24, bottom: 40 },
    xAxis: {
      type: 'category',
      data: orderedRows.map((row) => formatTime(row.sampleTime)),
      axisLabel: { color: '#64748b' },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
    },
    series: [
      {
        name: '采样值',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: orderedRows.map((row) => row.value),
        lineStyle: { color: '#2563eb', width: 2 },
        itemStyle: { color: '#2563eb' },
        areaStyle: { color: 'rgba(37, 99, 235, 0.10)' },
      },
    ],
  })
}

function resetHistoryRange() {
  historyForm.startTime = toLocalInputValue(new Date(Date.now() - 60 * 60 * 1000))
  historyForm.endTime = toLocalInputValue(new Date())
}

function openTransformerStatusList(status: TransformerStatusFilter) {
  selectedTransformerStatus.value = status
}

function statusLabel(status: number) {
  if (status === 0) {
    return '正常'
  }

  if (status === 1) {
    return '告警'
  }

  if (status === 2) {
    return '停运'
  }

  return '未知'
}

function statusTagType(status: number) {
  if (status === 0) {
    return 'success'
  }

  if (status === 1) {
    return 'danger'
  }

  if (status === 2) {
    return 'warning'
  }

  return 'info'
}

function taskStatusLabel(status?: number) {
  if (status === 0) {
    return '待办'
  }

  if (status === 1) {
    return '处理中'
  }

  if (status === 2) {
    return '已完成'
  }

  return '未知'
}

function taskStatusType(status?: number) {
  if (status === 2) {
    return 'success'
  }

  if (status === 1) {
    return 'warning'
  }

  return 'info'
}

function historyGroupStatus(qualityFlag: number): HistoryGroupStatus {
  if (qualityFlag >= 2) {
    return 'invalid'
  }

  if (qualityFlag === 1) {
    return 'suspect'
  }

  return 'normal'
}

function historyGroupStatusLabel(status: HistoryGroupStatus) {
  if (status === 'invalid') {
    return '无效'
  }

  if (status === 'suspect') {
    return '可疑'
  }

  return '正常'
}

function historyGroupStatusType(status: HistoryGroupStatus) {
  if (status === 'invalid') {
    return 'danger'
  }

  if (status === 'suspect') {
    return 'warning'
  }

  return 'success'
}

function runtimeLogTagType(level: RuntimeLogLevel) {
  if (level === 'ERROR') {
    return 'danger'
  }

  if (level === 'WARN') {
    return 'warning'
  }

  if (level === 'DEBUG') {
    return 'info'
  }

  return 'success'
}

function runtimeLogWeight(level: RuntimeLogLevel) {
  const weights: Record<RuntimeLogLevel, number> = {
    DEBUG: 10,
    INFO: 20,
    WARN: 30,
    ERROR: 40,
  }
  return weights[level]
}

function clearFrontendRuntimeLogs() {
  clearFrontendLogs()
  appendFrontendLog('INFO', '前端日志已清空')
  frontendLogs.value = readFrontendLogs()
}

function messageStatusLabel(row: MessageResponse) {
  if (row.category === 'ALARM') {
    return row.status === 0 ? '活跃' : '已关闭'
  }

  if (row.category === 'TASK') {
    return taskStatusLabel(row.status)
  }

  return row.qualityFlag === 0 ? '正常' : row.qualityFlag === 1 ? '可疑' : '无效'
}

function messageStatusType(row: MessageResponse) {
  if (row.category === 'ALARM') {
    return row.status === 0 ? 'danger' : 'success'
  }

  if (row.category === 'TASK') {
    return row.status === 2 ? 'success' : 'warning'
  }

  return row.qualityFlag === 0 ? 'success' : 'warning'
}

function summarizeText(values: string[], limit: number) {
  const visibleValues = values.filter(Boolean)
  if (visibleValues.length <= limit) {
    return visibleValues.join('、')
  }

  return `${visibleValues.slice(0, limit).join('、')} 等 ${visibleValues.length} 项`
}

function formatTime(value?: string) {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 19)
}

function toLocalInputValue(date: Date) {
  const offset = date.getTimezoneOffset()
  const local = new Date(date.getTime() - offset * 60 * 1000)
  return local.toISOString().slice(0, 16)
}

function toIsoValue(value: string) {
  if (!value) {
    return undefined
  }

  return value.length === 16 ? `${value}:00` : value
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败'
}

function handleMenuSelect(key: string) {
  activeTab.value = key as TabName
  appendFrontendLog('INFO', `切换到 ${activeTabTitle.value}`)
  frontendLogs.value = readFrontendLogs()

  if (activeTab.value === 'logs') {
    void loadRuntimeLogs()
  }

  if (activeTab.value === 'tasks') {
    void queryTasks()
  }
}
</script>

<template>
  <div class="dashboard-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">PSM</span>
        <div>
          <strong>箱变监测</strong>
          <small>Smart Operation Console</small>
        </div>
      </div>

      <el-menu :default-active="activeTab" class="nav-menu" @select="handleMenuSelect">
        <el-menu-item index="messages">消息查询</el-menu-item>
        <el-menu-item index="history">历史数据</el-menu-item>
        <el-menu-item v-if="canQueryTasks" index="tasks">工单管理</el-menu-item>
        <el-menu-item v-if="isAdmin" index="simulation">模拟测试</el-menu-item>
        <el-menu-item v-if="isAdmin" index="logs">运行日志</el-menu-item>
      </el-menu>
    </aside>

    <main class="content">
      <header class="topbar">
        <div>
          <h1>{{ activeTabTitle }}</h1>
          <p>{{ roleLabels[props.session.roleCode] }} / {{ props.session.displayName }}</p>
        </div>
        <el-button @click="emit('logout')">退出登录</el-button>
      </header>

      <el-alert v-if="errorMessage" class="error-alert" type="error" :title="errorMessage" show-icon closable @close="errorMessage = ''" />

      <section class="overview-grid" v-loading="isLoadingMetadata">
        <button class="metric metric-button" type="button" @click="openTransformerStatusList('all')">
          <span>箱变总数</span>
          <strong>{{ transformerStatusCounts.total }}</strong>
        </button>
        <button class="metric metric-button" type="button" @click="openTransformerStatusList('normal')">
          <span>正常箱变</span>
          <strong>{{ transformerStatusCounts.normal }}</strong>
        </button>
        <button class="metric metric-button" type="button" @click="openTransformerStatusList('warning')">
          <span>告警箱变</span>
          <strong :class="{ 'metric-danger': transformerStatusCounts.warning > 0 }">{{ transformerStatusCounts.warning }}</strong>
        </button>
        <button class="metric metric-button" type="button" @click="openTransformerStatusList('offline')">
          <span>停运箱变</span>
          <strong :class="{ 'metric-warning': transformerStatusCounts.offline > 0 }">{{ transformerStatusCounts.offline }}</strong>
        </button>
      </section>

      <section v-if="activeTab === 'messages'" class="panel">
        <el-form class="query-form" :model="messageForm" label-position="top">
          <el-form-item label="消息类型">
            <el-select v-model="messageForm.category" clearable placeholder="全部类型">
              <el-option v-for="category in categoryOptions" :key="category" :label="categoryLabels[category]" :value="category" />
            </el-select>
          </el-form-item>
          <el-form-item label="箱变">
            <el-select v-model="messageForm.transformerId" clearable filterable placeholder="全部箱变">
              <el-option
                v-for="transformer in transformers"
                :key="transformer.transformerId"
                :label="`${transformer.transformerCode} / ${transformer.transformerName}`"
                :value="transformer.transformerId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="回路">
            <el-select v-model="messageForm.circuitId" clearable filterable placeholder="全部回路">
              <el-option
                v-for="circuit in messageCircuitOptions"
                :key="circuit.circuitId"
                :label="`${circuit.circuitName} (${circuit.direction})`"
                :value="circuit.circuitId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="测点">
            <el-select v-model="messageForm.pointId" clearable filterable placeholder="全部测点">
              <el-option
                v-for="point in messagePointOptions"
                :key="point.id"
                :label="`${point.pointName} (${point.pointCode})`"
                :value="point.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-input v-model="messageForm.startTime" type="datetime-local" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-input v-model="messageForm.endTime" type="datetime-local" />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="messageForm.keyword" clearable placeholder="箱变/回路/测点/告警" />
          </el-form-item>
          <el-form-item class="query-actions">
            <el-button type="primary" :loading="isLoadingMessages" @click="queryMessages">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="messages" border stripe height="520" v-loading="isLoadingMessages">
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag>{{ categoryLabels[row.category as MessageCategory] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="eventTime" label="时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.eventTime) }}</template>
          </el-table-column>
          <el-table-column prop="transformerName" label="箱变" min-width="160" />
          <el-table-column prop="circuitName" label="回路" min-width="140" />
          <el-table-column prop="pointName" label="测点" min-width="150" />
          <el-table-column label="数值" min-width="120">
            <template #default="{ row }">{{ row.value ?? '-' }}{{ row.unit ? ` ${row.unit}` : '' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="messageStatusType(row)">{{ messageStatusLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" min-width="140">
            <template #default="{ row }">{{ row.alarmType || row.assignee || '-' }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeTab === 'history'" class="panel">
        <el-form class="query-form history-form" :model="historyForm" label-position="top">
          <el-form-item label="箱变">
            <el-select v-model="historyForm.transformerId" clearable filterable placeholder="全部箱变">
              <el-option
                v-for="transformer in transformers"
                :key="transformer.transformerId"
                :label="`${transformer.transformerCode} / ${transformer.transformerName}`"
                :value="transformer.transformerId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="回路">
            <el-select v-model="historyForm.circuitId" clearable filterable placeholder="全部回路">
              <el-option
                v-for="circuit in historyCircuitOptions"
                :key="circuit.circuitId"
                :label="`${circuit.circuitName} (${circuit.direction})`"
                :value="circuit.circuitId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="测点">
            <el-select v-model="historyForm.pointId" clearable filterable placeholder="全部测点">
              <el-option
                v-for="point in historyPointOptions"
                :key="point.id"
                :label="`${point.pointName} (${point.pointCode})`"
                :value="point.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-input v-model="historyForm.startTime" type="datetime-local" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-input v-model="historyForm.endTime" type="datetime-local" />
          </el-form-item>
          <el-form-item class="query-actions">
            <el-button @click="resetHistoryRange">最近一小时</el-button>
            <el-button type="primary" :loading="isLoadingHistory" @click="queryHistory">查询</el-button>
          </el-form-item>
        </el-form>

        <div ref="chartEl" class="history-chart"></div>

        <el-table :data="groupedHistoryRows" border stripe height="340" v-loading="isLoadingHistory" @row-click="openHistoryGroup">
          <el-table-column prop="sampleTime" label="采样时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.sampleTime) }}</template>
          </el-table-column>
          <el-table-column prop="transformerNames" label="箱变" min-width="160" show-overflow-tooltip />
          <el-table-column prop="pointSummary" label="测点汇总" min-width="190" show-overflow-tooltip />
          <el-table-column prop="valueSummary" label="采样值" min-width="220" show-overflow-tooltip />
          <el-table-column label="质量" width="110">
            <template #default="{ row }">
              <el-tag :type="historyGroupStatusType(row.status)">{{ historyGroupStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeTab === 'tasks'" class="panel">
        <el-form class="query-form" :model="taskForm" label-position="top">
          <el-form-item label="工单状态">
            <el-select v-model="taskForm.status" clearable placeholder="全部状态">
              <el-option label="待办" :value="0" />
              <el-option label="处理中" :value="1" />
              <el-option label="已完成" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="箱变">
            <el-select v-model="taskForm.transformerId" clearable filterable placeholder="全部箱变">
              <el-option
                v-for="transformer in transformers"
                :key="transformer.transformerId"
                :label="`${transformer.transformerCode} / ${transformer.transformerName}`"
                :value="transformer.transformerId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="回路">
            <el-select v-model="taskForm.circuitId" clearable filterable placeholder="全部回路">
              <el-option
                v-for="circuit in taskCircuitOptions"
                :key="circuit.circuitId"
                :label="`${circuit.circuitName} (${circuit.direction})`"
                :value="circuit.circuitId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-input v-model="taskForm.startTime" type="datetime-local" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-input v-model="taskForm.endTime" type="datetime-local" />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="taskForm.keyword" clearable placeholder="告警/负责人/反馈" />
          </el-form-item>
          <el-form-item class="query-actions">
            <el-button type="primary" :loading="isLoadingTasks" @click="queryTasks">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="tasks" border stripe height="520" v-loading="isLoadingTasks">
          <el-table-column prop="taskId" label="工单号" width="100" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="taskStatusType(row.status)">{{ taskStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="transformerName" label="箱变" min-width="150" />
          <el-table-column prop="circuitName" label="回路" min-width="130" />
          <el-table-column prop="pointName" label="测点" min-width="140" />
          <el-table-column label="告警值" min-width="120">
            <template #default="{ row }">{{ row.alarmValue ?? '-' }}{{ row.unit ? ` ${row.unit}` : '' }}</template>
          </el-table-column>
          <el-table-column prop="alarmType" label="告警类型" min-width="140" />
          <el-table-column prop="alarmTime" label="告警时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.alarmTime) }}</template>
          </el-table-column>
          <el-table-column prop="assignee" label="负责人" min-width="120" />
          <el-table-column v-if="canEditTasks" label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openTaskEditor(row)">处理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeTab === 'simulation'" class="panel simulation-panel">
        <div class="simulation-header">
          <div>
            <h2>秒级模拟采集</h2>
            <p>固定每 {{ simulation?.sampleIntervalSeconds ?? 1 }} 秒写入一次采样。异常开关打开后，模拟值越限并生成告警和工单。</p>
          </div>
          <el-tag :type="simulation?.running ? 'success' : 'info'">
            {{ simulation?.running ? '运行中' : '已停止' }}
          </el-tag>
        </div>

        <div class="simulation-actions">
          <el-button type="primary" :disabled="simulation?.running" :loading="isSimulationBusy" @click="handleStartSimulation">
            启动模拟
          </el-button>
          <el-button type="danger" :disabled="!simulation?.running" :loading="isSimulationBusy" @click="handleStopSimulation">
            停止模拟
          </el-button>
          <el-switch
            :model-value="simulation?.anomalyEnabled ?? false"
            active-text="异常"
            inactive-text="正常"
            :disabled="!simulation?.running || isSimulationBusy"
            @change="handleAnomalyChange"
          />
        </div>

        <div class="overview-grid simulation-metrics">
          <div class="metric">
            <span>写入采样</span>
            <strong>{{ simulation?.writeCount ?? 0 }}</strong>
          </div>
          <div class="metric">
            <span>告警数</span>
            <strong>{{ simulation?.alarmCount ?? 0 }}</strong>
          </div>
          <div class="metric">
            <span>工单数</span>
            <strong>{{ simulation?.taskCount ?? 0 }}</strong>
          </div>
          <div class="metric">
            <span>最近写入</span>
            <strong class="metric-time">{{ formatTime(simulation?.lastWriteAt) }}</strong>
          </div>
        </div>
      </section>

      <section v-else-if="activeTab === 'logs'" class="panel logs-panel">
        <div class="logs-toolbar">
          <el-select v-model="runtimeLogLevel" class="log-level-select" @change="loadRuntimeLogs">
            <el-option
              v-for="level in runtimeLogLevelOptions"
              :key="level"
              :label="runtimeLogLevelLabels[level]"
              :value="level"
            />
          </el-select>
          <el-button :loading="isLoadingRuntimeLogs" @click="loadRuntimeLogs">刷新</el-button>
          <el-button @click="clearFrontendRuntimeLogs">清空前端日志</el-button>
        </div>

        <el-table :data="visibleRuntimeLogs" border stripe height="560" v-loading="isLoadingRuntimeLogs">
          <el-table-column prop="createdAt" label="时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="来源" width="120">
            <template #default="{ row }">
              <el-tag :type="row.source === 'BACKEND' ? 'primary' : 'success'">
                {{ row.source }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="级别" width="100">
            <template #default="{ row }">
              <el-tag :type="runtimeLogTagType(row.level)">{{ runtimeLogLevelLabels[row.level as RuntimeLogLevel] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="消息" min-width="220" show-overflow-tooltip />
          <el-table-column prop="context" label="上下文" min-width="260" show-overflow-tooltip />
        </el-table>
      </section>
    </main>

    <el-dialog v-model="transformerStatusDialogVisible" :title="selectedTransformerStatusLabel" width="820px">
      <el-table :data="filteredStatusTransformers" border stripe max-height="480">
        <el-table-column prop="transformerCode" label="编码" min-width="130" />
        <el-table-column prop="transformerName" label="箱变" min-width="160" />
        <el-table-column prop="ratedCapacityKva" label="额定容量(kVA)" min-width="130" />
        <el-table-column prop="ratedVoltageRatio" label="电压比" min-width="110" />
        <el-table-column prop="location" label="位置" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="测点数" width="100">
          <template #default="{ row }">{{ row.points.length + row.circuits.reduce((total: number, circuit: CircuitOptionResponse) => total + circuit.points.length, 0) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="historyDetailDialogVisible" :title="`采样明细 ${formatTime(selectedHistoryGroup?.sampleTime)}`" width="900px">
      <el-table :data="selectedHistoryGroup?.rows ?? []" border stripe max-height="460">
        <el-table-column prop="transformerName" label="箱变" min-width="150" />
        <el-table-column prop="circuitName" label="回路" min-width="130" />
        <el-table-column prop="pointName" label="测点" min-width="150" />
        <el-table-column prop="pointCode" label="编码" min-width="170" />
        <el-table-column label="数值" min-width="120">
          <template #default="{ row }">{{ row.value }}{{ row.unit ? ` ${row.unit}` : '' }}</template>
        </el-table-column>
        <el-table-column label="质量" width="100">
          <template #default="{ row }">
            <el-tag :type="historyGroupStatusType(historyGroupStatus(row.qualityFlag))">
              {{ historyGroupStatusLabel(historyGroupStatus(row.qualityFlag)) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="taskEditDialogVisible" title="处理工单" width="560px">
      <el-form :model="taskEditForm" label-position="top">
        <el-form-item label="状态">
          <el-select v-model="taskEditForm.status">
            <el-option label="待办" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="taskEditForm.assignee" />
        </el-form-item>
        <el-form-item label="处理反馈">
          <el-input v-model="taskEditForm.feedback" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="selectedTask = null">取消</el-button>
        <el-button type="primary" :loading="isLoadingTasks" @click="submitTaskUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dashboard-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px 1fr;
  background: #f5f7fb;
  color: #0f172a;
}

.sidebar {
  background: #14213d;
  color: #f8fafc;
  padding: 24px 18px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.brand-mark {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #38bdf8;
  color: #082f49;
  font-weight: 800;
}

.brand strong,
.brand small {
  display: block;
}

.brand small {
  color: #bfd7ea;
  font-size: 12px;
  margin-top: 2px;
}

.nav-menu {
  border-right: 0;
  background: transparent;
}

.nav-menu :deep(.el-menu-item) {
  color: #dbeafe;
  border-radius: 8px;
}

.nav-menu :deep(.el-menu-item.is-active),
.nav-menu :deep(.el-menu-item:hover) {
  background: rgba(56, 189, 248, 0.18);
  color: #ffffff;
}

.content {
  padding: 24px;
  min-width: 0;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.topbar h1 {
  margin: 0;
  font-size: 24px;
}

.topbar p {
  margin: 4px 0 0;
  color: #64748b;
}

.error-alert {
  margin-bottom: 16px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric {
  min-height: 88px;
  padding: 16px;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  background: #ffffff;
  text-align: left;
}

.metric span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.metric strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
}

.metric-button {
  cursor: pointer;
}

.metric-button:hover {
  border-color: #2563eb;
}

.metric-danger {
  color: #dc2626;
}

.metric-warning {
  color: #d97706;
}

.metric-time {
  font-size: 18px !important;
}

.panel {
  background: #ffffff;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  padding: 18px;
}

.query-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 12px;
  align-items: end;
  margin-bottom: 16px;
}

.history-form {
  grid-template-columns: repeat(3, minmax(180px, 1fr));
}

.query-actions {
  align-self: end;
}

.history-chart {
  height: 260px;
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.simulation-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.simulation-header h2 {
  margin: 0;
  font-size: 20px;
}

.simulation-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.simulation-actions,
.logs-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.simulation-metrics {
  margin-top: 16px;
}

.log-level-select {
  width: 160px;
}

@media (max-width: 1100px) {
  .dashboard-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
  }

  .overview-grid,
  .query-form,
  .history-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .content {
    padding: 14px;
  }

  .overview-grid,
  .query-form,
  .history-form {
    grid-template-columns: 1fr;
  }

  .topbar {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
}
</style>
