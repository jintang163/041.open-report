import { get, post } from '@/utils/request'

export interface EmbedReportConfig {
  id: number
  templateName: string
  templateCode: string
  description: string
  templateJson: string
  params: Array<{
    name: string
    label?: string
    type: string
    defaultValue?: any
    required?: boolean
  }>
}

export interface EmbedReportData {
  templateId: number
  templateName: string
  templateJson: string
  dataSets: Record<string, any>
  error?: string
}

export interface EmbedExportResult {
  templateId: number
  templateName: string
  exportType: string
  exportData: Record<string, any>
  fileName: string
  error?: string
}

export interface EmbedTokenResult {
  token: string
  reportId: number
  reportName: string
  expireSeconds: number
  createTime: number
  expireTime: number
}

export interface BatchExportItem {
  reportId: number
  success: boolean
  message?: string
  reportName?: string
  exportType?: string
  fileName?: string
  downloadUrl?: string
  exportData?: Record<string, any>
}

export interface BatchExportRequest {
  reportIds: number[]
  exportType?: 'excel' | 'pdf' | 'html'
  params?: Record<string, any>
}

export const getEmbedReportConfig = (
  id: number,
  token: string
): Promise<EmbedReportConfig> => {
  return get(`/api/embed/report/${id}`, { token })
}

export const getEmbedReportData = (
  id: number,
  token: string,
  params?: Record<string, any>
): Promise<EmbedReportData> => {
  const queryParams = { ...params, token }
  return get(`/api/embed/report/${id}/data`, queryParams)
}

export const exportEmbedReport = (
  id: number,
  token: string,
  exportType: 'excel' | 'pdf' | 'html' = 'excel',
  params?: Record<string, any>
): Promise<EmbedExportResult> => {
  return post(`/api/embed/report/${id}/export?exportType=${exportType}&token=${token}`, params)
}

export const generateEmbedToken = (
  id: number,
  expireSeconds?: number
): Promise<EmbedTokenResult> => {
  const params: Record<string, any> = {}
  if (expireSeconds) {
    params.expireSeconds = expireSeconds
  }
  return get(`/api/embed/report/${id}/token`, params)
}

export const exportReportExcel = (
  id: number,
  token?: string,
  params?: Record<string, any>
): Promise<Blob> => {
  const query = new URLSearchParams()
  if (token) query.set('token', token)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      query.set(key, String(value))
    })
  }
  return get(`/api/export/report/${id}/excel?${query.toString()}`, undefined, {
    responseType: 'blob'
  })
}

export const exportReportPdf = (
  id: number,
  token?: string,
  params?: Record<string, any>
): Promise<Blob> => {
  const query = new URLSearchParams()
  if (token) query.set('token', token)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      query.set(key, String(value))
    })
  }
  return get(`/api/export/report/${id}/pdf?${query.toString()}`, undefined, {
    responseType: 'blob'
  })
}

export const exportReportHtml = (
  id: number,
  token?: string,
  params?: Record<string, any>
): Promise<string> => {
  const query = new URLSearchParams()
  if (token) query.set('token', token)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      query.set(key, String(value))
    })
  }
  return get(`/api/export/report/${id}/html?${query.toString()}`)
}

export const batchExportReport = (
  data: BatchExportRequest
): Promise<BatchExportItem[]> => {
  return post('/api/export/report/batch', data)
}

export const downloadFile = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
