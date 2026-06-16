import { create } from 'zustand'
import { ReportParam, ReportRenderResult, initParamValues, formatParamValue } from '../utils/report'
import { getReportParameters, executeReport, exportReportExcel, exportReportPdf } from '@/api/report'
import { downloadBlob } from '../utils/report'
import dayjs from 'dayjs'

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

  executeReport: async () => {
    const { reportId, params, paramValues } = get()
    if (!reportId) return

    set({ loading: true })
    try {
      const formattedParams = formatParamValue(params, paramValues)
      const result = await executeReport(reportId, formattedParams)
      set({
        reportData: result as ReportRenderResult
      })
    } catch (error) {
      console.error('执行报表失败:', error)
    } finally {
      set({ loading: false })
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
      isFullscreen: false
    })
  }
}))
