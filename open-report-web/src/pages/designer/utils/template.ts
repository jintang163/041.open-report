import type { ChartConfig, ConditionalFormatRule, CellStyle, CellDataBinding } from '../store/designer'

export interface TemplateSheetCell {
  row: number
  col: number
  value?: string | number | null
  formula?: string
  expression?: string
  style?: CellStyle
  dataBinding?: CellDataBinding
  mergeInfo?: {
    rowSpan: number
    colSpan: number
  }
  custom?: any
}

export interface TemplateSheet {
  name: string
  index: number
  cells: TemplateSheetCell[]
  columnWidths: Record<number, number>
  rowHeights: Record<number, number>
  mergedCells: Record<string, { r: number; c: number; rs: number; cs: number }>
  frozen?: {
    row?: number
    column?: number
  }
}

export interface ReportTemplateData {
  version: string
  name: string
  createdAt: string
  updatedAt: string
  sheets: TemplateSheet[]
  conditionalFormats: ConditionalFormatRule[]
  charts: ChartConfig[]
  parameters: ReportParameter[]
  datasets: TemplateDataset[]
}

export interface ReportParameter {
  name: string
  label: string
  type: 'text' | 'number' | 'date' | 'select' | 'boolean'
  required: boolean
  defaultValue?: any
  options?: { label: string; value: any }[]
}

export interface TemplateDataset {
  id?: number
  name: string
  code?: string
  datasourceId?: number
  datasourceName?: string
  sql?: string
  fields?: { name: string; type: string; label?: string }[]
}

const TEMPLATE_VERSION = '1.0.0'

export const createEmptyTemplate = (name: string = '未命名报表'): ReportTemplateData => {
  const now = new Date().toISOString()
  return {
    version: TEMPLATE_VERSION,
    name,
    createdAt: now,
    updatedAt: now,
    sheets: [
      {
        name: 'Sheet1',
        index: 0,
        cells: [],
        columnWidths: {},
        rowHeights: {},
        mergedCells: {}
      }
    ],
    conditionalFormats: [],
    charts: [],
    parameters: [],
    datasets: []
  }
}

export const extractTemplateFromLuckysheet = (
  luckysheetData: any[],
  options: {
    name?: string
    conditionalFormats?: ConditionalFormatRule[]
    charts?: ChartConfig[]
    parameters?: ReportParameter[]
    datasets?: TemplateDataset[]
  } = {}
): ReportTemplateData => {
  const now = new Date().toISOString()
  const sheets: TemplateSheet[] = []

  luckysheetData.forEach((sheetData, index) => {
    const cells: TemplateSheetCell[] = []
    const celldata = sheetData.celldata || []

    celldata.forEach((cellData: any) => {
      const cell: TemplateSheetCell = {
        row: cellData.r,
        col: cellData.c,
        value: cellData.v?.v ?? null,
        formula: cellData.v?.f,
        custom: cellData.v?.custom
      }

      const style: CellStyle = {}
      const v = cellData.v || {}
      if (v.fs !== undefined) style.fontSize = v.fs
      if (v.ff !== undefined) style.fontFamily = v.ff
      if (v.bl === true) style.fontWeight = 'bold'
      if (v.it === true) style.fontStyle = 'italic'
      if (v.td === 1) style.textDecoration = 'underline'
      if (v.cl === true) style.textDecoration = 'line-through'
      if (v.fc !== undefined) style.color = v.fc
      if (v.bg !== undefined) style.backgroundColor = v.bg
      if (v.ht !== undefined) {
        const alignMap: Record<number, string> = { 0: 'left', 1: 'center', 2: 'right' }
        style.textAlign = alignMap[v.ht] as any
      }
      if (v.vt !== undefined) {
        const valignMap: Record<number, string> = { 0: 'top', 1: 'middle', 2: 'bottom' }
        style.verticalAlign = valignMap[v.vt] as any
      }
      if (v.cl === true || v.td !== undefined || Object.keys(style).length > 0) {
        cell.style = style
      }

      if (v.custom?.dataBinding) {
        cell.dataBinding = v.custom.dataBinding
      }
      if (v.custom?.expression) {
        cell.expression = v.custom.expression
      }

      if (cellData.v?.mc) {
        cell.mergeInfo = {
          rowSpan: cellData.v.mc.rs,
          colSpan: cellData.v.mc.cs
        }
      }

      cells.push(cell)
    })

    sheets.push({
      name: sheetData.name || `Sheet${index + 1}`,
      index,
      cells,
      columnWidths: sheetData.config?.columnlen || {},
      rowHeights: sheetData.config?.rowlen || {},
      mergedCells: sheetData.config?.merge || {},
      frozen: sheetData.config?.frozen
    })
  })

  return {
    version: TEMPLATE_VERSION,
    name: options.name || '未命名报表',
    createdAt: now,
    updatedAt: now,
    sheets,
    conditionalFormats: options.conditionalFormats || [],
    charts: options.charts || [],
    parameters: options.parameters || [],
    datasets: options.datasets || []
  }
}

