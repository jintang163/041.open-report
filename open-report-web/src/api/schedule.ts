import { get, post, put, del } from '@/utils/request'
import { ScheduleJob, ScheduleLog, PageParams, PageResult } from '@/types'

export const getScheduleJobList = (params: PageParams & { reportId?: number; status?: number }): Promise<PageResult<ScheduleJob>> => {
  return get('/report-schedule/page', params)
}

export const getScheduleJobAll = (): Promise<ScheduleJob[]> => {
  return get('/report-schedule/page', { pageNum: 1, pageSize: 1000 }).then((res: any) => res?.list || [])
}

export const getScheduleJobById = (id: number): Promise<ScheduleJob> => {
  return get(`/report-schedule/${id}`)
}

export const createScheduleJob = (data: ScheduleJob): Promise<void> => {
  return post('/report-schedule', data)
}

export const updateScheduleJob = (data: ScheduleJob): Promise<void> => {
  return put('/report-schedule', data)
}

export const deleteScheduleJob = (id: number): Promise<void> => {
  return del(`/report-schedule/${id}`)
}

export const batchDeleteScheduleJob = (ids: number[]): Promise<void> => {
  return Promise.all(ids.map(id => deleteScheduleJob(id))).then(() => {})
}

export const executeScheduleJob = (id: number): Promise<void> => {
  return post(`/report-schedule/trigger/${id}`)
}

export const enableScheduleJob = (id: number): Promise<void> => {
  return post(`/report-schedule/enable/${id}`)
}

export const disableScheduleJob = (id: number): Promise<void> => {
  return post(`/report-schedule/disable/${id}`)
}

export const toggleScheduleJobStatus = (id: number, enable: boolean): Promise<void> => {
  return enable ? enableScheduleJob(id) : disableScheduleJob(id)
}

export const getScheduleLogList = (params: PageParams & { reportId?: number; scheduleId?: number; status?: string; executeType?: string }): Promise<PageResult<ScheduleLog>> => {
  return get('/report-log/page', params)
}

export const getScheduleLogByJobId = (scheduleId: number): Promise<ScheduleLog[]> => {
  return get('/report-log/page', { pageNum: 1, pageSize: 100, scheduleId }).then((res: any) => res?.list || [])
}

export const clearScheduleLog = (jobId?: number): Promise<void> => {
  return Promise.resolve()
}
