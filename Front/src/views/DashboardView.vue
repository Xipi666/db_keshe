<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import {
  fetchDevices,
  fetchHistory,
  fetchMessages,
  fetchRuntimeLogs,
  fetchSimulationStatus,
  fetchTasks,
  setSimulationAnomaly,
  startSimulation,
  stopSimulation,
  updateTask,
} from '../api/dashboard'
import { roleLabels } from '../types/auth'
import {
  allowedCategories,
  categoryLabels,
  type DeviceOptionResponse,
  type HistoryDataRow,
  type MaintenanceTaskResponse,
  type MessageCategory,
  type MessageResponse,
  type RuntimeLogLevel,
  type RuntimeLogResponse,
  type SimulationStatusResponse,
  type TagOptionResponse,
  runtimeLogLevelLabels,
  runtimeLogLevelOptions,
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
type DeviceStatusFilter = 'all' | 'normal' | 'warning' | 'offline'
type HistoryGroupStatus = 'normal' | 'suspect' | 'invalid'

interface HistoryTimeGroup {
  sampleTime: string
  rows: HistoryDataRow[]
  deviceNames: string
  tagSummary: string
  highestQualityFlag: number
  status: HistoryGroupStatus
  freqFlags: number[]
  valueSummary: string
}

const activeTab = ref<TabName>('messages')
const devices = ref<DeviceOptionResponse[]>([])
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
const selectedDeviceStatus = ref<DeviceStatusFilter | null>(null)
const selectedHistoryGroup = ref<HistoryTimeGroup | null>(null)
const selectedTask = ref<MaintenanceTaskResponse | null>(null)
const runtimeLogLevel = ref<RuntimeLogLevel>('INFO')
const chartEl = ref<HTMLElement>()
let simulationPollTimer: number | undefined
let chart: echarts.ECharts | null = null

const messageForm = reactive({
  category: '' as '' | MessageCategory,
  deviceId: undefined as number | undefined,
  tagId: undefined as number | undefined,
  startTime: '' as string,
  endTime: '' as string,
  keyword: '',
})

const historyForm = reactive({
  deviceId: undefined as number | undefined,
  tagId: undefined as number | undefined,
  startTime: '' as string,
  endTime: '' as string,
  freqFlag: '' as '' | 0 | 1,
})

const taskForm = reactive({
  status: '' as '' | 0 | 1 | 2,
  deviceId: undefined as number | undefined,
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

const messageTagOptions = computed(() => tagsForDevice(messageForm.deviceId))
const historyTagOptions = computed(() => tagsForDevice(historyForm.deviceId))

const canQueryTasks = computed(() => ['ADMIN', 'ENGINEER', 'MANAGER'].includes(props.session.roleCode))
const canEditTasks = computed(() => ['ADMIN', 'ENGINEER'].includes(props.session.roleCode))

const deviceStatusCounts = computed(() => {
  const normal = devices.value.filter((device) => device.status === 0).length
  const warning = devices.value.filter((device) => device.status === 1).length
  const offline = devices.value.filter((device) => device.status === 2).length
  return { normal, warning, offline, total: devices.value.length }
})

const selectedDeviceStatusLabel = computed(() => {
  if (selectedDeviceStatus.value === 'normal') {
    return '正常设备'
  }

  if (selectedDeviceStatus.value === 'warning') {
    return '告警设备'
  }

  if (selectedDeviceStatus.value === 'offline') {
    return '停运设备'
  }

  return '全部设备'
})

const deviceStatusDialogVisible = computed({
  get: () => selectedDeviceStatus.value !== null,
  set: (visible: boolean) => {
    if (!visible) {
      selectedDeviceStatus.value = null
    }
  },
})

const filteredStatusDevices = computed(() => {
  if (selectedDeviceStatus.value === 'normal') {
    return devices.value.filter((device) => device.status === 0)
  }

  if (selectedDeviceStatus.value === 'warning') {
    return devices.value.filter((device) => device.status === 1)
  }

  if (selectedDeviceStatus.value === 'offline') {
    return devices.value.filter((device) => device.status === 2)
  }

  return devices.value
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
    const key = row.sampleTime
    groups.set(key, [...(groups.get(key) ?? []), row])
  })

  return [...groups.entries()]
    .map(([sampleTime, rows]) => {
      const orderedRows = [...rows].sort((left, right) => (left.tagName ?? '').localeCompare(right.tagName ?? ''))
      const highestQualityFlag = Math.max(...orderedRows.map((row) => row.qualityFlag ?? 0))
      const deviceNames = [...new Set(orderedRows.map((row) => row.deviceName))].join('、')
      const tagNames = orderedRows.map((row) => row.tagName ?? row.tagCode ?? '未知测点')
      const freqFlags = [...new Set(orderedRows.map((row) => row.freqFlag))]
      return {
        sampleTime,
        rows: orderedRows,
        deviceNames,
        tagSummary: summarizeText(tagNames, 3),
        highestQualityFlag,
        status: historyGroupStatus(highestQualityFlag),
        freqFlags,
        valueSummary: summarizeText(orderedRows.map((row) => `${row.tagName ?? row.tagCode}: ${row.value}${row.unit ? ` ${row.unit}` : ''}`), 2),
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
  () => messageForm.deviceId,
  () => {
    messageForm.tagId = undefined
  },
)

watch(
  () => historyForm.deviceId,
  () => {
    historyForm.tagId = undefined
  },
)

watch(historyRows, () => {
  void nextTick(renderChart)
})

function tagsForDevice(deviceId?: number): TagOptionResponse[] {
  if (!deviceId) {
    return devices.value.flatMap((device) => device.tags)
  }

  return devices.value.find((device) => device.deviceId === deviceId)?.tags ?? []
}

async function loadMetadata() {
  isLoadingMetadata.value = true
  errorMessage.value = ''
  try {
    devices.value = await fetchDevices(props.session)
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
      deviceId: messageForm.deviceId,
      tagId: messageForm.tagId,
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
      deviceId: historyForm.deviceId,
      tagId: historyForm.tagId,
      startTime: toIsoValue(historyForm.startTime),
      endTime: toIsoValue(historyForm.endTime),
      freqFlag: historyForm.freqFlag === '' ? undefined : historyForm.freqFlag,
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
      deviceId: taskForm.deviceId,
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

function openDeviceStatusList(status: DeviceStatusFilter) {
  selectedDeviceStatus.value = status
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
    return row.status === 0 ? '待办' : row.status === 1 ? '处理中' : '已完成'
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

function freqLabel(freqFlag?: number) {
  if (freqFlag === 1) {
    return '秒级'
  }

  if (freqFlag === 0) {
    return '分钟级'
  }

  return '-'
}

function freqSummary(freqFlags: number[]) {
  return freqFlags
    .sort()
    .map((freqFlag) => freqLabel(freqFlag))
    .join('、')
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
  appendFrontendLog('INFO', `切换到${activeTabTitle.value}`)
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
          <strong>变电站监测</strong>
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

    <main class="workspace">
      <header class="topbar">
        <div>
          <h1>{{ activeTabTitle }}</h1>
          <p>{{ props.session.displayName }} · {{ roleLabels[props.session.roleCode] }}</p>
        </div>

        <el-button @click="emit('logout')">退出登录</el-button>
      </header>

      <el-alert v-if="errorMessage" class="error-alert" type="error" :title="errorMessage" show-icon />

      <section class="overview-grid" v-loading="isLoadingMetadata">
        <button class="metric metric-button" type="button" @click="openDeviceStatusList('all')">
          <span>设备总数</span>
          <strong>{{ deviceStatusCounts.total }}</strong>
        </button>
        <button class="metric metric-button" type="button" @click="openDeviceStatusList('normal')">
          <span>正常设备</span>
          <strong>{{ deviceStatusCounts.normal }}</strong>
        </button>
        <button class="metric metric-button" type="button" @click="openDeviceStatusList('warning')">
          <span>告警设备</span>
          <strong :class="{ 'metric-danger': deviceStatusCounts.warning > 0 }">{{ deviceStatusCounts.warning }}</strong>
        </button>
        <button class="metric metric-button" type="button" @click="openDeviceStatusList('offline')">
          <span>停运设备</span>
          <strong :class="{ 'metric-warning': deviceStatusCounts.offline > 0 }">{{ deviceStatusCounts.offline }}</strong>
        </button>
      </section>

      <section v-if="activeTab === 'messages'" class="panel">
        <el-form class="query-form" :model="messageForm" label-position="top">
          <el-form-item label="消息类型">
            <el-select v-model="messageForm.category" clearable placeholder="全部可查询类型">
              <el-option
                v-for="category in categoryOptions"
                :key="category"
                :label="categoryLabels[category]"
                :value="category"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="设备">
            <el-select v-model="messageForm.deviceId" clearable filterable placeholder="全部设备">
              <el-option
                v-for="device in devices"
                :key="device.deviceId"
                :label="`${device.stationName} / ${device.deviceName}`"
                :value="device.deviceId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="测点">
            <el-select v-model="messageForm.tagId" clearable filterable placeholder="全部测点">
              <el-option
                v-for="tag in messageTagOptions"
                :key="tag.id"
                :label="`${tag.tagName} (${tag.tagCode})`"
                :value="tag.id"
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
            <el-input v-model="messageForm.keyword" clearable placeholder="设备、测点、告警类型" />
          </el-form-item>

          <el-form-item class="form-actions">
            <el-button type="primary" :loading="isLoadingMessages" @click="queryMessages">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="messages" border stripe height="520" v-loading="isLoadingMessages">
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag>{{ categoryLabels[row.category as MessageCategory] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="eventTime" label="时间" min-width="160">
            <template #default="{ row }">{{ formatTime(row.eventTime) }}</template>
          </el-table-column>
          <el-table-column prop="deviceName" label="设备" min-width="150" />
          <el-table-column prop="tagName" label="测点" min-width="150" />
          <el-table-column label="数值" width="120">
            <template #default="{ row }">
              {{ row.value ?? '-' }}{{ row.unit ? ` ${row.unit}` : '' }}
            </template>
          </el-table-column>
          <el-table-column label="告警/工单" min-width="150">
            <template #default="{ row }">
              {{ row.alarmType || row.assignee || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="messageStatusType(row)">{{ messageStatusLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="feedback" label="反馈" min-width="180" show-overflow-tooltip />
        </el-table>
      </section>

      <section v-else-if="activeTab === 'history'" class="panel">
        <el-form class="query-form history-form" :model="historyForm" label-position="top">
          <el-form-item label="设备">
            <el-select v-model="historyForm.deviceId" clearable filterable placeholder="全部设备">
              <el-option
                v-for="device in devices"
                :key="device.deviceId"
                :label="`${device.stationName} / ${device.deviceName}`"
                :value="device.deviceId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="测点">
            <el-select v-model="historyForm.tagId" clearable filterable placeholder="全部测点">
              <el-option
                v-for="tag in historyTagOptions"
                :key="tag.id"
                :label="`${tag.tagName} (${tag.tagCode})`"
                :value="tag.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="采样频率">
            <el-select v-model="historyForm.freqFlag" clearable placeholder="全部">
              <el-option label="分钟级" :value="0" />
              <el-option label="秒级" :value="1" />
            </el-select>
          </el-form-item>

          <el-form-item label="开始时间">
            <el-input v-model="historyForm.startTime" type="datetime-local" />
          </el-form-item>

          <el-form-item label="结束时间">
            <el-input v-model="historyForm.endTime" type="datetime-local" />
          </el-form-item>

          <el-form-item class="form-actions">
            <el-button @click="resetHistoryRange">最近一小时</el-button>
            <el-button type="primary" :loading="isLoadingHistory" @click="queryHistory">查询</el-button>
          </el-form-item>
        </el-form>

        <div ref="chartEl" class="history-chart"></div>

        <el-table :data="groupedHistoryRows" border stripe height="420" v-loading="isLoadingHistory" @row-click="openHistoryGroup">
          <el-table-column prop="sampleTime" label="采样时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.sampleTime) }}</template>
          </el-table-column>
          <el-table-column prop="deviceNames" label="设备" min-width="160" show-overflow-tooltip />
          <el-table-column prop="tagSummary" label="测点汇总" min-width="190" show-overflow-tooltip />
          <el-table-column prop="valueSummary" label="数值摘要" min-width="220" show-overflow-tooltip />
          <el-table-column label="频率" width="120">
            <template #default="{ row }">
              {{ freqSummary(row.freqFlags) }}
            </template>
          </el-table-column>
          <el-table-column label="整体状态" width="110">
            <template #default="{ row }">
              <el-tag :type="historyGroupStatusType(row.status)">{{ historyGroupStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="明细数" width="90">
            <template #default="{ row }">{{ row.rows.length }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeTab === 'tasks'" class="panel">
        <el-form class="query-form task-form" :model="taskForm" label-position="top">
          <el-form-item label="工单状态">
            <el-select v-model="taskForm.status" clearable placeholder="全部状态">
              <el-option label="待办" :value="0" />
              <el-option label="处理中" :value="1" />
              <el-option label="已完成" :value="2" />
            </el-select>
          </el-form-item>

          <el-form-item label="设备">
            <el-select v-model="taskForm.deviceId" clearable filterable placeholder="全部设备">
              <el-option
                v-for="device in devices"
                :key="device.deviceId"
                :label="`${device.stationName} / ${device.deviceName}`"
                :value="device.deviceId"
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
            <el-input v-model="taskForm.keyword" clearable placeholder="设备、测点、处理人、反馈" />
          </el-form-item>

          <el-form-item class="form-actions">
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
          <el-table-column prop="createdAt" label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="deviceName" label="设备" min-width="150" />
          <el-table-column prop="tagName" label="测点" min-width="140" />
          <el-table-column label="告警值" width="120">
            <template #default="{ row }">{{ row.alarmValue ?? '-' }}{{ row.unit ? ` ${row.unit}` : '' }}</template>
          </el-table-column>
          <el-table-column prop="alarmType" label="告警类型" min-width="130" />
          <el-table-column prop="assignee" label="处理人" min-width="120" />
          <el-table-column prop="feedback" label="反馈" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button size="small" :disabled="!canEditTasks" @click="openTaskEditor(row)">处理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeTab === 'simulation'" class="panel simulation-panel">
        <div class="simulation-header">
          <div>
            <h2>模拟数据写入</h2>
            <p>仅系统管理员可用。正常模式每 {{ simulation?.normalIntervalSeconds ?? 60 }} 秒写入一次，异常开关打开后每 {{ simulation?.burstIntervalSeconds ?? 1 }} 秒写入高频采样并按合理范围生成告警和工单。</p>
          </div>
          <el-tag :type="simulation?.running ? 'success' : 'info'">
            {{ simulation?.running ? '运行中' : '已停止' }}
          </el-tag>
        </div>

        <div class="simulation-actions">
          <el-button type="primary" :disabled="simulation?.running" :loading="isSimulationBusy" @click="handleStartSimulation">
            开始模拟数据
          </el-button>
          <el-button type="danger" :disabled="!simulation?.running" :loading="isSimulationBusy" @click="handleStopSimulation">
            停止
          </el-button>
          <el-switch
            :model-value="simulation?.anomalyEnabled ?? false"
            active-text="模拟异常数据"
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
            <span>生成告警</span>
            <strong>{{ simulation?.alarmCount ?? 0 }}</strong>
          </div>
          <div class="metric">
            <span>生成工单</span>
            <strong>{{ simulation?.taskCount ?? 0 }}</strong>
          </div>
          <div class="metric">
            <span>最近写入</span>
            <strong class="metric-time">{{ formatTime(simulation?.lastWriteAt) }}</strong>
          </div>
        </div>
      </section>

      <section v-else class="panel">
        <div class="log-toolbar">
          <el-form-item label="日志级别">
            <el-select v-model="runtimeLogLevel" @change="loadRuntimeLogs">
              <el-option
                v-for="level in runtimeLogLevelOptions"
                :key="level"
                :label="runtimeLogLevelLabels[level]"
                :value="level"
              />
            </el-select>
          </el-form-item>
          <div class="log-actions">
            <el-button :loading="isLoadingRuntimeLogs" @click="loadRuntimeLogs">刷新日志</el-button>
            <el-button @click="clearFrontendRuntimeLogs">清空前端日志</el-button>
          </div>
        </div>

        <el-table :data="visibleRuntimeLogs" border stripe height="560" v-loading="isLoadingRuntimeLogs">
          <el-table-column prop="createdAt" label="时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="来源" width="110">
            <template #default="{ row }">
              <el-tag :type="row.source === 'BACKEND' ? 'primary' : 'success'">
                {{ row.source === 'BACKEND' ? '后端' : '前端' }}
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

    <el-dialog v-model="deviceStatusDialogVisible" :title="selectedDeviceStatusLabel" width="760px">
      <el-table :data="filteredStatusDevices" border stripe max-height="480">
        <el-table-column prop="stationName" label="变电站" min-width="150" />
        <el-table-column prop="bayName" label="间隔" min-width="130" />
        <el-table-column prop="deviceName" label="设备" min-width="150" />
        <el-table-column prop="deviceType" label="类型" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="测点数" width="90">
          <template #default="{ row }">{{ row.tags.length }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="historyDetailDialogVisible" :title="`采样明细 ${formatTime(selectedHistoryGroup?.sampleTime)}`" width="860px">
      <el-table :data="selectedHistoryGroup?.rows ?? []" border stripe max-height="480">
        <el-table-column prop="deviceName" label="设备" min-width="150" />
        <el-table-column prop="tagName" label="测点" min-width="150" />
        <el-table-column prop="tagCode" label="编码" min-width="160" />
        <el-table-column label="数值" width="130">
          <template #default="{ row }">{{ row.value }}{{ row.unit ? ` ${row.unit}` : '' }}</template>
        </el-table-column>
        <el-table-column label="频率" width="100">
          <template #default="{ row }">
            <el-tag :type="row.freqFlag === 1 ? 'warning' : 'success'">{{ freqLabel(row.freqFlag) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="质量" width="100">
          <template #default="{ row }">
            <el-tag :type="historyGroupStatusType(historyGroupStatus(row.qualityFlag))">
              {{ historyGroupStatusLabel(historyGroupStatus(row.qualityFlag)) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="入库时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="taskEditDialogVisible" title="处理工单" width="560px">
      <el-form :model="taskEditForm" label-position="top">
        <el-form-item label="工单状态">
          <el-select v-model="taskEditForm.status">
            <el-option label="待办" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人">
          <el-input v-model="taskEditForm.assignee" />
        </el-form-item>
        <el-form-item label="反馈说明">
          <el-input v-model="taskEditForm.feedback" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskEditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="isLoadingTasks" @click="submitTaskUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dashboard-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  background: #edf2f7;
  color: #0f172a;
}

.sidebar {
  border-right: 1px solid #d8dee8;
  background: #101827;
  color: #e2e8f0;
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 22px 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.16);
}

.brand-mark {
  display: inline-flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  font-weight: 800;
}

.brand strong,
.brand small {
  display: block;
}

.brand small {
  color: #94a3b8;
  font-size: 12px;
}

.nav-menu {
  border-right: none;
}

.workspace {
  min-width: 0;
  padding: 24px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.topbar h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 750;
  letter-spacing: 0;
}

.topbar p {
  margin-top: 4px;
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
  min-height: 78px;
  padding: 14px 16px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  box-sizing: border-box;
}

.metric-button {
  width: 100%;
  text-align: left;
  cursor: pointer;
  font: inherit;
}

.metric-button:hover {
  border-color: #2563eb;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.10);
}

.metric span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.metric strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
}

.metric-danger {
  color: #dc2626;
}

.metric-warning {
  color: #d97706;
}

.metric-time {
  font-size: 15px !important;
}

.panel {
  padding: 16px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
}

.query-form {
  display: grid;
  grid-template-columns: repeat(6, minmax(140px, 1fr));
  gap: 12px;
  align-items: end;
  margin-bottom: 16px;
}

.history-form {
  grid-template-columns: repeat(6, minmax(140px, 1fr));
}

.task-form {
  grid-template-columns: repeat(6, minmax(140px, 1fr));
}

.form-actions {
  align-self: end;
}

.history-chart {
  width: 100%;
  height: 260px;
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.simulation-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.simulation-header h2 {
  margin: 0 0 6px;
  font-size: 20px;
  letter-spacing: 0;
}

.simulation-header p {
  color: #64748b;
}

.simulation-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.simulation-metrics {
  margin-bottom: 0;
}

.log-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.log-toolbar .el-form-item {
  width: 180px;
  margin-bottom: 0;
}

.log-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 980px) {
  .dashboard-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    border-right: none;
  }

  .query-form,
  .overview-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .workspace {
    padding: 16px;
  }

  .query-form,
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .topbar {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .log-toolbar,
  .log-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .log-toolbar .el-form-item {
    width: 100%;
  }
}
</style>
