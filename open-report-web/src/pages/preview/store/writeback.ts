import { create } from 'zustand'
import type {
  WritebackConfig,
  WritebackField,
  EditableRowData,
  RowStatus,
  CellDataChange,
  DataSubmitResult
} from '@/types'
import { getWritebackConfigList, submitReportData } from '@/api/writeback'
import { message } from 'antd'

interface WritebackState {
  writebackConfigs: WritebackConfig[]
  activeConfig: WritebackConfig | null
  editableRows: EditableRowData[]
  editingCell: { rowIndex: number; fieldName: string } | null
  submitting: boolean
  submitResult: DataSubmitResult | null
  historyVisible: boolean

  setWritebackConfigs: (configs: WritebackConfig[]) => void
  setActiveConfig: (config: WritebackConfig | null) => void
  setEditableRows: (rows: EditableRowData[]) => void
  addEditableRow: (rowData: Record<string, any>) => void
  updateCellValue: (rowIndex: number, fieldName: string, value: any) => void
  updateRowStatus: (rowIndex: number, status: RowStatus) => void
  markRowForDelete: (rowIndex: number) => void
  setEditingCell: (cell: { rowIndex: number; fieldName: string } | null) => void
  setSubmitting: (submitting: boolean) => void
  setSubmitResult: (result: DataSubmitResult | null) => void
  setHistoryVisible: (visible: boolean) => void

  loadWritebackConfigs: (reportId: number) => Promise<void>
  initEditableRows: (data: Record<string, any>[]) => void
  getDirtyRows: () => CellDataChange[]
  submitData: (reportId: number, params?: Record<string, any>) => Promise<DataSubmitResult | null>
  reset: () => void
}

