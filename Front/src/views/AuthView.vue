<script setup lang="ts">
import { reactive, ref } from 'vue'
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
</script>

<template>
  <main class="auth-page">
    <section class="auth-panel">
      <header>
        <h1>用户认证</h1>
        <p>变电站监测系统</p>
      </header>

      <div class="auth-tabs" aria-label="认证方式">
        <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">
          登录
        </button>
        <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">
          注册
        </button>
      </div>

      <form v-if="mode === 'login'" class="auth-form" @submit.prevent="handleLogin">
        <label>
          用户名
          <input
            v-model="loginForm.username"
            name="username"
            autocomplete="username"
            placeholder="admin / operator / engineer01"
          />
        </label>

        <label>
          密码
          <input
            v-model="loginForm.password"
            name="password"
            type="password"
            autocomplete="current-password"
          />
        </label>

        <button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '提交中...' : '登录' }}
        </button>
      </form>

      <form v-else class="auth-form" @submit.prevent="handleRegister">
        <label>
          用户名
          <input v-model="registerForm.username" name="username" autocomplete="username" />
        </label>

        <label>
          显示名称
          <input v-model="registerForm.displayName" name="displayName" autocomplete="name" />
        </label>

        <label>
          角色
          <select v-model="registerForm.roleCode" name="roleCode">
            <option v-for="role in roleOptions" :key="role.value" :value="role.value">
              {{ role.label }}
            </option>
          </select>
        </label>

        <label>
          密码
          <input
            v-model="registerForm.password"
            name="password"
            type="password"
            autocomplete="new-password"
          />
        </label>

        <label>
          确认密码
          <input
            v-model="registerForm.confirmPassword"
            name="confirmPassword"
            type="password"
            autocomplete="new-password"
          />
        </label>

        <button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '提交中...' : '注册' }}
        </button>
      </form>

      <p v-if="message" class="auth-message">{{ message }}</p>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef2f7;
  padding: 32px 16px;
  box-sizing: border-box;
}

.auth-panel {
  width: min(420px, calc(100% - 32px));
  padding: 28px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.12);
  text-align: left;
}

.auth-panel header {
  margin-bottom: 20px;
}

.auth-panel h1 {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0;
  color: #111827;
}

.auth-panel p {
  color: #64748b;
}

.auth-tabs,
.auth-form {
  display: flex;
  gap: 12px;
}

.auth-tabs {
  margin: 24px 0;
}

.auth-tabs button {
  flex: 1;
  min-height: 36px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  cursor: pointer;
}

.auth-tabs .active {
  font-weight: 700;
  color: #fff;
  background: #1d4ed8;
  border-color: #1d4ed8;
}

.auth-form {
  flex-direction: column;
}

.auth-form label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: left;
  color: #334155;
}

.auth-form input,
.auth-form select,
.auth-form button {
  min-height: 36px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  padding: 0 10px;
  box-sizing: border-box;
}

.auth-form button {
  color: #fff;
  border-color: #0f172a;
  background: #0f172a;
  cursor: pointer;
}

.auth-form button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.auth-message {
  margin-top: 16px;
  color: #1d4ed8;
}

</style>
