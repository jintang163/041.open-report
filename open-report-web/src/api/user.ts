import { get, post, put, del } from '@/utils/request'
import { LoginParams, LoginResult, User, PageParams, PageResult } from '@/types'

export const login = (params: LoginParams): Promise<LoginResult> => {
  return post('/auth/login', params)
}

export const logout = (): Promise<void> => {
  return post('/auth/logout')
}

export const getUserInfo = (): Promise<User> => {
  return get('/auth/user-info')
}

export const getUserList = (params: PageParams): Promise<PageResult<User>> => {
  return get('/system/user/list', params)
}

export const getUserById = (id: number): Promise<User> => {
  return get(`/system/user/${id}`)
}

export const createUser = (data: User): Promise<User> => {
  return post('/system/user', data)
}

export const updateUser = (data: User): Promise<User> => {
  return put('/system/user', data)
}

export const deleteUser = (id: number): Promise<void> => {
  return del(`/system/user/${id}`)
}

export const batchDeleteUser = (ids: number[]): Promise<void> => {
  return post('/system/user/batch-delete', { ids })
}

export const resetPassword = (id: number, password: string): Promise<void> => {
  return post(`/system/user/reset-password/${id}`, { password })
}
