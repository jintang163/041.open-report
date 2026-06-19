export interface User {
  id?: number
  username: string
  password?: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
  deptId?: number
  createTime?: string
  updateTime?: string
  roles?: RoleInfo[]
  permissions?: string[]
  menus?: MenuItem[]
}

export interface RoleInfo {
  id: number
  roleName: string
  roleCode: string
}

export interface MenuItem {
  id: number
  parentId: number
  name: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  menuType: string
  sortOrder?: number
  children?: MenuItem[]
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
  createByName?: string
  updateBy?: string
}

export interface ReportTemplateSnapshot {
  id?: number
  templateId?: number
  version?: number
  templateName?: string
  templateJson?: string
  dataSetBind?: string
  paramConfig?: string
  description?: string
  changeLog?: string
  status?: number
  createBy?: number
  createByName?: string
  createTime?: string
}

export interface ReportApproval {
  id?: number
  templateId?: number
  templateName?: string
  snapshotId?: number
  version?: number
  approvalType?: number
  approvalStatus?: number
  submitBy?: number
  submitByName?: string
  submitTime?: string
  submitRemark?: string
  approveBy?: number
  approveByName?: string
  approveTime?: string
  approveRemark?: string
}

export interface TemplateVersionDiffDTO {
  templateId?: number
  baseVersion?: number
  targetVersion?: number
  baseVersionName?: string
  targetVersionName?: string
  baseCreateByName?: string
  targetCreateByName?: string
  baseCreateTime?: string
  targetCreateTime?: string
  diffItems?: DiffItem[]
}

export interface DiffItem {
  fieldName?: string
  fieldLabel?: string
  baseValue?: string
  targetValue?: string
  diffType?: 'ADD' | 'DELETE' | 'MODIFY'
  path?: string
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
  cronExpression?: string
  cron?: string
  params?: string
  outputType?: 'EXCEL' | 'PDF' | 'EMAIL'
  emailList?: string
  emailCcList?: string
  emailSubject?: string
  emailContent?: string
  retryCount?: number
  maxRetryCount?: number
  status?: number
  createTime?: string
  updateTime?: string
  lastExecuteTime?: string
  nextExecuteTime?: string
}

