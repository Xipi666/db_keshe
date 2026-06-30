<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { login, register } from '../api/auth'
import { toSession } from '../session'
import { roleOptions } from '../types/auth'
import type { AuthSession, LoginRequest, RegisterRequest } from '../types/auth'

type AuthMode = 'login' | 'register'

interface RegisterForm extends RegisterRequest {
  confirmPassword: string
}

const emit = defineEmits<{
  login: [session: AuthSession]
}>()

const mode = ref<AuthMode>('login')
const message = ref('')
const isSubmitting = ref(false)

const loginForm = reactive<LoginRequest>({
  username: '',
  password: '',
})

const registerForm = reactive<RegisterForm>({
  username: '',
  password: '',
  confirmPassword: '',
  displayName: '',
  roleCode: 'OPERATOR',
})

function switchMode(nextMode: AuthMode) {
  mode.value = nextMode
  message.value = ''
}

function validateLoginForm() {
  if (!loginForm.username.trim()) {
    return '请输入用户名'
  }

  if (!loginForm.password) {
    return '请输入密码'
  }

  return ''
}

function validateRegisterForm() {
  if (!registerForm.username.trim()) {
    return '请输入用户名'
  }

  if (!registerForm.displayName.trim()) {
    return '请输入显示名称'
  }

  if (!registerForm.password) {
    return '请输入密码'
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    return '两次输入的密码不一致'
  }

  return ''
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失败'
}

async function handleLogin() {
  const error = validateLoginForm()

  if (error) {
    message.value = error
    return
  }

  isSubmitting.value = true
  message.value = ''

  try {
    const response = await login({
      username: loginForm.username.trim(),
      password: loginForm.password,
    })
    const session = toSession(response)
    message.value = '登录成功'
    emit('login', session)
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

async function handleRegister() {
  const error = validateRegisterForm()

  if (error) {
    message.value = error
    return
  }

  isSubmitting.value = true
  message.value = ''

  try {
    await register({
      username: registerForm.username.trim(),
      password: registerForm.password,
      displayName: registerForm.displayName.trim(),
      roleCode: registerForm.roleCode,
    })
    message.value = '注册成功，请返回登录'
    switchMode('login')
  } catch (error) {
    message.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

// ── Particle canvas ──────────────────────────────────────────
const canvasRef = ref<HTMLCanvasElement | null>(null)
let animFrameId = 0

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  r: number
  alpha: number
}

function initParticles() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const dpr = window.devicePixelRatio || 1

  function resize() {
    canvas!.width = window.innerWidth * dpr
    canvas!.height = window.innerHeight * dpr
    canvas!.style.width = window.innerWidth + 'px'
    canvas!.style.height = window.innerHeight + 'px'
    ctx!.setTransform(dpr, 0, 0, dpr, 0, 0)
  }

  resize()
  window.addEventListener('resize', resize)

  // create particles
  const count = Math.min(80, Math.floor((window.innerWidth * window.innerHeight) / 12000))
  const particles: Particle[] = []
  const connectionDist = 120

  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * window.innerWidth,
      y: Math.random() * window.innerHeight,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
      r: Math.random() * 1.8 + 0.6,
      alpha: Math.random() * 0.5 + 0.3,
    })
  }

  function draw() {
    const w = window.innerWidth
    const h = window.innerHeight
    ctx!.clearRect(0, 0, w, h)

    // draw connections
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < connectionDist) {
          const opacity = (1 - dist / connectionDist) * 0.2
          ctx!.beginPath()
          ctx!.moveTo(particles[i].x, particles[i].y)
          ctx!.lineTo(particles[j].x, particles[j].y)
          ctx!.strokeStyle = `rgba(59,130,246,${opacity.toFixed(3)})`
          ctx!.lineWidth = 0.5
          ctx!.stroke()
        }
      }
    }

    // draw particles
    for (const p of particles) {
      ctx!.beginPath()
      ctx!.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx!.fillStyle = `rgba(96,165,250,${p.alpha.toFixed(2)})`
      ctx!.fill()

      // glow
      ctx!.beginPath()
      ctx!.arc(p.x, p.y, p.r * 3, 0, Math.PI * 2)
      ctx!.fillStyle = `rgba(59,130,246,${(p.alpha * 0.12).toFixed(3)})`
      ctx!.fill()
    }

    // move
    for (const p of particles) {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0) p.x = w
      if (p.x > w) p.x = 0
      if (p.y < 0) p.y = h
      if (p.y > h) p.y = 0
    }

    animFrameId = requestAnimationFrame(draw)
  }

  draw()
}

