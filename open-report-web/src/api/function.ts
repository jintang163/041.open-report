import { get, post, put, del } from '@/utils/request'
import { ReportFunction, ReportFunctionVersion, FunctionDoc, PageParams } from '@/types'

export interface FunctionPageParams extends PageParams {
  funcName?: string
  category?: string
  status?: number
}

export const getFunctionPage = (params: FunctionPageParams): Promise<{
  records: ReportFunction[]
  total: number
  current: number
  size: number
}> => {
  return get('/report-function/page', params)
}

export const getFunctionEnabled = (): Promise<ReportFunction[]> => {
  return get('/report-function/enabled')
}

export const getFunctionDocs = (): Promise<FunctionDoc[]> => {
  return get('/report-function/docs')
}

export const getFunctionById = (id: number): Promise<ReportFunction> => {
  return get(`/report-function/${id}`)
}

export const createFunction = (data: ReportFunction): Promise<void> => {
  return post('/report-function', data)
}

export const updateFunction = (data: ReportFunction): Promise<void> => {
  return put('/report-function', data)
}

export const deleteFunction = (id: number): Promise<void> => {
  return del(`/report-function/${id}`)
}

export const getFunctionVersions = (funcId: number): Promise<ReportFunctionVersion[]> => {
  return get(`/report-function/${funcId}/versions`)
}

export const switchFunctionVersion = (funcId: number, version: number): Promise<void> => {
  return post(`/report-function/${funcId}/switch-version`, { version })
}

export const testExecuteFunction = (funcId: number, params?: Record<string, any>): Promise<any> => {
  return post(`/report-function/${funcId}/test`, params || {})
}

export const testExecuteScript = (scriptContent: string, testData?: Record<string, any>): Promise<any> => {
  return post('/report-function/test-script', { scriptContent, testData })
}

export const validateScript = (scriptContent: string): Promise<void> => {
  return post('/report-function/validate-script', { scriptContent })
}

export const reloadFunctions = (): Promise<void> => {
  return post('/report-function/reload')
}
