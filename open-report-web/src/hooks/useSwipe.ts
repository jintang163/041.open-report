import { useRef, useCallback } from 'react'

interface UseSwipeOptions {
  onSwipeLeft?: () => void
  onSwipeRight?: () => void
  onSwipeUp?: () => void
  onSwipeDown?: () => void
  threshold?: number
  preventDefault?: boolean
}

interface UseSwipeResult {
  bind: {
    onTouchStart: (e: React.TouchEvent) => void
    onTouchMove: (e: React.TouchEvent) => void
    onTouchEnd: (e: React.TouchEvent) => void
  }
}

export const useSwipe = (options: UseSwipeOptions = {}): UseSwipeResult => {
  const {
    onSwipeLeft,
    onSwipeRight,
    onSwipeUp,
    onSwipeDown,
    threshold = 50,
    preventDefault = true
  } = options

  const startXRef = useRef(0)
  const startYRef = useRef(0)
  const endXRef = useRef(0)
  const endYRef = useRef(0)

  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    if (e.touches.length !== 1) return
    const touch = e.touches[0]
    startXRef.current = touch.clientX
    startYRef.current = touch.clientY
    endXRef.current = touch.clientX
    endYRef.current = touch.clientY
  }, [])

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (e.touches.length !== 1) return
    if (preventDefault) {
      e.preventDefault()
    }
    const touch = e.touches[0]
    endXRef.current = touch.clientX
    endYRef.current = touch.clientY
  }, [preventDefault])

  const handleTouchEnd = useCallback((_e: React.TouchEvent) => {
    const deltaX = endXRef.current - startXRef.current
    const deltaY = endYRef.current - startYRef.current
    const absX = Math.abs(deltaX)
    const absY = Math.abs(deltaY)

    if (absX < threshold && absY < threshold) {
      return
    }

    if (absX > absY) {
      if (deltaX > 0) {
        onSwipeRight?.()
      } else {
        onSwipeLeft?.()
      }
    } else {
      if (deltaY > 0) {
        onSwipeDown?.()
      } else {
        onSwipeUp?.()
      }
    }
  }, [threshold, onSwipeLeft, onSwipeRight, onSwipeUp, onSwipeDown])

  return {
    bind: {
      onTouchStart: handleTouchStart,
      onTouchMove: handleTouchMove,
      onTouchEnd: handleTouchEnd
    }
  }
}

export default useSwipe
