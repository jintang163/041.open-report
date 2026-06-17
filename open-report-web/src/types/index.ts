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
  templateName: string
  templateCode?: string
  templateType?: number
  templateJson?: string
  dataSetBind?: string
  paramConfig?: string
  description?: string
  status?: number
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

export type RowStatus = 'INSERT' | 'UPDATE' | 'DELETE'
export type SubmitStatus = 'PROCESSING' | 'SUCCESS' | 'PARTIAL' | 'FAIL' | 'PENDING'
export type FieldType = 'STRING' | 'NUMBER' | 'DATE' | 'DATETIME' | 'BOOLEAN'

export interface WritebackField {
  id?: number
  cellPosition: string
  fieldName: string
  fieldType: FieldType
  editable?: number
  required?: number
  defaultValue?: string
  validationRule?: string
  validationMessage?: string
}

export interface WritebackConfig {
  id?: number
  reportId: number
  dataSourceId: number
  tableName: string
  primaryKeyField: string
  primaryKeyColumn?: string
  versionField?: string
  logicDeleteField?: string
  logicDeleteValue?: string
  logicNotDeleteValue?: string
  batchSupport?: number
  transactionEnable?: number
  fields?: WritebackField[]
  createTime?: string
  updateTime?: string
}

export interface CellDataChange {
  rowIndex: number
  rowStatus: RowStatus
  oldData?: Record<string, any>
  newData?: Record<string, any>
  cellValues?: Record<string, string>
}

export interface DataSubmitRequest {
  reportId: number
  configId?: number
  params?: Record<string, any>
  changes: CellDataChange[]
}

export interface DataSubmitResult {
  batchNo: string
  status: SubmitStatus
  totalCount: number
  successCount: number
  failCount: number
  executeTime?: number
  errorMsg?: string
  details?: SubmitDetailResult[]
}

export interface SubmitDetailResult {
  rowIndex: number
  rowStatus: RowStatus
  status: SubmitStatus
  errorMsg?: string
  executeSql?: string
}

export interface WritebackHistory {
  id: number
  reportId: number
  configId: number
  batchNo: string
  totalCount: number
  successCount: number
  failCount: number
  status: SubmitStatus
  executeTime?: number
  errorMsg?: string
  params?: string
  createTime: string
  createBy?: number
  details?: WritebackDetail[]
}

export interface WritebackDetail {
  id: number
  historyId: number
  rowIndex: number
  rowStatus: RowStatus
  primaryKeyValue?: string
  oldData?: string
  newData?: string
  status: SubmitStatus
  executeSql?: string
  errorMsg?: string
  createTime: string
}

export interface EditableCellConfig {
  editable: boolean
  fieldName?: string
  fieldType?: FieldType
  required?: boolean
  validationRule?: string
  validationMessage?: string
  cellPosition: string
}

export interface EditableRowData {
  rowIndex: number
  rowStatus: RowStatus
  originalData: Record<string, any>
  currentData: Record<string, any>
  cells: Record<string, EditableCellConfig>
  dirty: boolean
}
