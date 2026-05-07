export type RoleCode = 'ADMIN' | '操作员' | '工程师' | '经理'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  displayName: string
  roleCode: RoleCode
}

export interface AuthResponse {
  id?: number
  token?: string
  username?: string
  displayName?: string
  roleCode?: RoleCode
}
