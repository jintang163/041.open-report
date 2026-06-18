import { create } from 'zustand'
import { ReportParam, ReportRenderResult, initParamValues, formatParamValue } from '../utils/report'
import { getReportParameters, executeReport, exportReportExcel, exportReportPdf, getReportDataPage } from '@/api/report'
import { downloadBlob } from '../utils/report'
import dayjs from 'dayjs'

const BIG_DATA_THRESHOLD = 100_000
const DEFAULT_PAGE_SIZE = 200

interface PreviewState {
  reportId: number | null
  reportName: string
  params: ReportParam[]
  paramValues: Record<string, any>
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
}

export const usePreviewStore = create<PreviewState>((set, get) => ({
  reportId: null,
  reportName: '',
  params: [],
  paramValues: {},
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

  executeReport: async () => {
    const { reportId, params, paramValues } = get()
    if (!reportId) return

    set({ loading: true, pageMode: false, pageData: [], pageColumns: [], pageNum: 0, hasMore: false })
    try {
      const formattedParams = formatParamValue(params, paramValues)
      const result: any = await executeReport(reportId, formattedParams)
      const renderResult = result as ReportRenderResult

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

  loadMoreData: async () => {
    const { reportId, params, paramValues, pageMode, pageNum, pageSize, hasMore, pageLoading, pageData, dataSetId } = get()
    if (!reportId || !pageMode || !hasMore || pageLoading) return

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
  }
}))
