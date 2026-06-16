import type { CellPosition, CellStyle } from '../store/designer'

declare global {
  interface Window {
    luckysheet: any
  }
}

export interface LuckysheetCell {
  v?: string | number | null
  m?: string
  f?: string
  ct?: { fa: string; t: string }
  bl?: boolean
  it?: boolean
  fs?: number
  ff?: string
  fc?: string
  bg?: string
  ht?: number
  vt?: number
  tr?: number
  td?: number
  tb?: string
  tl?: string
  cl?: boolean
  mc?: { r: number; c: number; rs: number; cs: number }
  custom?: any
}

export interface LuckysheetRange {
  row: number[]
  column: number[]
}

export const initLuckysheet = (containerId: string, options?: any): void => {
  if (!window.luckysheet) {
    console.error('Luckysheet is not loaded')
    return
  }

  const defaultOptions = {
    container: containerId,
    title: '报表设计器',
    lang: 'zh',
    showinfobar: false,
    showsheetbar: true,
    showstatisticBar: false,
    data: [
      {
        name: 'Sheet1',
        color: '',
        status: 1,
        order: 0,
        data: [],
        config: {},
        index: 0
      }
    ],
    hook: {
      cellMousedown: () => {},
      cellMouseup: () => {},
      cellClick: () => {},
      cellSelected: () => {},
      rangeSelect: () => {},
      cellEdit: () => {},
      cellEdited: () => {}
    }
  }

  window.luckysheet.create({
    ...defaultOptions,
    ...options
  })
}

export const destroyLuckysheet = (): void => {
  if (window.luckysheet) {
    window.luckysheet.destroy()
  }
}

export const getSelectedCell = (): CellPosition | null => {
  if (!window.luckysheet) return null

  const selection = window.luckysheet.getRange()
  if (!selection || selection.length === 0) return null

  const range = selection[0]
  return {
    row: range.row[0],
    col: range.column[0]
  }
}

export const getSelectedRange = (): { start: CellPosition; end: CellPosition } | null => {
  if (!window.luckysheet) return null

  const selection = window.luckysheet.getRange()
  if (!selection || selection.length === 0) return null

  const range = selection[0]
  return {
    start: { row: range.row[0], col: range.column[0] },
    end: { row: range.row[1], col: range.column[1] }
  }
}

export const getCellValue = (row: number, col: number, sheetIndex = 0): string | number | null => {
  if (!window.luckysheet) return null
  const cell = window.luckysheet.getCell(sheetIndex, row, col)
  return cell?.v ?? null
}

export const getCellFormula = (row: number, col: number, sheetIndex = 0): string | null => {
  if (!window.luckysheet) return null
  const cell = window.luckysheet.getCell(sheetIndex, row, col)
  return cell?.f ?? null
}

export const setCellValue = (row: number, col: number, value: string | number, sheetIndex = 0): void => {
  if (!window.luckysheet) return
  window.luckysheet.setCellValue(row, col, value, sheetIndex)
}

export const setCellFormula = (row: number, col: number, formula: string, sheetIndex = 0): void => {
  if (!window.luckysheet) return
  const cell: LuckysheetCell = window.luckysheet.getCell(sheetIndex, row, col) || {}
  cell.f = formula
  cell.v = formula
  cell.m = formula
  window.luckysheet.setCell(row, col, cell, sheetIndex)
}

export const setCellStyle = (row: number, col: number, style: Partial<CellStyle>, sheetIndex = 0): void => {
  if (!window.luckysheet) return

  const cell: LuckysheetCell = window.luckysheet.getCell(sheetIndex, row, col) || {}

  if (style.fontSize !== undefined) cell.fs = style.fontSize
  if (style.fontFamily !== undefined) cell.ff = style.fontFamily
  if (style.fontWeight !== undefined) cell.bl = style.fontWeight === 'bold'
  if (style.fontStyle !== undefined) cell.it = style.fontStyle === 'italic'
  if (style.textDecoration !== undefined) {
    cell.cl = style.textDecoration === 'line-through'
    cell.td = style.textDecoration === 'underline' ? 1 : 0
  }
  if (style.color !== undefined) cell.fc = style.color
  if (style.backgroundColor !== undefined) cell.bg = style.backgroundColor
  if (style.textAlign !== undefined) {
    const alignMap: Record<string, number> = { left: 0, center: 1, right: 2 }
    cell.ht = alignMap[style.textAlign]
  }
  if (style.verticalAlign !== undefined) {
    const valignMap: Record<string, number> = { top: 0, middle: 1, bottom: 2 }
    cell.vt = valignMap[style.verticalAlign]
  }

  window.luckysheet.setCell(row, col, cell, sheetIndex)
}

