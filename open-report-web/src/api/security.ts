import { get, post, put, del } from '@/utils/request'

export const getRowSecurityList = (params?: any) => {
  return get('/sys/row-security/list', params)
}

export const getRowSecurityPage = (params: any) => {
  return get('/sys/row-security/page', params)
}

export const createRowSecurity = (data: any) => {
  return post('/sys/row-security', data)
}

export const updateRowSecurity = (data: any) => {
  return put('/sys/row-security', data)
}

export const deleteRowSecurity = (id: number) => {
  return del(`/sys/row-security/${id}`)
}

export const getFieldPermissionList = (params?: any) => {
  return get('/sys/field-permission/list', params)
}

export const getFieldPermissionPage = (params: any) => {
  return get('/sys/field-permission/page', params)
}

export const createFieldPermission = (data: any) => {
  return post('/sys/field-permission', data)
}

export const updateFieldPermission = (data: any) => {
  return put('/sys/field-permission', data)
}

export const deleteFieldPermission = (id: number) => {
  return del(`/sys/field-permission/${id}`)
}

export const getRoleAll = () => {
  return get('/sys/role/list')
}
