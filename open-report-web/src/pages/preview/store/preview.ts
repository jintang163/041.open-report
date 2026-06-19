import { create } from 'zustand'
import { ReportParam, ReportRenderResult, initParamValues, formatParamValue } from '../utils/report'
import { getReportParameters, executeReport, exportReportExcel, exportReportPdf, getReportDataPage, executeReportWithSnapshot, getSnapshotConfigByReportId, getSnapshotListByReportId } from '@/api/report'
import { downloadBlob } from '../utils/report'
import dayjs from 'dayjs'
import type { ReportSnapshotConfig, ReportDataSnapshot } from '@/types'

const BIG_DATA_THRESHOLD = 100_000
const DEFAULT_PAGE_SIZE = 200

export type SnapshotMode = 'realtime' | 'latest' | 'snapshot'

interface PreviewState {
  reportId: number | null
  reportName: string
  params: ReportParam[]
  paramValues: Record<string, any>
  paramConfigs: any[]
  reportData: ReportRenderResult | null
  loading: boolean
  exporting: boolean
  isFullscreen: boolean
  isMobile: boolean

  pageMode: boolean
  pageData: any[]
  pageColumns: any[]
  pageNum: number
  pageSize: number
  totalRows: number
  hasMore: boolean
  pageLoading: boolean
  dataSetId?: string
  bigDataThreshold: number

  snapshotMode: SnapshotMode
  selectedSnapshotId: number | null
  snapshotInfo: {
    snapshotId?: number
    snapshotName?: string
    snapshotTime?: string
    dataVersion?: string
    expireTime?: string
    isSnapshot?: boolean
  } | null
  snapshotConfig: ReportSnapshotConfig | null
  snapshotList: ReportDataSnapshot[]
  snapshotLoading: boolean

  setReportId: (id: number) => void
  setReportName: (name: string) => void
  setParams: (params: ReportParam[]) => void
  setParamValues: (values: Record<string, any>) => void
  updateParamValue: (name: string, value: any) => void
  setReportData: (data: ReportRenderResult | null) => void
  setLoading: (loading: boolean) => void
  setExporting: (exporting: boolean) => void
  setIsFullscreen: (isFullscreen: boolean) => void
  toggleFullscreen: () => void
  toggleMobile: (isMobile: boolean) => void
  resetParams: () => void
  loadParams: () => Promise<void>
  executeReport: () => Promise<void>
  loadMoreData: () => Promise<void>
  resetPageData: () => void
  exportExcel: () => Promise<void>
  exportPdf: () => Promise<void>
  reset: () => void

  setSnapshotMode: (mode: SnapshotMode) => void
  setSelectedSnapshotId: (id: number | null) => void
  loadSnapshotConfig: () => Promise<void>
  loadSnapshotList: () => Promise<void>
  executeReportWithSnapshotMode: () => Promise<void>
  resetSnapshotState: () => void
}

