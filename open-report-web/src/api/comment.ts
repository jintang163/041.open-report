import { get, post, del } from '@/utils/request'
import type { ReportComment } from '@/types'

export const getCommentsByTemplateId = (
  templateId: number,
  snapshotVersion?: number
): Promise<ReportComment[]> => {
  const params: Record<string, any> = {}
  if (snapshotVersion !== undefined) params.snapshotVersion = snapshotVersion
  return get(`/report-comment/template/${templateId}`, params)
}

export const getCommentsByCellRef = (
  templateId: number,
  cellRef: string
): Promise<ReportComment[]> => {
  return get(`/report-comment/template/${templateId}/cell/${cellRef}`)
}

export const getCommentsByChartId = (
  templateId: number,
  chartId: string
): Promise<ReportComment[]> => {
  return get(`/report-comment/template/${templateId}/chart/${chartId}`)
}

export const getCellRefsWithComments = (templateId: number): Promise<string[]> => {
  return get(`/report-comment/template/${templateId}/cell-refs`)
}

export const getChartIdsWithComments = (templateId: number): Promise<string[]> => {
  return get(`/report-comment/template/${templateId}/chart-ids`)
}

export const getCellRefsWithCommentsByVersion = (
  templateId: number,
  snapshotVersion: number
): Promise<string[]> => {
  return get(`/report-comment/template/${templateId}/version/${snapshotVersion}/cell-refs`)
}

export const getChartIdsWithCommentsByVersion = (
  templateId: number,
  snapshotVersion: number
): Promise<string[]> => {
  return get(`/report-comment/template/${templateId}/version/${snapshotVersion}/chart-ids`)
}

export const getCommentCount = (templateId: number): Promise<number> => {
  return get(`/report-comment/template/${templateId}/count`)
}

export const addComment = (data: Partial<ReportComment>): Promise<ReportComment> => {
  return post('/report-comment', data)
}

export const addReply = (
  parentId: number,
  data: Partial<ReportComment>
): Promise<ReportComment> => {
  return post(`/report-comment/${parentId}/reply`, data)
}

export const deleteComment = (commentId: number): Promise<void> => {
  return del(`/report-comment/${commentId}`)
}

export const toggleCommentLike = (commentId: number): Promise<boolean> => {
  return post(`/report-comment/${commentId}/like`)
}
