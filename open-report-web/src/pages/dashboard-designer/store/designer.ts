import { create } from 'zustand'
import type { ChartDashboard, ChartDashboardItem, ChartType } from '../../../types'

export interface ChartPaletteItem {
  type: ChartType
  label: string
  icon: string
  defaultWidth: number
  defaultHeight: number
}

export const CHART_PALETTE: ChartPaletteItem[] = [
  { type: 'bar', label: '柱状图', icon: '📊', defaultWidth: 450, defaultHeight: 320 },
  { type: 'line', label: '折线图', icon: '📈', defaultWidth: 450, defaultHeight: 320 },
  { type: 'pie', label: '饼图', icon: '🥧', defaultWidth: 400, defaultHeight: 350 },
  { type: 'radar', label: '雷达图', icon: '🕸️', defaultWidth: 400, defaultHeight: 350 },
  { type: 'scatter', label: '散点图', icon: '⚪', defaultWidth: 450, defaultHeight: 320 }
]

export interface DashboardDesignerState {
  dashboard: ChartDashboard | null
  items: ChartDashboardItem[]
  selectedItemId: number | null
  draggingType: ChartType | null
  isDirty: boolean
  configModalVisible: boolean
  editingItem: ChartDashboardItem | null
  scale: number

  setDashboard: (dashboard: ChartDashboard) => void
  setItems: (items: ChartDashboardItem[]) => void
  addItem: (item: ChartDashboardItem) => void
  updateItem: (id: number, updates: Partial<ChartDashboardItem>) => void
  removeItem: (id: number) => void
  setSelectedItemId: (id: number | null) => void
  setDraggingType: (type: ChartType | null) => void
  setIsDirty: (dirty: boolean) => void
  setConfigModalVisible: (visible: boolean, item?: ChartDashboardItem | null) => void
  setScale: (scale: number) => void
  moveItem: (id: number, positionX: number, positionY: number) => void
  resizeItem: (id: number, width: number, height: number) => void
  reset: () => void
}

const initialState = {
  dashboard: null,
  items: [],
  selectedItemId: null,
  draggingType: null,
  isDirty: false,
  configModalVisible: false,
  editingItem: null,
  scale: 1
}

export const useDashboardDesignerStore = create<DashboardDesignerState>((set, get) => ({
  ...initialState,

  setDashboard: (dashboard) => set({ dashboard }),
  setItems: (items) => set({ items, isDirty: true }),
  addItem: (item) => set({ items: [...get().items, item], isDirty: true }),
  updateItem: (id, updates) => set({
    items: get().items.map(i => i.id === id ? { ...i, ...updates } : i),
    isDirty: true
  }),
  removeItem: (id) => set({
    items: get().items.filter(i => i.id !== id),
    selectedItemId: get().selectedItemId === id ? null : get().selectedItemId,
    isDirty: true
  }),
  setSelectedItemId: (id) => set({ selectedItemId: id }),
  setDraggingType: (type) => set({ draggingType: type }),
  setIsDirty: (dirty) => set({ isDirty: dirty }),
  setConfigModalVisible: (visible, item = null) => set({
    configModalVisible: visible,
    editingItem: item
  }),
  setScale: (scale) => set({ scale }),
  moveItem: (id, positionX, positionY) => set({
    items: get().items.map(i => i.id === id ? { ...i, positionX, positionY } : i),
    isDirty: true
  }),
  resizeItem: (id, width, height) => set({
    items: get().items.map(i => i.id === id ? { ...i, width, height } : i),
    isDirty: true
  }),
  reset: () => set(initialState)
}))
