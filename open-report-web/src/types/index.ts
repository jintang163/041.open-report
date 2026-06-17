export interface User {
  id?: number
  username: string
  password?: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  user: User
}

export interface SysMenu {
  id?: number
  name: string
  path: string
  icon?: string
  component?: string
  parentId?: number
  sort?: number
  type?: number
  perms?: string
  children?: SysMenu[]
}

export interface SysRole {
  id?: number
  name: string
  code?: string
  description?: string
  status?: number
  createTime?: string
}

export interface DataSourceConfig {
  id?: number
  name: string
  type: string
  host: string
  port: number
  database: string
  username: string
  password?: string
  url?: string
  driverClassName?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DataSet {
  id?: number
  name: string
  code?: string
  datasourceId: number
  datasourceName?: string
  sql: string
  params?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ReportTemplate {
  id?: number
  name: string
  code?: string
  type?: number
  template?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
  createBy?: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface PageParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
}

export interface Result<T = any> {
  code: number
  message: string
  data: T
  success: boolean
}

export interface ScheduleJob {
  id?: number
  name: string
  reportId: number
  reportName?: string
  cron: string
  status?: number
  params?: string
  createTime?: string
  updateTime?: string
  lastExecuteTime?: string
}

export interface ScheduleLog {
  id?: number
  jobId: number
  status: number
  message?: string
  executeTime?: string
  duration?: number
}

export interface DataSetParam {
  name: string
  label?: string
  type: 'STRING' | 'NUMBER' | 'DATE' | 'DATETIME' | 'BOOLEAN'
  defaultValue?: string
  required?: boolean
}

export interface ReportSchedule {
  id?: number
  name: string
  reportId?: number
  reportName?: string
  cronExpression: string
  status?: number
  createTime?: string
  updateTime?: string
}

export type DataSourceType =
  | 'MYSQL'
  | 'POSTGRESQL'
  | 'ORACLE'
  | 'SQLSERVER'
  | 'DM'
  | 'API'
  | 'EXCEL'

export interface ChartDashboard {
  id?: number
  name: string
  code?: string
  description?: string
  canvasWidth?: number
  canvasHeight?: number
  backgroundColor?: string
  refreshInterval?: number
  status?: number
  createTime?: string
  updateTime?: string
  createBy?: number
  updateBy?: number
}

export type ChartType = 'bar' | 'line' | 'pie' | 'radar' | 'scatter'

export interface ChartDashboardItem {
  id?: number
  dashboardId?: number
  title?: string
  chartType: ChartType
  datasetId?: number
  xField?: string
  yFields?: string[]
  linkageField?: string
  linkageTargetId?: number
  positionX: number
  positionY: number
  width: number
  height: number
  chartConfig?: Record<string, any>
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

export interface DashboardDetail {
  dashboard: ChartDashboard
  items: ChartDashboardItem[]
}
