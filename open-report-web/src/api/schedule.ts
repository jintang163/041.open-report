import { get, post, put, del } from '@/utils/request'
import { ScheduleJob, ScheduleLog, PageParams, PageResult } from '@/types'

export const getScheduleJobList = (params: PageParams): Promise<PageResult<ScheduleJob>> => {
  return get('/schedule/job/list', params)
}

export const getScheduleJobAll = (): Promise<ScheduleJob[]> => {
  return get('/schedule/job/all')
}

export const getScheduleJobById = (id: number): Promise<ScheduleJob> => {
  return get(`/schedule/job/${id}`)
}

export const createScheduleJob = (data: ScheduleJob): Promise<ScheduleJob> => {
  return post('/schedule/job', data)
}

export const updateScheduleJob = (data: ScheduleJob): Promise<ScheduleJob> => {
  return put('/schedule/job', data)
}

export const deleteScheduleJob = (id: number): Promise<void> => {
  return del(`/schedule/job/${id}`)
}

export const batchDeleteScheduleJob = (ids: number[]): Promise<void> => {
  return post('/schedule/job/batch-delete', { ids })
}

export const executeScheduleJob = (id: number): Promise<void> => {
  return post(`/schedule/job/execute/${id}`)
}

export const toggleScheduleJobStatus = (id: number): Promise<void> => {
  return post(`/schedule/job/toggle/${id}`)
}

export const getScheduleLogList = (params: PageParams & { jobId?: number }): Promise<PageResult<ScheduleLog>> => {
  return get('/schedule/log/list', params)
}

export const getScheduleLogByJobId = (jobId: number): Promise<ScheduleLog[]> => {
  return get(`/schedule/log/job/${jobId}`)
}

export const clearScheduleLog = (jobId?: number): Promise<void> => {
  return post('/schedule/log/clear', { jobId })
}
