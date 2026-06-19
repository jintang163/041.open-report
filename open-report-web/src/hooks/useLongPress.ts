import { useRef, useCallback, useEffect } from 'react'

interface UseLongPressOptions {
  onLongPress: (e: React.TouchEvent | React.MouseEvent) => void
  onPress?: (e: React.TouchEvent | React.MouseEvent) => void
  duration?: number
  moveThreshold?: number
}

interface UseLongPressResult {
  bind: {
    onTouchStart: (e: React.TouchEvent) => void
    onTouchMove: (e: React.TouchEvent) => void
    onTouchEnd: (e: React.TouchEvent) => void
    onMouseDown?: (e: React.MouseEvent) => void
    onMouseMove?: (e: React.MouseEvent) => void
    onMouseUp?: (e: React.MouseEvent) => void
    onMouseLeave?: (e: React.MouseEvent) => void
  }
}

export const useLongPress = (options: UseLongPressOptions): UseLongPressResult => {
  const { onLongPress, onPress, duration = 500, moveThreshold = 10 } = options

  const timerRef = useRef<number | null>(null)
  const startXRef = useRef(0)
  const startYRef = useRef(0)
  const triggeredRef = useRef(false)
  const lastEventRef = useRef<React.TouchEvent | React.MouseEvent | null>(null)

  const clearTimer = useCallback(() => {
    if (timerRef.current) {
      window.clearTimeout(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const startPress = useCallback((x: number, y: number, e: React.TouchEvent | React.MouseEvent) => {
    triggeredRef.current = false
    startXRef.current = x
    startYRef.current = y
    lastEventRef.current = e
    clearTimer()
    timerRef.current = window.setTimeout(() => {
      triggeredRef.current = true
      if (lastEventRef.current) {
        onLongPress(lastEventRef.current)
      }
    }, duration)
  }, [duration, onLongPress, clearTimer])

  const handleMove = useCallback((x: number, y: number) => {
    const dist = Math.sqrt(
      Math.pow(x - startXRef.current, 2) +
      Math.pow(y - startYRef.current, 2)
    )
    if (dist > moveThreshold) {
      clearTimer()
    }
  }, [moveThreshold, clearTimer])

  const endPress = useCallback(() => {
    if (!triggeredRef.current && lastEventRef.current) {
      onPress?.(lastEventRef.current)
    }
    clearTimer()
    lastEventRef.current = null
  }, [onPress, clearTimer])

  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    if (e.touches.length !== 1) return
    const touch = e.touches[0]
    startPress(touch.clientX, touch.clientY, e)
  }, [startPress])

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (e.touches.length !== 1) return
    const touch = e.touches[0]
    handleMove(touch.clientX, touch.clientY)
  }, [handleMove])

  const handleTouchEnd = useCallback((_e: React.TouchEvent) => {
    endPress()
  }, [endPress])

  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    startPress(e.clientX, e.clientY, e)
  }, [startPress])

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    handleMove(e.clientX, e.clientY)
  }, [handleMove])

  const handleMouseUp = useCallback((_e: React.MouseEvent) => {
    endPress()
  }, [endPress])

  const handleMouseLeave = useCallback((_e: React.MouseEvent) => {
    clearTimer()
  }, [clearTimer])

  useEffect(() => {
    return () => clearTimer()
  }, [clearTimer])

  return {
    bind: {
      onTouchStart: handleTouchStart,
      onTouchMove: handleTouchMove,
      onTouchEnd: handleTouchEnd,
      onMouseDown: handleMouseDown,
      onMouseMove: handleMouseMove,
      onMouseUp: handleMouseUp,
      onMouseLeave: handleMouseLeave
    }
  }
}

export default useLongPress