export const usePreviewStore = create<PreviewState>((set, get) => ({
  reportId: null,
  reportName: '',
  params: [],
  paramValues: {},
  paramConfigs: [],
  reportData: null,
  loading: false,
  exporting: false,
  isFullscreen: false,
  isMobile: false,

  pageMode: false,
  pageData: [],
  pageColumns: [],
  pageNum: 0,
  pageSize: DEFAULT_PAGE_SIZE,
  totalRows: 0,
  hasMore: false,
  pageLoading: false,
  bigDataThreshold: BIG_DATA_THRESHOLD,

  snapshotMode: 'realtime',
  selectedSnapshotId: null,
  snapshotInfo: null,
  snapshotConfig: null,
  snapshotList: [],
  snapshotLoading: false,

  setReportId: (id: number) => set({ reportId: id }),

  setReportName: (name: string) => set({ reportName: name }),

  setParams: (params: ReportParam[]) => {
    const paramValues = initParamValues(params)
    set({ params, paramValues })
  },

  setParamValues: (values: Record<string, any>) => set({ paramValues: values }),

  updateParamValue: (name: string, value: any) =>
    set((state) => ({
      paramValues: { ...state.paramValues, [name]: value }
    })),

  setReportData: (data: ReportRenderResult | null) => set({ reportData: data }),

  setLoading: (loading: boolean) => set({ loading }),

  setExporting: (exporting: boolean) => set({ exporting }),

  setIsFullscreen: (isFullscreen: boolean) => set({ isFullscreen }),

  toggleFullscreen: () => set((state) => ({ isFullscreen: !state.isFullscreen })),

  toggleMobile: (isMobile: boolean) => set({ isMobile }),

  resetParams: () => {
    const { params } = get()
    const paramValues = initParamValues(params)
    set({ paramValues })
  },

  loadParams: async () => {
    const { reportId } = get()
    if (!reportId) return
    try {
      const params = await getReportParameters(reportId)
      const paramValues = initParamValues(params)
      set({ params, paramValues })
    } catch (error) {
      console.error('加载报表参数失败:', error)
    }
  },

  resetPageData: () => {
    set({
      pageMode: false,
      pageData: [],
      pageColumns: [],
      pageNum: 0,
      hasMore: false,
      totalRows: 0,
      pageLoading: false
    })
  },

  setSnapshotMode: (mode: SnapshotMode) => set({ snapshotMode: mode }),

  setSelectedSnapshotId: (id: number | null) => set({ selectedSnapshotId: id }),

  loadSnapshotConfig: async () => {
    const { reportId } = get()
    if (!reportId) return
    try {
      set({ snapshotLoading: true })
      const config = await getSnapshotConfigByReportId(reportId)
      set({ snapshotConfig: config || null })
    } catch (error) {
      console.error('加载快照配置失败:', error)
    } finally {
      set({ snapshotLoading: false })
    }
  },

  loadSnapshotList: async () => {
    const { reportId } = get()
    if (!reportId) return
    try {
      set({ snapshotLoading: true })
      const list = await getSnapshotListByReportId(reportId, 50)
      set({ snapshotList: list || [] })
    } catch (error) {
      console.error('加载快照列表失败:', error)
    } finally {
      set({ snapshotLoading: false })
    }
  },

  resetSnapshotState: () => {
    set({
      snapshotMode: 'realtime',
      selectedSnapshotId: null,
      snapshotInfo: null,
      snapshotConfig: null,
      snapshotList: [],
      snapshotLoading: false
    })
  },

  executeReportWithSnapshotMode: async () => {
    const { reportId, params, paramValues, snapshotMode, selectedSnapshotId } = get()
    if (!reportId) return

    set({ loading: true, pageMode: false, pageData: [], pageColumns: [], pageNum: 0, hasMore: false })
    try {
      const formattedParams = formatParamValue(params, paramValues)
      const result: any = await executeReportWithSnapshot(
        reportId,
        formattedParams,
        snapshotMode,
        snapshotMode === 'snapshot' ? selectedSnapshotId || undefined : undefined
      )
      const renderResult = result as ReportRenderResult

      if (result?.isSnapshot) {
        set({
          snapshotInfo: {
            snapshotId: result.snapshotId,
            snapshotName: result.snapshotName,
            snapshotTime: result.snapshotTime,
            dataVersion: result.dataVersion,
            expireTime: result.expireTime,
            isSnapshot: true
          }
        })
      } else {
        set({ snapshotInfo: null })
      }

      const serverPageMode = Boolean(result?.pageMode)
      const serverThreshold = result?.bigDataThreshold ?? BIG_DATA_THRESHOLD
      const serverPage = result?.page || {}
      const serverTables = result?.tables || []
      const firstTable = serverTables[0] || {}
      const tablesRows = firstTable?.rows || []

      if (serverPageMode) {
        const pageColumns = serverPage?.columns && serverPage.columns.length > 0
          ? serverPage.columns
          : (firstTable?.columns || [])
        const pageRows = serverPage?.rows && serverPage.rows.length > 0
          ? serverPage.rows
          : tablesRows

        set({
          reportData: {
            ...renderResult,
            title: result?.title,
            summary: result?.summary,
            charts: result?.charts,
            html: result?.html,
            table: result?.table
          },
          pageMode: true,
          pageData: pageRows || [],
          pageColumns,
          pageNum: serverPage?.pageNum || 1,
          pageSize: serverPage?.pageSize || DEFAULT_PAGE_SIZE,
          hasMore: Boolean(serverPage?.hasMore ?? (pageRows && pageRows.length >= (serverPage?.pageSize || DEFAULT_PAGE_SIZE))),
          totalRows: serverPage?.total ?? firstTable?.total ?? (pageRows ? pageRows.length : 0),
          dataSetId: serverPage?.dataSetId != null ? String(serverPage.dataSetId) : undefined,
          bigDataThreshold: serverThreshold
        })
      } else {
        set({
          reportData: {
            ...renderResult,
            title: result?.title,
            summary: result?.summary,
            charts: result?.charts,
            html: result?.html,
            table: result?.table
          },
          pageMode: false,
          pageData: [],
          pageColumns: [],
          bigDataThreshold: serverThreshold
        })
      }
    } catch (error) {
      console.error('执行报表失败:', error)
      get().resetPageData()
    } finally {
      set({ loading: false })
    }
  },

  executeReport: async () => {
    await get().executeReportWithSnapshotMode()
  },

  loadMoreData: async () => {
    const { reportId, params, paramValues, pageMode, pageNum, pageSize, hasMore, pageLoading, pageData, dataSetId, snapshotMode, selectedSnapshotId } = get()
    if (!reportId || !pageMode || !hasMore || pageLoading) return
    if (snapshotMode !== 'realtime') return

    const nextPage = pageNum + 1
    set({ pageLoading: true })
    try {
      const formattedParams = formatParamValue(params, paramValues)
      const result: any = await getReportDataPage(
        reportId,
        formattedParams,
        nextPage,
        pageSize,
        dataSetId
      )

      if (result?.success !== false && result?.rows) {
        set({
          pageData: [...pageData, ...result.rows],
          pageNum: nextPage,
          hasMore: result.hasMore ?? result.rows.length >= pageSize,
          totalRows: result.total ?? pageData.length + result.rows.length,
          pageColumns: result.columns && result.columns.length > 0 ? result.columns : get().pageColumns
        })
      }
    } catch (error) {
      console.error('加载更多数据失败:', error)
    } finally {
      set({ pageLoading: false })
    }
  },

  exportExcel: async () => {
    const { reportId, params, paramValues, reportName } = get()
    if (!reportId) return

    set({ exporting: true })
    try {
      const formattedParams = formatParamValue(params, paramValues)
      const blob = await exportReportExcel(reportId, formattedParams)
      const filename = `${reportName || '报表'}_${dayjs().format('YYYYMMDDHHmmss')}.xlsx`
      downloadBlob(blob, filename)
    } catch (error) {
      console.error('导出Excel失败:', error)
    } finally {
      set({ exporting: false })
    }
  },

  exportPdf: async () => {
    const { reportId, params, paramValues, reportName } = get()
    if (!reportId) return

    set({ exporting: true })
    try {
      const formattedParams = formatParamValue(params, paramValues)
      const blob = await exportReportPdf(reportId, formattedParams)
      const filename = `${reportName || '报表'}_${dayjs().format('YYYYMMDDHHmmss')}.pdf`
      downloadBlob(blob, filename)
    } catch (error) {
      console.error('导出PDF失败:', error)
    } finally {
      set({ exporting: false })
    }
  },

  reset: () => {
    set({
      reportId: null,
      reportName: '',
      params: [],
      paramValues: {},
      paramConfigs: [],
      reportData: null,
      loading: false,
      exporting: false,
      isFullscreen: false,
      pageMode: false,
      pageData: [],
      pageColumns: [],
      pageNum: 0,
      hasMore: false,
      totalRows: 0,
      pageLoading: false,
      bigDataThreshold: BIG_DATA_THRESHOLD
    })
    get().resetSnapshotState()
  }
}))
