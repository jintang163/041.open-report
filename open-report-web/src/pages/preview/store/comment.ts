import { create } from 'zustand'
import type { ReportComment } from '@/types'
import {
  getCommentsByTemplateId,
  getCommentsByCellRef,
  getCommentsByChartId,
  getCellRefsWithComments,
  getChartIdsWithComments,
  getCellRefsWithCommentsByVersion,
  getChartIdsWithCommentsByVersion,
  getCommentCount,
  addComment,
  addReply,
  deleteComment,
  toggleCommentLike
} from '@/api/comment'

interface CommentState {
  comments: ReportComment[]
  cellComments: ReportComment[]
  chartComments: ReportComment[]
  cellRefs: string[]
  chartIds: string[]
  cellCommentCounts: Record<string, number>
  chartCommentCounts: Record<string, number>
  totalCount: number
  loading: boolean

  selectedCellRef: string | null
  selectedChartId: string | null
  snapshotVersion: number | null

  loadComments: (templateId: number, snapshotVersion?: number) => Promise<void>
  loadCellComments: (templateId: number, cellRef: string) => Promise<void>
  loadChartComments: (templateId: number, chartId: string) => Promise<void>
  loadCellRefs: (templateId: number) => Promise<void>
  loadChartIds: (templateId: number) => Promise<void>
  loadCellRefsByVersion: (templateId: number, snapshotVersion: number) => Promise<void>
  loadChartIdsByVersion: (templateId: number, snapshotVersion: number) => Promise<void>
  loadCommentCount: (templateId: number) => Promise<void>

  getCellCommentCount: (cellRef: string) => number
  getChartCommentCount: (chartId: string) => number

  createComment: (data: Partial<ReportComment>) => Promise<ReportComment>
  createReply: (parentId: number, data: Partial<ReportComment>) => Promise<ReportComment>
  removeComment: (commentId: number) => Promise<void>
  toggleLike: (commentId: number) => Promise<boolean>

  setSelectedCellRef: (cellRef: string | null) => void
  setSelectedChartId: (chartId: string | null) => void
  setSnapshotVersion: (version: number | null) => void
  reset: () => void
}