export const setRangeStyle = (range: { start: CellPosition; end: CellPosition }, style: Partial<CellStyle>, sheetIndex = 0): void => {
  if (!window.luckysheet) return

  for (let r = range.start.row; r <= range.end.row; r++) {
    for (let c = range.start.col; c <= range.end.col; c++) {
      setCellStyle(r, c, style, sheetIndex)
    }
  }
  window.luckysheet.jfrefreshgrid()
}

export const mergeCells = (range: { start: CellPosition; end: CellPosition }, sheetIndex = 0): void => {
  if (!window.luckysheet) return

  const config = window.luckysheet.getConfig(sheetIndex) || {}
  const mergeData = config.merge || {}

  const key = `${range.start.row}_${range.start.col}`
  mergeData[key] = {
    r: range.start.row,
    c: range.start.col,
    rs: range.end.row - range.start.row + 1,
    cs: range.end.col - range.start.col + 1
  }

  window.luckysheet.setConfig({ merge: mergeData }, sheetIndex)
  window.luckysheet.jfrefreshgrid()
}

export const unmergeCells = (row: number, col: number, sheetIndex = 0): void => {
  if (!window.luckysheet) return

  const config = window.luckysheet.getConfig(sheetIndex) || {}
  const mergeData = config.merge || {}

  delete mergeData[`${row}_${col}`]

  window.luckysheet.setConfig({ merge: mergeData }, sheetIndex)
  window.luckysheet.jfrefreshgrid()
}

export const setBorder = (
  range: { start: CellPosition; end: CellPosition },
  borderType: 'all' | 'top' | 'bottom' | 'left' | 'right' | 'outer' | 'inner' | 'none',
  style: string = '1px solid #000',
  sheetIndex = 0
): void => {
  if (!window.luckysheet) return

  const borderConfig: Record<string, any> = {
    rangeType: 'range',
    borderType,
    style: '1',
    color: '#000',
    range: [
      {
        row: [range.start.row, range.end.row],
        column: [range.start.col, range.end.col]
      }
    ]
  }

  window.luckysheet.setBorder(borderConfig, sheetIndex)
  window.luckysheet.jfrefreshgrid()
}

export const getSheetData = (sheetIndex = 0): any => {
  if (!window.luckysheet) return null
  return window.luckysheet.getSheet(sheetIndex)
}

export const getAllSheetsData = (): any[] => {
  if (!window.luckysheet) return []
  return window.luckysheet.getSheet()
}

export const setSheetData = (data: any[], sheetIndex = 0): void => {
  if (!window.luckysheet) return
  window.luckysheet.setSheet(data, sheetIndex)
  window.luckysheet.jfrefreshgrid()
}

export const undo = (): void => {
  if (!window.luckysheet) return
  window.luckysheet.undo()
}

export const redo = (): void => {
  if (!window.luckysheet) return
  window.luckysheet.redo()
}

export const setColumnWidth = (colIndex: number, width: number, sheetIndex = 0): void => {
  if (!window.luckysheet) return
  const config = window.luckysheet.getConfig(sheetIndex) || {}
  const columnlen = config.columnlen || {}
  columnlen[colIndex] = width
  window.luckysheet.setConfig({ columnlen }, sheetIndex)
  window.luckysheet.jfrefreshgrid()
}

export const setRowHeight = (rowIndex: number, height: number, sheetIndex = 0): void => {
  if (!window.luckysheet) return
  const config = window.luckysheet.getConfig(sheetIndex) || {}
  const rowlen = config.rowlen || {}
  rowlen[rowIndex] = height
  window.luckysheet.setConfig({ rowlen }, sheetIndex)
  window.luckysheet.jfrefreshgrid()
}

export const refreshLuckysheet = (): void => {
  if (!window.luckysheet) return
  window.luckysheet.jfrefreshgrid()
}

export const cellPositionToRef = (row: number, col: number): string => {
  const colLetter = String.fromCharCode(65 + col)
  return `${colLetter}${row + 1}`
}

export const refToCellPosition = (ref: string): CellPosition | null => {
  const match = ref.match(/^([A-Z]+)(\d+)$/)
  if (!match) return null

  const [, colStr, rowStr] = match
  let col = 0
  for (let i = 0; i < colStr.length; i++) {
    col = col * 26 + (colStr.charCodeAt(i) - 64)
  }
  col -= 1

  return {
    row: parseInt(rowStr, 10) - 1,
    col
  }
}

export const setCellCustomData = (row: number, col: number, data: any, sheetIndex = 0): void => {
  if (!window.luckysheet) return
  const cell: LuckysheetCell = window.luckysheet.getCell(sheetIndex, row, col) || {}
  cell.custom = { ...(cell.custom || {}), ...data }
  window.luckysheet.setCell(row, col, cell, sheetIndex)
}

export const getCellCustomData = (row: number, col: number, sheetIndex = 0): any => {
  if (!window.luckysheet) return null
  const cell: LuckysheetCell = window.luckysheet.getCell(sheetIndex, row, col) || {}
  return cell.custom || null
}