export const useWritebackStore = create<WritebackState>((set, get) => ({
  writebackConfigs: [],
  activeConfig: null,
  editableRows: [],
  editingCell: null,
  submitting: false,
  submitResult: null,
  historyVisible: false,

  setWritebackConfigs: (configs) => set({ writebackConfigs: configs }),
  setActiveConfig: (config) => set({ activeConfig: config }),
  setEditableRows: (rows) => set({ editableRows: rows }),
  setEditingCell: (cell) => set({ editingCell: cell }),
  setSubmitting: (submitting) => set({ submitting }),
  setSubmitResult: (result) => set({ submitResult: result }),
  setHistoryVisible: (visible) => set({ historyVisible: visible }),

  addEditableRow: (rowData) => {
    const { activeConfig, editableRows } = get()
    if (!activeConfig) return

    const cells: Record<string, any> = {}
    activeConfig.fields?.forEach((field: WritebackField) => {
      cells[field.fieldName] = {
        editable: field.editable === 1,
        fieldName: field.fieldName,
        fieldType: field.fieldType,
        required: field.required === 1,
        validationRule: field.validationRule,
        validationMessage: field.validationMessage,
        cellPosition: field.cellPosition
      }
    })

    const newRow: EditableRowData = {
      rowIndex: -1,
      rowStatus: 'INSERT',
      originalData: {},
      currentData: { ...rowData },
      cells,
      dirty: true
    }

    set({ editableRows: [...editableRows, newRow] })
  },

  updateCellValue: (rowIndex, fieldName, value) => {
    set((state) => {
      const newRows = [...state.editableRows]
      const row = newRows.find(r => r.rowIndex === rowIndex)
      if (row) {
        row.currentData[fieldName] = value
        row.dirty = true
        if (row.rowStatus === 'UPDATE' || row.rowStatus === 'INSERT') {
          row.rowStatus = row.rowIndex < 0 ? 'INSERT' : 'UPDATE'
        }
      }
      return { editableRows: newRows }
    })
  },

  updateRowStatus: (rowIndex, status) => {
    set((state) => {
      const newRows = [...state.editableRows]
      const row = newRows.find(r => r.rowIndex === rowIndex)
      if (row) {
        row.rowStatus = status
        row.dirty = true
      }
      return { editableRows: newRows }
    })
  },

  markRowForDelete: (rowIndex) => {
    set((state) => {
      const newRows = [...state.editableRows]
      const row = newRows.find(r => r.rowIndex === rowIndex)
      if (row) {
        if (row.rowStatus === 'INSERT') {
          return { editableRows: newRows.filter(r => r.rowIndex !== rowIndex) }
        }
        row.rowStatus = 'DELETE'
        row.dirty = true
      }
      return { editableRows: newRows }
    })
  },

  loadWritebackConfigs: async (reportId) => {
    try {
      const configs = await getWritebackConfigList(reportId)
      set({
        writebackConfigs: configs,
        activeConfig: configs.length > 0 ? configs[0] : null
      })
    } catch (error: any) {
      console.error('加载回写配置失败:', error)
      message.error(error.message || '加载回写配置失败')
    }
  },

  initEditableRows: (data) => {
    const { activeConfig } = get()
    if (!activeConfig) {
      set({ editableRows: [] })
      return
    }

    const cells: Record<string, any> = {}
    activeConfig.fields?.forEach((field: WritebackField) => {
      cells[field.fieldName] = {
        editable: field.editable === 1,
        fieldName: field.fieldName,
        fieldType: field.fieldType,
        required: field.required === 1,
        validationRule: field.validationRule,
        validationMessage: field.validationMessage,
        cellPosition: field.cellPosition
      }
    })

    const rows: EditableRowData[] = data.map((item, index) => ({
      rowIndex: index,
      rowStatus: 'UPDATE' as RowStatus,
      originalData: { ...item },
      currentData: { ...item },
      cells: { ...cells },
      dirty: false
    }))

    set({ editableRows: rows })
  },

  getDirtyRows: () => {
    const { editableRows, activeConfig } = get()
    const changes: CellDataChange[] = []

    editableRows.forEach(row => {
      if (!row.dirty && row.rowStatus !== 'DELETE') return

      const cellValues: Record<string, string> = {}
      if (activeConfig?.fields) {
        activeConfig.fields.forEach(field => {
          if (row.cells[field.fieldName]) {
            const value = row.currentData[field.fieldName]
            if (value !== undefined && value !== null) {
              cellValues[field.cellPosition] = String(value)
            }
          }
        })
      }

      changes.push({
        rowIndex: row.rowIndex,
        rowStatus: row.rowStatus,
        oldData: row.rowStatus === 'UPDATE' ? row.originalData : undefined,
        newData: row.rowStatus !== 'DELETE' ? row.currentData : undefined,
        cellValues
      })
    })

    return changes
  },

  submitData: async (reportId, params) => {
    const { activeConfig, getDirtyRows } = get()
    if (!activeConfig) {
      message.error('请先配置回写规则')
      return null
    }

    const changes = getDirtyRows()
    if (changes.length === 0) {
      message.warning('没有需要提交的数据')
      return null
    }

    set({ submitting: true })
    try {
      const result = await submitReportData({
        reportId,
        configId: activeConfig.id,
        params,
        changes
      })

      set({ submitResult: result })

      if (result.status === 'SUCCESS') {
        message.success(`提交成功：共 ${result.totalCount} 条，成功 ${result.successCount} 条`)
        set((state) => ({
          editableRows: state.editableRows.map(row => ({
            ...row,
            dirty: false,
            originalData: row.rowStatus === 'INSERT' ? { ...row.currentData } : row.originalData,
            rowStatus: row.rowStatus === 'DELETE' ? 'DELETE' : (row.rowIndex < 0 ? 'UPDATE' : row.rowStatus)
          })).filter(row => row.rowStatus !== 'DELETE')
        }))
      } else if (result.status === 'PARTIAL') {
        message.warning(`部分成功：共 ${result.totalCount} 条，成功 ${result.successCount} 条，失败 ${result.failCount} 条`)
      } else {
        message.error(`提交失败：${result.errorMsg || '未知错误'}`)
      }

      return result
    } catch (error: any) {
      console.error('提交数据失败:', error)
      message.error(error.message || '提交数据失败')
      return null
    } finally {
      set({ submitting: false })
    }
  },

  reset: () => {
    set({
      writebackConfigs: [],
      activeConfig: null,
      editableRows: [],
      editingCell: null,
      submitting: false,
      submitResult: null,
      historyVisible: false
    })
  }
}))
