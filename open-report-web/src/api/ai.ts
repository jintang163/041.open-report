import { get, post } from '@/utils/request'

export interface AiGenerateRequest {
  prompt: string
  dsId?: number
  tableNames?: string
  schemaInfo?: string
  reportType?: string
}

export interface AiChartSuggestion {
  chartType: string
  title: string
  xField: string
  yFields: string[]
  description?: string
}

export interface AiFieldInfo {
  name: string
  type: string
  label: string
}

export interface AiGenerateResult {
  sql: string
  description: string
  charts?: AiChartSuggestion[]
  fields: AiFieldInfo[]
  reportTitle: string
}

export interface GeneratedReportResult {
  reportId: number
  reportName: string
  dataSetId: number
  dataSetName: string
  message: string
}

export const getAiStatus = (): Promise<{ enabled: boolean; mode: string }> => {
  return get('/ai-report/status')
}

export const generateAiReport = (data: AiGenerateRequest): Promise<AiGenerateResult> => {
  return post('/ai-report/generate', data)
}

export const generateAiSql = (data: AiGenerateRequest): Promise<AiGenerateResult> => {
  return post('/ai-report/generate-sql', data)
}

export const createReportFromPrompt = (data: AiGenerateRequest): Promise<GeneratedReportResult> => {
  return post('/ai-report/create-report', data)
}

export const createReportFromResult = (
  aiResult: AiGenerateResult, dsId: number
): Promise<GeneratedReportResult> => {
  return post('/ai-report/create-from-result', { aiResult, dsId })
}