onMounted(() => {
  initParticles()
})

onUnmounted(() => {
  cancelAnimationFrame(animFrameId)
})
</script>

<template>
  <main class="auth-page">
    <!-- particle canvas -->
    <canvas ref="canvasRef" class="particle-canvas"></canvas>

    <!-- background grid -->
    <div class="circuit-grid"></div>

    <!-- scanning line -->
    <div class="scan-line"></div>

    <!-- decorative pulse rings -->
    <div class="pulse-ring ring-1"></div>
    <div class="pulse-ring ring-2"></div>
    <div class="pulse-ring ring-3"></div>

    <section class="auth-panel">
      <!-- corner accents -->
      <span class="corner corner-tl"></span>
      <span class="corner corner-tr"></span>
      <span class="corner corner-bl"></span>
      <span class="corner corner-br"></span>

      <header>
        <div class="brand-icon">
          <span class="icon-core">⚡</span>
          <span class="icon-ring"></span>
        </div>
        <h1>
          <span class="title-accent">PSM</span>
          智能变电
        </h1>
        <p>箱式变压器 · 状态监测 · 智能运维</p>
      </header>

      <div class="auth-tabs">
        <button
          type="button"
          :class="{ active: mode === 'login' }"
          @click="switchMode('login')"
        >
          <span class="tab-label">登 录</span>
          <span class="tab-underline"></span>
        </button>
        <button
          type="button"
          :class="{ active: mode === 'register' }"
          @click="switchMode('register')"
        >
          <span class="tab-label">注 册</span>
          <span class="tab-underline"></span>
        </button>
      </div>

      <form v-if="mode === 'login'" class="auth-form" @submit.prevent="handleLogin">
        <label>
          <span class="input-label">用户名</span>
          <div class="input-wrapper">
            <span class="input-icon">👤</span>
            <input
              v-model="loginForm.username"
              name="username"
              autocomplete="username"
              placeholder="admin / operator / engineer01"
            />
          </div>
        </label>

        <label>
          <span class="input-label">密码</span>
          <div class="input-wrapper">
            <span class="input-icon">🔒</span>
            <input
              v-model="loginForm.password"
              name="password"
              type="password"
              autocomplete="current-password"
              placeholder="········"
            />
          </div>
        </label>

        <button type="submit" :disabled="isSubmitting" class="submit-btn">
          <span class="btn-bg"></span>
          <span class="btn-text">{{ isSubmitting ? '验证中...' : '进入系统' }}</span>
        </button>
      </form>

      <form v-else class="auth-form" @submit.prevent="handleRegister">
        <label>
          <span class="input-label">用户名</span>
          <div class="input-wrapper">
            <span class="input-icon">👤</span>
            <input v-model="registerForm.username" name="username" autocomplete="username" />
          </div>
        </label>

        <label>
          <span class="input-label">显示名称</span>
          <div class="input-wrapper">
            <span class="input-icon">📛</span>
            <input v-model="registerForm.displayName" name="displayName" autocomplete="name" />
          </div>
        </label>

        <label>
          <span class="input-label">角色</span>
          <div class="input-wrapper">
            <span class="input-icon">🛡️</span>
            <select v-model="registerForm.roleCode" name="roleCode">
              <option v-for="role in roleOptions" :key="role.value" :value="role.value">
                {{ role.label }}
              </option>
            </select>
          </div>
        </label>

        <label>
          <span class="input-label">密码</span>
          <div class="input-wrapper">
            <span class="input-icon">🔒</span>
            <input
              v-model="registerForm.password"
              name="password"
              type="password"
              autocomplete="new-password"
              placeholder="········"
            />
          </div>
        </label>

        <label>
          <span class="input-label">确认密码</span>
          <div class="input-wrapper">
            <span class="input-icon">🔒</span>
            <input
              v-model="registerForm.confirmPassword"
              name="confirmPassword"
              type="password"
              autocomplete="new-password"
              placeholder="········"
            />
          </div>
        </label>

        <button type="submit" :disabled="isSubmitting" class="submit-btn">
          <span class="btn-bg"></span>
          <span class="btn-text">{{ isSubmitting ? '提交中...' : '创建账户' }}</span>
        </button>
      </form>

      <p v-if="message" class="auth-message" :class="{ error: message.includes('误') || message.includes('败') || message.includes('不一致') }">
        {{ message }}
      </p>

      <div class="auth-footer">
        <span class="status-dot"></span>
        <span>系统运行中 · 安全连接</span>
      </div>
    </section>
  </main>
