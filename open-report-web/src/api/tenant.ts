import { get, post, put, del } from '@/utils/request'
import { SysTenant, TenantDatasourceMappingVO, PageParams, PageResult } from '@/types'

export const getTenantPage = (params: PageParams & { tenantName?: string }): Promise<PageResult<SysTenant>> => {
  return get('/tenant/page', params)
}

export const getTenantList = (): Promise<SysTenant[]> => {
  return get('/tenant/list')
}

export const getTenantById = (id: number): Promise<SysTenant> => {
  return get(`/tenant/${id}`)
}

export const createTenant = (data: SysTenant): Promise<void> => {
  return post('/tenant', data)
}

export const updateTenant = (data: SysTenant): Promise<void> => {
  return put('/tenant', data)
}

export const deleteTenant = (id: number): Promise<void> => {
  return del(`/tenant/${id}`)
}

export const getCurrentTenant = (): Promise<SysTenant> => {
  return get('/tenant/current')
}

export const getTenantDatasourceMappings = (tenantId: number): Promise<TenantDatasourceMappingVO[]> => {
  return get(`/tenant-datasource/tenant/${tenantId}`)
}

export const getCurrentTenantDatasourceMappings = (): Promise<TenantDatasourceMappingVO[]> => {
  return get('/tenant-datasource/current')
}

export const resolveTenantDatasource = (originalDsId: number): Promise<{
  id: number
  dsName: string
  dsCode: string
  dsType: string
  status: number
}> => {
  return get(`/tenant-datasource/resolve/${originalDsId}`)
}

export const saveTenantDatasourceMapping = (data: {
  tenantId: number
  originalDsId: number
  targetDsId: number
}): Promise<void> => {
  return post('/tenant-datasource', data)
}

export const updateTenantDatasourceMapping = (id: number, targetDsId: number): Promise<void> => {
  return put(`/tenant-datasource/${id}`, { targetDsId })
}

export const deleteTenantDatasourceMapping = (id: number): Promise<void> => {
  return del(`/tenant-datasource/${id}`)
}

export const batchSaveTenantDatasourceMappings = (data: {
  tenantId: number
  mappings: Array<{ originalDsId: number; targetDsId: number }>
}): Promise<void> => {
  return post('/tenant-datasource/batch', data)
}

export const testTenantDatasourceConnection = (
  tenantId: number,
  originalDsId: number
): Promise<{ success: boolean; message: string; dsName?: string; dsId?: number }> => {
  return post(`/tenant-datasource/test/${tenantId}/${originalDsId}`)
}
