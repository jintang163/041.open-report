import { get, post, put, del } from '@/utils/request'
import { SysRole, SysMenu, PageParams, PageResult } from '@/types'

export const getRoleList = (params: PageParams): Promise<PageResult<SysRole>> => {
  return get('/system/role/list', params)
}

export const getRoleAll = (): Promise<SysRole[]> => {
  return get('/system/role/all')
}

export const getRoleById = (id: number): Promise<SysRole> => {
  return get(`/system/role/${id}`)
}

export const createRole = (data: SysRole): Promise<SysRole> => {
  return post('/system/role', data)
}

export const updateRole = (data: SysRole): Promise<SysRole> => {
  return put('/system/role', data)
}

export const deleteRole = (id: number): Promise<void> => {
  return del(`/system/role/${id}`)
}

export const batchDeleteRole = (ids: number[]): Promise<void> => {
  return post('/system/role/batch-delete', { ids })
}

export const getRoleMenuIds = (roleId: number): Promise<number[]> => {
  return get(`/system/role/${roleId}/menu-ids`)
}

export const assignRoleMenus = (roleId: number, menuIds: number[]): Promise<void> => {
  return post(`/system/role/${roleId}/assign-menus`, { menuIds })
}

export const getMenuTree = (): Promise<SysMenu[]> => {
  return get('/system/menu/tree')
}

export const getMenuList = (): Promise<SysMenu[]> => {
  return get('/system/menu/list')
}

export const getMenuById = (id: number): Promise<SysMenu> => {
  return get(`/system/menu/${id}`)
}

export const createMenu = (data: SysMenu): Promise<SysMenu> => {
  return post('/system/menu', data)
}

export const updateMenu = (data: SysMenu): Promise<SysMenu> => {
  return put('/system/menu', data)
}

export const deleteMenu = (id: number): Promise<void> => {
  return del(`/system/menu/${id}`)
}