</template>

<style scoped>
/* ═══════════════════════════════════════════════════════════════
   AUTH VIEW — Dark Theme v2
   Cool blue-gray palette with glassmorphism accents
   ═══════════════════════════════════════════════════════════════ */

/* ── CSS custom properties (scoped mirror of global tokens) ── */
.auth-page {
  --_bg-primary: #0B1120;
  --_bg-glass: rgba(19, 28, 49, 0.88);
  --_text-primary: #F1F5F9;
  --_text-secondary: #94A3B8;
  --_accent: #3B82F6;
  --_accent-light: #60A5FA;
  --_border-subtle: rgba(255, 255, 255, 0.06);
  --_border-medium: rgba(255, 255, 255, 0.10);
}

/* ═══════════════════════════════════════════════════════════════
   PAGE BACKGROUND
   ═══════════════════════════════════════════════════════════════ */
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse at 50% 0%, #1a2640 0%, var(--_bg-primary) 70%);
  padding: 32px 16px;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
}

/* particle canvas – behind everything */
.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

/* ── Circuit grid ─────────────────────────────────────────── */
.circuit-grid {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background-image:
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 59px,
      rgba(59, 130, 246, 0.04) 59px,
      rgba(59, 130, 246, 0.04) 60px
    ),
    repeating-linear-gradient(
      90deg,
      transparent,
      transparent 59px,
      rgba(59, 130, 246, 0.04) 59px,
      rgba(59, 130, 246, 0.04) 60px
    ),
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 299px,
      rgba(59, 130, 246, 0.06) 299px,
      rgba(59, 130, 246, 0.06) 300px
    ),
    repeating-linear-gradient(
      90deg,
      transparent,
      transparent 299px,
      rgba(59, 130, 246, 0.06) 299px,
      rgba(59, 130, 246, 0.06) 300px
    );
  mask-image: radial-gradient(ellipse 80% 60% at 50% 50%, black 30%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse 80% 60% at 50% 50%, black 30%, transparent 70%);
}

/* ── Scanning line ────────────────────────────────────────── */
.scan-line {
  position: absolute;
  left: 0;
  width: 100%;
  height: 2px;
  z-index: 2;
  pointer-events: none;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(59, 130, 246, 0.2) 20%,
    rgba(96, 165, 250, 0.35) 50%,
    rgba(59, 130, 246, 0.2) 80%,
    transparent 100%
  );
  box-shadow: 0 0 20px rgba(59, 130, 246, 0.3), 0 0 60px rgba(59, 130, 246, 0.1);
  animation: scanDown 6s linear infinite;
}