export const useCommentStore = create<CommentState>((set, get) => ({
  comments: [],
  cellComments: [],
  chartComments: [],
  cellRefs: [],
  chartIds: [],
  cellCommentCounts: {},
  chartCommentCounts: {},
  totalCount: 0,
  loading: false,

  selectedCellRef: null,
  selectedChartId: null,
  snapshotVersion: null,

  loadComments: async (templateId: number, snapshotVersion?: number) => {
    set({ loading: true, snapshotVersion: snapshotVersion || null })
    try {
      const comments = await getCommentsByTemplateId(templateId, snapshotVersion)

      const cellCounts: Record<string, number> = {}
      const chartCounts: Record<string, number> = {}
      const countComments = (commentList: ReportComment[]) => {
        commentList.forEach((c) => {
          if (c.cellRef) {
            cellCounts[c.cellRef] = (cellCounts[c.cellRef] || 0) + 1 + (c.replies?.length || 0)
          }
          if (c.chartId) {
            chartCounts[c.chartId] = (chartCounts[c.chartId] || 0) + 1 + (c.replies?.length || 0)
          }
          if (c.replies) {
            countComments(c.replies)
          }
        })
      }
      countComments(comments)

      set({ comments, cellCommentCounts: cellCounts, chartCommentCounts: chartCounts })
    } catch (error) {
      console.error('加载评论失败:', error)
    } finally {
      set({ loading: false })
    }
  },

  loadCellComments: async (templateId: number, cellRef: string) => {
    set({ loading: true, selectedCellRef: cellRef })
    try {
      const cellComments = await getCommentsByCellRef(templateId, cellRef)
      set({ cellComments })
    } catch (error) {
      console.error('加载单元格评论失败:', error)
    } finally {
      set({ loading: false })
    }
  },

  loadChartComments: async (templateId: number, chartId: string) => {
    set({ loading: true, selectedChartId: chartId })
    try {
      const chartComments = await getCommentsByChartId(templateId, chartId)
      set({ chartComments })
    } catch (error) {
      console.error('加载图表评论失败:', error)
    } finally {
      set({ loading: false })
    }
  },

  loadCellRefs: async (templateId: number) => {
    try {
      const cellRefs = await getCellRefsWithComments(templateId)
      set({ cellRefs })
    } catch (error) {
      console.error('加载评论单元格引用失败:', error)
    }
  },

  loadChartIds: async (templateId: number) => {
    try {
      const chartIds = await getChartIdsWithComments(templateId)
      set({ chartIds })
    } catch (error) {
      console.error('加载评论图表ID失败:', error)
    }
  },

  loadCellRefsByVersion: async (templateId: number, snapshotVersion: number) => {
    try {
      const cellRefs = await getCellRefsWithCommentsByVersion(templateId, snapshotVersion)
      set({ cellRefs, snapshotVersion })
    } catch (error) {
      console.error('按版本加载评论单元格引用失败:', error)
    }
  },

  loadChartIdsByVersion: async (templateId: number, snapshotVersion: number) => {
    try {
      const chartIds = await getChartIdsWithCommentsByVersion(templateId, snapshotVersion)
      set({ chartIds, snapshotVersion })
    } catch (error) {
      console.error('按版本加载评论图表ID失败:', error)
    }
  },

  loadCommentCount: async (templateId: number) => {
    try {
      const totalCount = await getCommentCount(templateId)
      set({ totalCount })
    } catch (error) {
      console.error('加载评论数量失败:', error)
    }
  },

  getCellCommentCount: (cellRef: string) => {
    return get().cellCommentCounts[cellRef] || 0
  },

  getChartCommentCount: (chartId: string) => {
    return get().chartCommentCounts[chartId] || 0
  },

  createComment: async (data: Partial<ReportComment>) => {
    const comment = await addComment(data)
    set((state) => {
      const newCellCounts = { ...state.cellCommentCounts }
      const newChartCounts = { ...state.chartCommentCounts }
      if (data.cellRef) {
        newCellCounts[data.cellRef] = (newCellCounts[data.cellRef] || 0) + 1
      }
      if (data.chartId) {
        newChartCounts[data.chartId] = (newChartCounts[data.chartId] || 0) + 1
      }
      return {
        comments: [comment, ...state.comments],
        totalCount: state.totalCount + 1,
        cellRefs: data.cellRef && !state.cellRefs.includes(data.cellRef)
          ? [...state.cellRefs, data.cellRef]
          : state.cellRefs,
        chartIds: data.chartId && !state.chartIds.includes(data.chartId)
          ? [...state.chartIds, data.chartId]
          : state.chartIds,
        cellCommentCounts: newCellCounts,
        chartCommentCounts: newChartCounts
      }
    })
    return comment
  },

  createReply: async (parentId: number, data: Partial<ReportComment>) => {
    const reply = await addReply(parentId, data)
    set((state) => {
      const updateReplies = (comments: ReportComment[]): ReportComment[] => {
        return comments.map((c) => {
          if (c.id === parentId) {
            const newCellCounts = { ...state.cellCommentCounts }
            const newChartCounts = { ...state.chartCommentCounts }
            if (c.cellRef) {
              newCellCounts[c.cellRef] = (newCellCounts[c.cellRef] || 0) + 1
            }
            if (c.chartId) {
              newChartCounts[c.chartId] = (newChartCounts[c.chartId] || 0) + 1
            }
            set({ cellCommentCounts: newCellCounts, chartCommentCounts: newChartCounts })
            return {
              ...c,
              replyCount: (c.replyCount || 0) + 1,
              replies: [...(c.replies || []), reply]
            }
          }
          return c
        })
      }
      return {
        comments: updateReplies(state.comments),
        cellComments: updateReplies(state.cellComments),
        chartComments: updateReplies(state.chartComments),
        totalCount: state.totalCount + 1
      }
    })
    return reply
  },

  removeComment: async (commentId: number) => {
    const commentToRemove = get().comments.find((c) => c.id === commentId)
    const repliesCount = commentToRemove?.replies?.length || 0
    const totalToRemove = 1 + repliesCount
    const cellRef = commentToRemove?.cellRef
    const chartId = commentToRemove?.chartId

    await deleteComment(commentId)
    set((state) => {
      const filterComments = (comments: ReportComment[]): ReportComment[] => {
        return comments
          .filter((c) => c.id !== commentId)
          .map((c) => ({
            ...c,
            replies: c.replies?.filter((r) => r.id !== commentId) || []
          }))
      }

      const newCellCounts = { ...state.cellCommentCounts }
      const newChartCounts = { ...state.chartCommentCounts }
      if (cellRef && newCellCounts[cellRef]) {
        newCellCounts[cellRef] = Math.max(0, newCellCounts[cellRef] - totalToRemove)
      }
      if (chartId && newChartCounts[chartId]) {
        newChartCounts[chartId] = Math.max(0, newChartCounts[chartId] - totalToRemove)
      }

      const newCellRefs = newCellCounts[cellRef || ''] > 0
        ? state.cellRefs
        : state.cellRefs.filter((r) => r !== cellRef)
      const newChartIds = newChartCounts[chartId || ''] > 0
        ? state.chartIds
        : state.chartIds.filter((r) => r !== chartId)

      return {
        comments: filterComments(state.comments),
        cellComments: filterComments(state.cellComments),
        chartComments: filterComments(state.chartComments),
        totalCount: Math.max(0, state.totalCount - totalToRemove),
        cellCommentCounts: newCellCounts,
        chartCommentCounts: newChartCounts,
        cellRefs: newCellRefs,
        chartIds: newChartIds
      }
    })
  },

  toggleLike: async (commentId: number) => {
    const liked = await toggleCommentLike(commentId)
    const updateLike = (comments: ReportComment[]): ReportComment[] => {
      return comments.map((c) => {
        if (c.id === commentId) {
          return {
            ...c,
            liked,
            likeCount: liked ? (c.likeCount || 0) + 1 : Math.max(0, (c.likeCount || 0) - 1)
          }
        }
        if (c.replies) {
          return {
            ...c,
            replies: updateLike(c.replies)
          }
        }
        return c
      })
    }
    set((state) => ({
      comments: updateLike(state.comments),
      cellComments: updateLike(state.cellComments),
      chartComments: updateLike(state.chartComments)
    }))
    return liked
  },

  setSelectedCellRef: (cellRef: string | null) => set({ selectedCellRef: cellRef }),
  setSelectedChartId: (chartId: string | null) => set({ selectedChartId: chartId }),
  setSnapshotVersion: (version: number | null) => set({ snapshotVersion: version }),

  reset: () => {
    set({
      comments: [],
      cellComments: [],
      chartComments: [],
      cellRefs: [],
      chartIds: [],
      cellCommentCounts: {},
      chartCommentCounts: {},
      totalCount: 0,
      loading: false,
      selectedCellRef: null,
      selectedChartId: null,
      snapshotVersion: null
    })
  }
}))
