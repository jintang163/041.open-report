import { create } from 'zustand'
import { ReportParam, ReportRenderResult, initParamValues, formatParamValue } from '../utils/report'
import { getReportParameters, executeReport, exportReportExcel, exportReportPdf, getReportDataPage } from '@/api/report'
import { downloadBlob } from '../utils/report'
import dayjs from 'dayjs'

const VIRTUAL_SCROLL_THRESHOLD = 500
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

    set({ loading: true })
    try {
      const formattedParams = formatParamValue(params, paramValues)
      const result = await executeReport(reportId, formattedParams)
      const renderResult = result as ReportRenderResult

      const tables = renderResult?.tables || []
      const firstTableRows = tables[0]?.rows || []

      if (firstTableRows.length >= VIRTUAL_SCROLL_THRESHOLD) {
        set({
          pageMode: true,
          pageData: firstTableRows,
          pageColumns: tables[0]?.columns || [],
          pageNum: 1,
          hasMore: true,
          totalRows: firstTableRows.length,
          reportData: renderResult,
          dataSetId: undefined
        })

        try {
          const pageResult = await getReportDataPage(
            reportId,
            formattedParams,
            1,
            DEFAULT_PAGE_SIZE
          )
          if (pageResult?.success !== false && pageResult?.rows) {
            set({
              pageData: pageResult.rows,
              pageColumns: pageResult.columns || [],
              pageNum: 1,
              pageSize: DEFAULT_PAGE_SIZE,
              hasMore: pageResult.hasMore ?? pageResult.rows.length >= DEFAULT_PAGE_SIZE,
              totalRows: pageResult.total ?? 0
            })
          }
        } catch (e) {
          console.warn('分页查询失败，使用原始数据:', e)
        }
      } else {
        set({
          reportData: renderResult,
          pageMode: false,
          pageData: [],
          pageColumns: []
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
      const result = await getReportDataPage(
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
          pageColumns: result.columns || get().pageColumns
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
      pageLoading: false
    })
  }
}))