@keyframes scanDown {
  0%   { top: -2px; opacity: 0; }
  5%   { opacity: 1; }
  95%  { opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

/* ── Pulse rings ──────────────────────────────────────────── */
.pulse-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(59, 130, 246, 0.2);
  z-index: 1;
  pointer-events: none;
  animation: pulseExpand 4s ease-out infinite;
}

.ring-1 { width: 280px; height: 280px; animation-delay: 0s; }
.ring-2 { width: 280px; height: 280px; animation-delay: 1.3s; }
.ring-3 { width: 280px; height: 280px; animation-delay: 2.6s; }

@keyframes pulseExpand {
  0%   { transform: scale(1); opacity: 0.5; }
  100% { transform: scale(2.8); opacity: 0; }
}

/* ═══════════════════════════════════════════════════════════════
   GLASS PANEL
   ═══════════════════════════════════════════════════════════════ */
.auth-panel {
  position: relative;
  z-index: 10;
  width: min(440px, calc(100% - 32px));
  padding: 36px 32px 28px;
  border-radius: var(--radius-lg, 14px);
  background: var(--_bg-glass);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: var(--_border-subtle);
  box-shadow:
    0 0 80px rgba(59, 130, 246, 0.08),
    0 0 30px rgba(59, 130, 246, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  text-align: center;
  overflow: hidden;
  transition: box-shadow 0.6s;
}

/* Top highlight bar */
.auth-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.08), transparent);
  pointer-events: none;
}

.auth-panel:hover {
  box-shadow:
    0 0 120px rgba(59, 130, 246, 0.12),
    0 0 40px rgba(59, 130, 246, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

/* ── Corner accents ───────────────────────────────────────── */
.corner {
  position: absolute;
  width: 24px;
  height: 24px;
  z-index: 1;
  pointer-events: none;
}

.corner::before,
.corner::after {
  content: '';
  position: absolute;
  background: rgba(96, 165, 250, 0.45);
}

.corner::before { width: 24px; height: 1.5px; }
.corner::after  { width: 1.5px; height: 24px; }

.corner-tl { top: 14px; left: 14px; }
.corner-tl::before { top: 0; left: 0; }
.corner-tl::after  { top: 0; left: 0; }

.corner-tr { top: 14px; right: 14px; }
.corner-tr::before { top: 0; right: 0; }
.corner-tr::after  { top: 0; right: 0; }

.corner-bl { bottom: 14px; left: 14px; }
.corner-bl::before { bottom: 0; left: 0; }
.corner-bl::after  { bottom: 0; left: 0; }

.corner-br { bottom: 14px; right: 14px; }
.corner-br::before { bottom: 0; right: 0; }
.corner-br::after  { bottom: 0; right: 0; }

/* ═══════════════════════════════════════════════════════════════
   HEADER
   ═══════════════════════════════════════════════════════════════ */
.auth-panel header {
  margin-bottom: 24px;
}

.brand-icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  margin-bottom: 12px;
}

.icon-core {
  font-size: 32px;
  position: relative;
  z-index: 1;
  filter: drop-shadow(0 0 8px rgba(59, 130, 246, 0.5));
  animation: iconBuzz 2.5s ease-in-out infinite;
}

@keyframes iconBuzz {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.12); }
}

.icon-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1.5px solid rgba(59, 130, 246, 0.3);
  animation: iconRingPulse 2.5s ease-in-out infinite;
}

@keyframes iconRingPulse {
  0%, 100% { transform: scale(1); opacity: 0.35; }
  50% { transform: scale(1.18); opacity: 0.08; }
}

.auth-panel h1 {
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 2px;
  color: var(--_text-primary);
}

.title-accent {
  background: linear-gradient(135deg, var(--_accent-light), #818CF8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-right: 6px;
}

.auth-panel header p {
  margin: 0;
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--_text-secondary);
  text-transform: uppercase;
}

/* ═══════════════════════════════════════════════════════════════
   TABS
   ═══════════════════════════════════════════════════════════════ */
.auth-tabs {
  display: flex;
  gap: 0;
  margin: 24px 0 22px;
  border-bottom: var(--_border-subtle);
}

