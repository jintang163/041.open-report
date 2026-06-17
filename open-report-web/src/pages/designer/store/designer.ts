import { create } from 'zustand'
import type { DataSet, DataSourceConfig, WritebackConfig, WritebackField } from '../../../types'

export interface FieldItem {
  name: string
  type: string
  label?: string
}

export interface DataSetWithFields extends DataSet {
  fields?: FieldItem[]
}

export interface DataSourceWithDataSets extends DataSourceConfig {
  dataSets?: DataSetWithFields[]
}

export interface CellPosition {
  row: number
  col: number
  rowSpan?: number
  colSpan?: number
}

export interface CellStyle {
  fontSize?: number
  fontFamily?: string
  fontWeight?: string
  fontStyle?: string
  textDecoration?: string
  color?: string
  backgroundColor?: string
  textAlign?: 'left' | 'center' | 'right'
  verticalAlign?: 'top' | 'middle' | 'bottom'
  borderTop?: string
  borderBottom?: string
  borderLeft?: string
  borderRight?: string
  wrapText?: boolean
}

export interface CellDataBinding {
  type: 'field' | 'expression' | 'static'
  dataset?: string
  field?: string
  expression?: string
  format?: string
}

export interface ConditionalFormatRule {
  id: string
  type: 'cellValue' | 'expression' | 'dataBar' | 'colorScale' | 'iconSet'
  operator?: 'equal' | 'notEqual' | 'greaterThan' | 'lessThan' | 'between' | 'contains'
  value1?: string
  value2?: string
  expression?: string
  style?: Partial<CellStyle>
  range?: string
}

export interface ChartConfig {
  id: string
  type: 'bar' | 'line' | 'pie' | 'area' | 'scatter' | 'radar'
  title?: string
  datasetId?: number
  datasetName?: string
  xAxisField?: string
  yAxisFields?: string[]
  seriesName?: string
  position?: {
    startRow: number
    startCol: number
    endRow: number
    endCol: number
  }
  x?: number
  y?: number
  width?: number
  height?: number
  linkageField?: string
  linkageTargetId?: string
}

export interface DesignerState {
  luckysheetInstance: any | null
  selectedCell: CellPosition | null
  selectedRange: { start: CellPosition; end: CellPosition } | null
  selectedChartId: string | null
  cellValue: string
  dataSources: DataSourceWithDataSets[]
  conditionalFormats: ConditionalFormatRule[]
  charts: ChartConfig[]
  templateName: string
  templateId: number | null
  undoStack: any[]
  redoStack: any[]
  expressionEditorVisible: boolean
  expressionEditorInitialValue: string
  conditionalFormatVisible: boolean
  chartConfigVisible: boolean
  editingChart: ChartConfig | null
  expandedDataSourceKeys: string[]
  expandedDataSetKeys: string[]
  writebackConfigVisible: boolean
  writebackConfigs: WritebackConfig[]
  currentWritebackConfig: WritebackConfig | null

  setLuckysheetInstance: (instance: any) => void
  setSelectedCell: (cell: CellPosition | null) => void
  setSelectedRange: (range: { start: CellPosition; end: CellPosition } | null) => void
  setSelectedChartId: (id: string | null) => void
  setCellValue: (value: string) => void
  setDataSources: (sources: DataSourceWithDataSets[]) => void
  setConditionalFormats: (formats: ConditionalFormatRule[]) => void
  setCharts: (charts: ChartConfig[]) => void
  addChart: (chart: ChartConfig) => void
  updateChart: (chart: ChartConfig) => void
  removeChart: (chartId: string) => void
  setTemplateName: (name: string) => void
  setTemplateId: (id: number | null) => void
  pushUndo: (data: any) => void
  undo: () => void
  redo: () => void
  setExpressionEditorVisible: (visible: boolean, initialValue?: string) => void
  setConditionalFormatVisible: (visible: boolean) => void
  setChartConfigVisible: (visible: boolean, chart?: ChartConfig | null) => void
  toggleDataSourceExpand: (key: string) => void
  toggleDataSetExpand: (key: string) => void
  setWritebackConfigVisible: (visible: boolean, config?: WritebackConfig | null) => void
  setWritebackConfigs: (configs: WritebackConfig[]) => void
  addWritebackConfig: (config: WritebackConfig) => void
  updateWritebackConfig: (config: WritebackConfig) => void
  removeWritebackConfig: (id: number) => void
  setCurrentWritebackConfig: (config: WritebackConfig | null) => void
  reset: () => void
}

