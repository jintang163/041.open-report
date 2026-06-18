import { get, post, put, del } from '@/utils/request'
import type { ReportSubscription, SubscriptionNotifyLog, PageParams, PageResult } from '@/types'

export const getSubscriptionList = (params: PageParams & { reportId?: number; channel?: string; status?: number }): Promise<PageResult<ReportSubscription>> => {
  return get('/report-subscription/page', params)
}

export const getSubscriptionById = (id: number): Promise<ReportSubscription> => {
  return get(`/report-subscription/${id}`)
}

export const createSubscription = (data: ReportSubscription): Promise<void> => {
  return post('/report-subscription', data)
}

export const updateSubscription = (data: ReportSubscription): Promise<void> => {
  return put('/report-subscription', data)
}

export const deleteSubscription = (id: number): Promise<void> => {
  return del(`/report-subscription/${id}`)
}

export const manualPushSubscription = (id: number): Promise<void> => {
  return post(`/report-subscription/push/${id}`)
}

export const enableSubscription = (id: number): Promise<void> => {
  return post(`/report-subscription/enable/${id}`)
}

export const disableSubscription = (id: number): Promise<void> => {
  return post(`/report-subscription/disable/${id}`)
}

export const toggleSubscriptionStatus = (id: number, enable: boolean): Promise<void> => {
  return enable ? enableSubscription(id) : disableSubscription(id)
}

export const getSubscriptionNotifyLogList = (params: PageParams & { subscriptionId?: number; channel?: string; status?: string }): Promise<PageResult<SubscriptionNotifyLog>> => {
  return get('/report-subscription/notify-log/page', params)
}
