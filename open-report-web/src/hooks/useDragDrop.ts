import { useState, useCallback } from 'react'
import type { PivotFieldType } from '@/types'

export interface DragItem {
  fieldName: string
  displayName?: string
  sourceZone?: string
}

export interface UseDragDropReturn {
  draggedItem: DragItem | null
  setDraggedItem: (item: DragItem | null) => void
  handleDragStart: (item: DragItem, e: React.DragEvent) => void
  handleDrop: (targetZone: PivotFieldType, e: React.DragEvent, onDrop?: (item: DragItem, targetZone: PivotFieldType) => void) => void
  handleDragOver: (e: React.DragEvent) => void
}

export const useDragDrop = (): UseDragDropReturn => {
  const [draggedItem, setDraggedItem] = useState<DragItem | null>(null)

  const handleDragStart = useCallback((item: DragItem, e: React.DragEvent) => {
    setDraggedItem(item)
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', JSON.stringify(item))
  }, [])

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }, [])

  const handleDrop = useCallback(
    (targetZone: PivotFieldType, e: React.DragEvent, onDrop?: (item: DragItem, targetZone: PivotFieldType) => void) => {
      e.preventDefault()
      if (draggedItem) {
        if (onDrop) {
          onDrop(draggedItem, targetZone)
        }
        setDraggedItem(null)
      }
    },
    [draggedItem]
  )

  return {
    draggedItem,
    setDraggedItem,
    handleDragStart,
    handleDrop,
    handleDragOver
  }
}

export default useDragDrop
