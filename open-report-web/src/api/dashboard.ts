import { get, post, put, del } from '@/utils/request'
import { ChartDashboard, ChartDashboardItem, DashboardDetail, PageParams, PageResult } from '@/types'

export const getDashboardPage = (params: PageParams): Promise<PageResult<ChartDashboard>> => {
  return get('/dashboard/page', params)
}

export const getDashboardList = (): Promise<ChartDashboard[]> => {
  return get('/dashboard/list')
}

export const getDashboardDetail = (id: number): Promise<DashboardDetail> => {
  return get(`/dashboard/${id}`)
}

export const createDashboard = (data: ChartDashboard): Promise<ChartDashboard> => {
  return post('/dashboard', data)
}

export const updateDashboard = (data: ChartDashboard): Promise<void> => {
  return put('/dashboard', data)
}

export const deleteDashboard = (id: number): Promise<void> => {
  return del(`/dashboard/${id}`)
}

export const saveDashboardItems = (id: number, items: ChartDashboardItem[]): Promise<ChartDashboardItem[]> => {
  return post(`/dashboard/${id}/items`, items)
}

export const getDashboardItems = (id: number): Promise<ChartDashboardItem[]> => {
  return get(`/dashboard/${id}/items`)
}

export const getChartData = (datasetId: number, params?: Record<string, any>): Promise<Record<string, any>[]> => {
  return post(`/dashboard/chart-data/${datasetId}`, params || {})
}