export interface ScheduleLog {
  id?: number
  scheduleId?: number
  jobId?: number
  reportId?: number
  executeType?: 'MANUAL' | 'SCHEDULE' | 'RETRY'
  params?: string
  status?: 'RUNNING' | 'SUCCESS' | 'FAIL' | number
  message?: string
  retryCount?: number
  costTime?: number
  duration?: number
  errorMsg?: string
  outputPath?: string
  executeTime?: string
  createTime?: string
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

export type SubscribeChannel = 'DINGTALK' | 'WECOM' | 'EMAIL'
export type PushFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY'
export type MessageFormat = 'MARKDOWN' | 'CARD' | 'TEXT'
export type NotifyStatus = 'PENDING' | 'SUCCESS' | 'FAIL' | 'RETRY'

export interface ReportSubscription {
  id?: number
  name: string
  reportId: number
  reportName?: string
  channels: string
  frequency: PushFrequency
  pushTime?: string
  pushDayOfWeek?: number
  pushDayOfMonth?: number
  dingtalkWebhook?: string
  dingtalkSecret?: string
  wecomWebhook?: string
  emailList?: string
  emailCcList?: string
  emailSubject?: string
  messageFormat: MessageFormat
  contentTemplate?: string
  includeChart?: boolean
  includeAttachment?: boolean
  attachmentType?: 'EXCEL' | 'PDF'
  params?: string
  retryCount?: number
  maxRetryCount?: number
  status?: number
  lastPushTime?: string
  nextPushTime?: string
  createTime?: string
  updateTime?: string
}

export interface SubscriptionNotifyLog {
  id?: number
  subscriptionId?: number
  reportId?: number
  channel?: SubscribeChannel
  status?: NotifyStatus
  retryCount?: number
  messageFormat?: MessageFormat
  requestData?: string
  responseData?: string
  errorMsg?: string
  costTime?: number
  createTime?: string
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

export interface RowSecurityRule {
  id?: number
  roleId: number
  tableName: string
  filterExpression: string
  description?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface FieldPermissionRule {
  id?: number
  roleId: number
  tableName: string
  fieldName: string
  permissionType: 'HIDDEN' | 'MASKED'
  description?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface SysDept {
  id?: number
  parentId: number
  deptName: string
  deptCode: string
  leader?: string
  sortOrder?: number
  status?: number
  createTime?: string
  updateTime?: string
}

export type PivotFieldType = 'ROW' | 'COLUMN' | 'VALUE'

export type AggregateFunction = 'SUM' | 'COUNT' | 'AVG' | 'MAX' | 'MIN'

export interface PivotField {
  fieldName: string
  displayName?: string
  fieldType: PivotFieldType
  aggregateFunction?: AggregateFunction
  sortOrder?: number
}

export interface PivotTableConfig {
  dataSetId: number
  rowFields: PivotField[]
  columnFields: PivotField[]
  valueFields: PivotField[]
  showSubtotal?: boolean
  showGrandTotal?: boolean
  subtotalPosition?: 'top' | 'bottom'
}

export interface PivotHeaderCell {
  value: any
  rowSpan?: number
  colSpan?: number
  level?: number
  isLeaf?: boolean
  fieldName?: string
  fieldValue?: any
}

export interface PivotDataCell {
  rowIndex: number
  colIndex: number
  value: any
  formattedValue?: string
  aggregateFunction?: string
  isSubtotal?: boolean
  isGrandTotal?: boolean
}

export interface PivotTableResult {
  rowHeaders: PivotHeaderCell[][]
  columnHeaders: PivotHeaderCell[][]
  dataCells: PivotDataCell[][]
  summary?: Record<string, any>
  drillDownFields?: string[]
}

export type FunctionCategory = 'MATH' | 'DATE' | 'STRING' | 'LOGIC' | 'CUSTOM'

export interface FunctionParam {
  name: string
  type: string
  required?: boolean
  description?: string
}

export interface ReportFunction {
  id?: number
  funcName: string
  funcLabel: string
  funcCategory?: FunctionCategory
  description?: string
  paramConfig?: string
  params?: FunctionParam[]
  returnType?: string
  example?: string
  status?: number
  currentVersion?: number
  scriptContent?: string
  changeLog?: string
  createBy?: number
  createTime?: string
  updateBy?: number
  updateTime?: string
}

export interface ReportFunctionVersion {
  id?: number
  funcId: number
  version: number
  scriptContent?: string
  scriptType?: string
  changeLog?: string
  createBy?: number
  createTime?: string
}

export interface ReportSnapshotConfig {
  id?: number
  reportId: number
  reportName?: string
  enabled?: number
  cronExpression: string
  retentionDays?: number
  snapshotType?: string
  storageType?: string
  paramsJson?: string
  description?: string
  lastSnapshotTime?: string
  lastSnapshotId?: number
  snapshotCount?: number
  maxSnapshots?: number
  shardEnabled?: number
  shardThresholdRows?: number
  shardPageSize?: number
  status?: number
  createBy?: number
  createByName?: string
  createTime?: string
  updateTime?: string
}

export interface ReportDataSnapshot {
  id?: number
  reportId?: number
  reportName?: string
  configId?: number
  snapshotName?: string
  snapshotType?: string
  storageType?: string
  dataVersion?: string
  paramsJson?: string
  dataJson?: string
  dataSize?: number
  rowCount?: number
  tableCount?: number
  executeTime?: number
  dataHash?: string
  expireTime?: string
  status?: number
  errorMsg?: string
  createBy?: number
  createByName?: string
  createTime?: string
}

export interface SnapshotComparisonResult {
  success: boolean
  message?: string
  baseSnapshot?: {
    id: number
    name: string
    dataVersion: string
    createTime: string
    rowCount: number
    dataSize: number
    tableCount: number
    dataHash: string
  }
  targetSnapshot?: {
    id: number
    name: string
    dataVersion: string
    createTime: string
    rowCount: number
    dataSize: number
    tableCount: number
    dataHash: string
  }
  realtimeInfo?: {
    name: string
    time: string
  }
  tablesComparison?: Array<{
    bindName: string
    baseRows: number
    targetRows: number
    rowDiff: number
    rowDiffPercent: string
    baseCols: number
    targetCols: number
    colDiff: number
    xField: string
    yField: string
    chartData: Array<{
      x: string
      category: string
      value: number
    }>
  }>
  summary?: {
    baseRowCount?: number
    targetRowCount?: number
    snapshotRowCount?: number
    realtimeRowCount?: number
    totalRowDiff?: number
    rowDiff?: number
    rowDiffPercent?: string
    dataHashChanged?: boolean
    baseDataSize?: number
    targetDataSize?: number
    sizeDiff?: number
    hoursDiff?: number
  }
  realtimeData?: any
}

export interface SnapshotPageData {
  success: boolean
  message?: string
  snapshotId?: number
  bindName?: string
  pageNum?: number
  pageSize?: number
  total?: number
  totalPages?: number
  hasMore?: boolean
  columns?: Array<Record<string, any>>
  rows?: Array<Record<string, any>>
  storageType?: string
  isSharded?: boolean
}

export interface SnapshotStorageInfo {
  success: boolean
  message?: string
  snapshotId?: number
  storageType?: string
  rowCount?: number
  dataSize?: number
  storageInfo?: {
    totalShards?: number
    bindNames?: string[]
    shardSize?: number
    engine?: string
  }
}

export interface DataLineage {
  id?: number
  reportId?: number
  reportName?: string
  reportField?: string
  reportFieldTitle?: string
  dataSetId?: number
  dataSetName?: string
  dataSetField?: string
  bindName?: string
  expression?: string
  lineageType?: 'DIRECT' | 'EXPRESSION' | 'AGGREGATION'
  datasourceId?: number
  datasourceName?: string
  datasourceType?: string
  databaseName?: string
  schemaName?: string
  tableName?: string
  columnName?: string
  sourceTables?: string
  sourceColumns?: string
  sqlText?: string
  lineageHash?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface LineageTreeNode {
  id: string
  name: string
  type: 'report' | 'dataSet' | 'table' | 'column' | 'reportField'
  children?: LineageTreeNode[]
  field?: string
  title?: string
  expression?: string
  lineageType?: string
  tableName?: string
  columnName?: string
  datasourceId?: number
  datasourceName?: string
  databaseName?: string
  schemaName?: string
  dataSetField?: string
}

export interface LineageTraceNode {
  level: number
  type: 'report' | 'dataSet' | 'database'
  name: string
  field?: string
  title?: string
  expression?: string
  lineageType?: string
  datasourceName?: string
  databaseName?: string
  tableName?: string
  columnName?: string
  sqlText?: string
}

export interface LineageTraceResult {
  success: boolean
  message?: string
  reportId?: number
  reportField?: string
  trace?: LineageTraceNode[]
  lineage?: DataLineage
}

export interface LineageTreeResult {
  success: boolean
  message?: string
  reportId?: number
  tree?: LineageTreeNode
  lineageCount?: number
  dataSetCount?: number
  tableCount?: number
}

export interface ImpactAnalysisResult {
  success: boolean
  message?: string
  datasourceId?: number
  tableName?: string
  columnName?: string
  datasourceName?: string
  datasourceType?: string
  affectedReportCount?: number
  affectedDataSetCount?: number
  affectedFieldCount?: number
  affectedReports?: DataLineage[]
  affectedDataSets?: DataLineage[]
  affectedFields?: string[]
  lineageByReport?: Record<string, DataLineage[]>
  allLineage?: DataLineage[]
}

export interface SqlParseResult {
  success: boolean
  message?: string
  dataSetId?: number
  dataSetName?: string
  sqlText?: string
  tables?: string[]
  columns?: string[]
  selectColumns?: string[]
  whereColumns?: string[]
  aggregations?: string[]
  hasAggregation?: boolean
  tableAliases?: Record<string, string>
  mainTable?: string
  datasourceId?: number
  datasourceName?: string
  datasourceType?: string
  databaseName?: string
  schemaName?: string
}

export interface LineageRefreshResult {
  success: boolean
  message?: string
  reportId?: number
  reportName?: string
  dataSetId?: number
  dataSetName?: string
  lineageCount?: number
  affectedReportCount?: number
  impactSummaries?: Array<{
    datasourceId?: number
    datasourceName?: string
    tableName?: string
    affectedReportCount?: number
    affectedReports?: Array<{
      reportId?: number
      reportName?: string
      reportField?: string
    }>
  }>
}

export interface ReportSnapshotShard {
  id?: number
  snapshotId?: number
  reportId?: number
  configId?: number
  bindName?: string
  datasetId?: number
  shardIndex?: number
  shardType?: string
  pageNum?: number
  pageSize?: number
  startIndex?: number
  endIndex?: number
  rowCount?: number
  dataJson?: string
  dataSize?: number
  storageEngine?: string
  createTime?: string
}

export interface FunctionDoc {
  id?: number
  name: string
  label: string
  category: FunctionCategory
  description?: string
  params?: FunctionParam[]
  returnType?: string
  example?: string
  status: number
}