.auth-tabs button {
  flex: 1;
  position: relative;
  min-height: 40px;
  border: none;
  background: transparent;
  color: var(--_text-secondary);
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 3px;
  transition: color 0.2s ease;
  overflow: hidden;
}

.auth-tabs button:hover {
  color: var(--_text-primary);
}

.auth-tabs .active {
  color: var(--_accent-light);
  font-weight: 700;
  text-shadow: 0 0 12px rgba(59, 130, 246, 0.4);
}

.tab-underline {
  position: absolute;
  bottom: 0;
  left: 50%;
  translate: -50% 0;
  width: 0;
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, transparent, var(--_accent-light), transparent);
  transition: width 0.35s ease;
}

.auth-tabs .active .tab-underline {
  width: 60%;
}

/* ═══════════════════════════════════════════════════════════════
   FORM
   ═══════════════════════════════════════════════════════════════ */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  text-align: left;
}

.auth-form label {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.input-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1.5px;
  color: var(--_text-secondary);
  text-transform: uppercase;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 12px;
  font-size: 14px;
  z-index: 1;
  pointer-events: none;
  opacity: 0.4;
}

.auth-form input,
.auth-form select {
  width: 100%;
  min-height: 42px;
  padding: 0 12px 0 38px;
  border-radius: 8px;
  border: var(--_border-subtle);
  background: rgba(19, 28, 49, 0.7);
  color: var(--_text-primary);
  font-size: 14px;
  box-sizing: border-box;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.auth-form input::placeholder {
  color: var(--_text-tertiary, #64748B);
  font-size: 12px;
}

.auth-form input:focus,
.auth-form select:focus {
  border-color: rgba(59, 130, 246, 0.5);
  background: rgba(19, 28, 49, 0.9);
  box-shadow:
    0 0 0 3px rgba(59, 130, 246, 0.08),
    0 0 18px rgba(59, 130, 246, 0.06);
}

.auth-form select {
  appearance: none;
  -webkit-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='rgba(148,163,184,0.5)' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
  cursor: pointer;
}

.auth-form select option {
  background: #131C31;
  color: var(--_text-primary);
}

/* ── Submit button ────────────────────────────────────────── */
.submit-btn {
  position: relative;
  width: 100%;
  min-height: 44px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 4px;
  color: #F1F5F9;
  margin-top: 4px;
  overflow: hidden;
  isolation: isolate;
}

.btn-bg {
  position: absolute;
  inset: 0;
  z-index: -1;
  background: linear-gradient(135deg, #2563EB, #3B82F6, #60A5FA);
  background-size: 200% 200%;
  animation: btnShimmer 3s ease infinite;
  transition: filter 0.3s;
}

@keyframes btnShimmer {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.submit-btn:hover:not(:disabled) .btn-bg {
  filter: brightness(1.15);
}

.submit-btn:hover:not(:disabled) {
  box-shadow: 0 0 24px rgba(59, 130, 246, 0.4), 0 0 48px rgba(59, 130, 246, 0.12);
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.btn-text {
  position: relative;
  z-index: 1;
}

/* ═══════════════════════════════════════════════════════════════
   MESSAGE & FOOTER
   ═══════════════════════════════════════════════════════════════ */
.auth-message {
  margin-top: 16px;
  font-size: 13px;
  color: var(--_success-text, #4ADE80);
  text-align: center;
  letter-spacing: 0.5px;
}

.auth-message.error {
  color: var(--_danger-text, #F87171);
}

.auth-footer {
  margin-top: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 1.5px;
  color: var(--_text-tertiary, #64748B);
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--_success, #22C55E);
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.5);
  animation: dotPulse 2s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

/* ═══════════════════════════════════════════════════════════════
   RESPONSIVE
   ═══════════════════════════════════════════════════════════════ */
@media (max-width: 480px) {
  .auth-panel {
    padding: 28px 20px 22px;
  }

  .auth-panel h1 {
    font-size: 24px;
  }

  .auth-panel header p {
    font-size: 11px;
    letter-spacing: 2px;
  }

  .pulse-ring {
    display: none;
  }
}
</style>