const initialState = {
  luckysheetInstance: null,
  selectedCell: null,
  selectedRange: null,
  selectedChartId: null,
  cellValue: '',
  dataSources: [],
  conditionalFormats: [],
  charts: [],
  templateName: '未命名报表',
  templateId: null,
  undoStack: [],
  redoStack: [],
  expressionEditorVisible: false,
  expressionEditorInitialValue: '',
  conditionalFormatVisible: false,
  chartConfigVisible: false,
  editingChart: null,
  expandedDataSourceKeys: [],
  expandedDataSetKeys: [],
  writebackConfigVisible: false,
  writebackConfigs: [],
  currentWritebackConfig: null
}

export const useDesignerStore = create<DesignerState>((set, get) => ({
  ...initialState,

  setLuckysheetInstance: (instance) => set({ luckysheetInstance: instance }),
  setSelectedCell: (cell) => set({ selectedCell: cell, selectedChartId: null }),
  setSelectedRange: (range) => set({ selectedRange: range }),
  setSelectedChartId: (id) => set({ selectedChartId: id, selectedCell: null, selectedRange: null }),
  setCellValue: (value) => set({ cellValue: value }),
  setDataSources: (sources) => set({ dataSources: sources }),
  setConditionalFormats: (formats) => set({ conditionalFormats: formats }),
  setCharts: (charts) => set({ charts }),
  addChart: (chart) => set({ charts: [...get().charts, chart] }),
  updateChart: (chart) => set({
    charts: get().charts.map(c => c.id === chart.id ? chart : c)
  }),
  removeChart: (chartId) => set({
    charts: get().charts.filter(c => c.id !== chartId)
  }),
  setTemplateName: (name) => set({ templateName: name }),
  setTemplateId: (id) => set({ templateId: id }),
  pushUndo: (data) => set({
    undoStack: [...get().undoStack, data].slice(-50),
    redoStack: []
  }),
  undo: () => {
    const { undoStack, redoStack } = get()
    if (undoStack.length === 0) return
    const prev = undoStack[undoStack.length - 1]
    set({
      undoStack: undoStack.slice(0, -1),
      redoStack: [...redoStack, prev]
    })
  },
  redo: () => {
    const { undoStack, redoStack } = get()
    if (redoStack.length === 0) return
    const next = redoStack[redoStack.length - 1]
    set({
      undoStack: [...undoStack, next],
      redoStack: redoStack.slice(0, -1)
    })
  },
  setExpressionEditorVisible: (visible, initialValue = '') => set({
    expressionEditorVisible: visible,
    expressionEditorInitialValue: initialValue
  }),
  setConditionalFormatVisible: (visible) => set({ conditionalFormatVisible: visible }),
  setChartConfigVisible: (visible, chart = null) => set({
    chartConfigVisible: visible,
    editingChart: chart
  }),
  toggleDataSourceExpand: (key) => {
    const { expandedDataSourceKeys } = get()
    if (expandedDataSourceKeys.includes(key)) {
      set({ expandedDataSourceKeys: expandedDataSourceKeys.filter(k => k !== key) })
    } else {
      set({ expandedDataSourceKeys: [...expandedDataSourceKeys, key] })
    }
  },
  toggleDataSetExpand: (key) => {
    const { expandedDataSetKeys } = get()
    if (expandedDataSetKeys.includes(key)) {
      set({ expandedDataSetKeys: expandedDataSetKeys.filter(k => k !== key) })
    } else {
      set({ expandedDataSetKeys: [...expandedDataSetKeys, key] })
    }
  },
  setWritebackConfigVisible: (visible, config = null) => set({
    writebackConfigVisible: visible,
    currentWritebackConfig: config
  }),
  setWritebackConfigs: (configs) => set({ writebackConfigs: configs }),
  addWritebackConfig: (config) => set({
    writebackConfigs: [...get().writebackConfigs, config]
  }),
  updateWritebackConfig: (config) => set({
    writebackConfigs: get().writebackConfigs.map(c => c.id === config.id ? config : c)
  }),
  removeWritebackConfig: (id) => set({
    writebackConfigs: get().writebackConfigs.filter(c => c.id !== id)
  }),
  setCurrentWritebackConfig: (config) => set({ currentWritebackConfig: config }),
  reset: () => set(initialState)
}))