export const templateToLuckysheetData = (template: ReportTemplateData): any[] => {
  return template.sheets.map((sheet) => {
    const celldata: any[] = []

    sheet.cells.forEach((cell) => {
      const v: any = {}

      if (cell.value !== undefined && cell.value !== null) {
        v.v = cell.value
        v.m = String(cell.value)
      }
      if (cell.formula) {
        v.f = cell.formula
      }
      if (cell.style) {
        const s = cell.style
        if (s.fontSize !== undefined) v.fs = s.fontSize
        if (s.fontFamily !== undefined) v.ff = s.fontFamily
        if (s.fontWeight === 'bold') v.bl = true
        if (s.fontStyle === 'italic') v.it = true
        if (s.textDecoration === 'underline') v.td = 1
        if (s.textDecoration === 'line-through') v.cl = true
        if (s.color !== undefined) v.fc = s.color
        if (s.backgroundColor !== undefined) v.bg = s.backgroundColor
        if (s.textAlign !== undefined) {
          const alignMap: Record<string, number> = { left: 0, center: 1, right: 2 }
          v.ht = alignMap[s.textAlign]
        }
        if (s.verticalAlign !== undefined) {
          const valignMap: Record<string, number> = { top: 0, middle: 1, bottom: 2 }
          v.vt = valignMap[s.verticalAlign]
        }
      }
      if (cell.dataBinding || cell.expression) {
        v.custom = {
          ...(v.custom || {}),
          ...(cell.dataBinding ? { dataBinding: cell.dataBinding } : {}),
          ...(cell.expression ? { expression: cell.expression } : {})
        }
      }
      if (cell.mergeInfo) {
        v.mc = {
          r: cell.row,
          c: cell.col,
          rs: cell.mergeInfo.rowSpan,
          cs: cell.mergeInfo.colSpan
        }
      }
      if (cell.custom) {
        v.custom = { ...(v.custom || {}), ...cell.custom }
      }

      celldata.push({
        r: cell.row,
        c: cell.col,
        v
      })
    })

    return {
      name: sheet.name,
      index: sheet.index,
      status: sheet.index === 0 ? 1 : 0,
      order: sheet.index,
      celldata,
      config: {
        columnlen: sheet.columnWidths,
        rowlen: sheet.rowHeights,
        merge: sheet.mergedCells,
        frozen: sheet.frozen
      },
      data: []
    }
  })
}

export const serializeTemplate = (template: ReportTemplateData): string => {
  return JSON.stringify(template, null, 2)
}

export const parseTemplate = (json: string): ReportTemplateData => {
  const parsed = JSON.parse(json)
  if (!parsed.version) {
    throw new Error('无效的报表模板：缺少版本信息')
  }
  return parsed
}

export const validateTemplate = (template: ReportTemplateData): { valid: boolean; errors: string[] } => {
  const errors: string[] = []

  if (!template.version) {
    errors.push('缺少模板版本信息')
  }
  if (!template.name) {
    errors.push('缺少模板名称')
  }
  if (!template.sheets || template.sheets.length === 0) {
    errors.push('至少需要一个工作表')
  }

  template.sheets.forEach((sheet, idx) => {
    if (!sheet.name) {
      errors.push(`工作表 ${idx + 1} 缺少名称`)
    }
    if (sheet.cells) {
      sheet.cells.forEach((cell, cellIdx) => {
        if (cell.row === undefined || cell.col === undefined) {
          errors.push(`工作表 ${sheet.name} 的第 ${cellIdx + 1} 个单元格缺少位置信息`)
        }
      })
    }
  })

  return {
    valid: errors.length === 0,
    errors
  }
}
