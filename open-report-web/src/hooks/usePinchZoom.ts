import { useState, useRef, useCallback, useEffect } from 'react'

interface UsePinchZoomOptions {
  minScale?: number
  maxScale?: number
  onScaleChange?: (scale: number) => void
}

interface UsePinchZoomResult {
  scale: number
  setScale: (scale: number) => void
  bind: {
    onTouchStart: (e: React.TouchEvent) => void
    onTouchMove: (e: React.TouchEvent) => void
    onTouchEnd: () => void
  }
}

const getDistance = (touches: React.TouchList): number => {
  const dx = touches[0].clientX - touches[1].clientX
  const dy = touches[0].clientY - touches[1].clientY
  return Math.sqrt(dx * dx + dy * dy)
}

export const usePinchZoom = (options: UsePinchZoomOptions = {}): UsePinchZoomResult => {
  const { minScale = 0.5, maxScale = 3, onScaleChange } = options
  const [scale, setScaleState] = useState(1)
  const initialDistanceRef = useRef(0)
  const initialScaleRef = useRef(1)
  const isPinchingRef = useRef(false)

  const setScale = useCallback((newScale: number) => {
    const clamped = Math.min(maxScale, Math.max(minScale, newScale))
    setScaleState(clamped)
    onScaleChange?.(clamped)
  }, [minScale, maxScale, onScaleChange])

  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      isPinchingRef.current = true
      initialDistanceRef.current = getDistance(e.touches)
      initialScaleRef.current = scale
    }
  }, [scale])

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (e.touches.length === 2 && isPinchingRef.current) {
      e.preventDefault()
      const currentDistance = getDistance(e.touches)
      if (initialDistanceRef.current > 0) {
        const ratio = currentDistance / initialDistanceRef.current
        const newScale = initialScaleRef.current * ratio
        setScale(newScale)
      }
    }
  }, [setScale])

  const handleTouchEnd = useCallback(() => {
    isPinchingRef.current = false
  }, [])

  useEffect(() => {
    const prevent = (e: TouchEvent) => {
      if (e.touches.length === 2 && isPinchingRef.current) {
        e.preventDefault()
      }
    }
    document.addEventListener('touchmove', prevent, { passive: false })
    return () => document.removeEventListener('touchmove', prevent)
  }, [])

  return {
    scale,
    setScale,
    bind: {
      onTouchStart: handleTouchStart,
      onTouchMove: handleTouchMove,
      onTouchEnd: handleTouchEnd
    }
  }
}

export default usePinchZoom
