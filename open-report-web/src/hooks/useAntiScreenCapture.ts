import { useEffect, useRef } from 'react'

interface AntiScreenCaptureOptions {
  disableContextMenu?: boolean
  disableTextSelection?: boolean
  disableCopy?: boolean
  disablePrint?: boolean
  disableScreenshotKeys?: boolean
  enableScreenshotDetection?: boolean
  enableBlurOnVisibilityChange?: boolean
  customBlurStyle?: string
}

const useAntiScreenCapture = (options: AntiScreenCaptureOptions = {}) => {
  const {
    disableContextMenu = true,
    disableTextSelection = true,
    disableCopy = true,
    disablePrint = true,
    disableScreenshotKeys = true,
    enableScreenshotDetection = true,
    enableBlurOnVisibilityChange = true,
    customBlurStyle
  } = options

  const blurOverlayRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const preventDefault = (e: Event) => {
      e.preventDefault()
      return false
    }

    const handleContextMenu = (e: MouseEvent) => {
      if (disableContextMenu) {
        e.preventDefault()
        return false
      }
    }

    const handleCopy = (e: ClipboardEvent) => {
      if (disableCopy) {
        e.preventDefault()
        return false
      }
    }

    const handleKeyDown = (e: KeyboardEvent) => {
      const key = e.key.toLowerCase()

      if (disableCopy && (e.ctrlKey || e.metaKey) && key === 'c') {
        e.preventDefault()
        return false
      }

      if (disableCopy && (e.ctrlKey || e.metaKey) && key === 'x') {
        e.preventDefault()
        return false
      }

      if (disablePrint && (e.ctrlKey || e.metaKey) && key === 'p') {
        e.preventDefault()
        return false
      }

      if (disableScreenshotKeys) {
        if (e.key === 'PrintScreen' || key === 'printscreen') {
          triggerScreenBlur()
          setTimeout(removeScreenBlur, 200)
          e.preventDefault()
          return false
        }

        if ((e.altKey || e.metaKey) && key === 'printscreen') {
          triggerScreenBlur()
          setTimeout(removeScreenBlur, 200)
          e.preventDefault()
          return false
        }

        if ((e.shiftKey || e.metaKey) && (e.metaKey || e.shiftKey) && e.key === '3') {
          triggerScreenBlur()
          setTimeout(removeScreenBlur, 200)
          e.preventDefault()
          return false
        }

        if ((e.shiftKey || e.metaKey) && (e.metaKey || e.shiftKey) && e.key === '4') {
          triggerScreenBlur()
          setTimeout(removeScreenBlur, 200)
          e.preventDefault()
          return false
        }

        if ((e.shiftKey || e.metaKey) && (e.metaKey || e.shiftKey) && e.key === '5') {
          triggerScreenBlur()
          setTimeout(removeScreenBlur, 200)
          e.preventDefault()
          return false
        }
      }

      if ((e.ctrlKey || e.metaKey) && (e.shiftKey || e.metaKey) && key === 'i') {
        e.preventDefault()
        return false
      }

      if ((e.ctrlKey || e.metaKey) && (e.shiftKey || e.metaKey) && key === 'j') {
        e.preventDefault()
        return false
      }

      if ((e.ctrlKey || e.metaKey) && (e.shiftKey || e.metaKey) && key === 'c') {
        e.preventDefault()
        return false
      }

      if (key === 'f12') {
        e.preventDefault()
        return false
      }
    }

    const triggerScreenBlur = () => {
      if (!blurOverlayRef.current) {
        const overlay = document.createElement('div')
        overlay.className = 'anti-screenshot-blur-overlay'
        overlay.style.cssText = customBlurStyle || `
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          z-index: 2147483647;
          background: rgba(255, 255, 255, 0.95);
          backdrop-filter: blur(30px);
          -webkit-backdrop-filter: blur(30px);
          pointer-events: none;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: opacity 0.1s ease;
        `
        const text = document.createElement('div')
        text.innerHTML = `
          <div style="text-align:center;">
            <div style="font-size: 48px; margin-bottom: 16px;">📛</div>
            <div style="font-size: 24px; font-weight: bold; color: #666;">内容受版权保护</div>
            <div style="font-size: 14px; color: #999; margin-top: 8px;">禁止截屏 / 内容已加密</div>
          </div>
        `
        overlay.appendChild(text)
        document.body.appendChild(overlay)
        blurOverlayRef.current = overlay
      }
      blurOverlayRef.current.style.opacity = '1'
      blurOverlayRef.current.style.display = 'flex'
    }

    const removeScreenBlur = () => {
      if (blurOverlayRef.current) {
        blurOverlayRef.current.style.opacity = '0'
        setTimeout(() => {
          if (blurOverlayRef.current) {
            blurOverlayRef.current.style.display = 'none'
          }
        }, 100)
      }
    }

    const handleVisibilityChange = () => {
      if (enableBlurOnVisibilityChange) {
        if (document.hidden) {
          triggerScreenBlur()
        } else {
          setTimeout(removeScreenBlur, 300)
        }
      }
    }

    const handleMouseDown = (e: MouseEvent) => {
      if (disableTextSelection && e.button === 0) {
        const target = e.target as HTMLElement
        const isInput =
          target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.isContentEditable
        if (!isInput && !target.closest('.ant-modal') && !target.closest('.ant-message') && !target.closest('.ant-notification')) {
          if (!window.getSelection()?.toString()) {
            setTimeout(() => {
              window.getSelection()?.removeAllRanges()
            }, 10)
          }
        }
      }
    }

    const handleSelectStart = (e: Event) => {
      if (disableTextSelection) {
        const target = e.target as HTMLElement
        const isInput =
          target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.isContentEditable
        if (!isInput && !target.closest('.ant-modal') && !target.closest('.ant-message') && !target.closest('.ant-notification')) {
          e.preventDefault()
          return false
        }
      }
    }

    const handleDragStart = (e: DragEvent) => {
      if (disableCopy) {
        e.preventDefault()
        return false
      }
    }

    const detectScreenshot = () => {
      if (!enableScreenshotDetection) return

      const checkSize = () => {
        const w = window.outerWidth - window.innerWidth
        const h = window.outerHeight - window.innerHeight
        if (w > 100 || h > 100) {
          triggerScreenBlur()
        }
      }

      setInterval(checkSize, 500)
    }

    const addNoSelectStyle = () => {
      if (!disableTextSelection) return
      if (document.getElementById('anti-selection-style')) return

      const style = document.createElement('style')
      style.id = 'anti-selection-style'
      style.textContent = `
        body {
          -webkit-user-select: none !important;
          -moz-user-select: none !important;
          -ms-user-select: none !important;
          user-select: none !important;
        }
        input, textarea, [contenteditable="true"],
        .ant-modal, .ant-modal *,
        .ant-message, .ant-message *,
        .ant-notification, .ant-notification *,
        .ant-select-dropdown, .ant-select-dropdown *,
        .ant-calendar, .ant-calendar *,
        .ant-time-picker-panel, .ant-time-picker-panel * {
          -webkit-user-select: auto !important;
          -moz-user-select: auto !important;
          -ms-user-select: auto !important;
          user-select: auto !important;
        }
        img {
          -webkit-user-drag: none !important;
          user-drag: none !important;
          pointer-events: none;
        }
        @media print {
          body * {
            visibility: hidden !important;
          }
          body::after {
            content: "打印功能已禁用";
            visibility: visible !important;
            display: block;
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            font-size: 24px;
            color: #666;
          }
        }
      `
      document.head.appendChild(style)
    }

    document.addEventListener('contextmenu', handleContextMenu)
    document.addEventListener('copy', handleCopy)
    document.addEventListener('cut', handleCopy)
    document.addEventListener('keydown', handleKeyDown)
    document.addEventListener('mousedown', handleMouseDown)
    document.addEventListener('selectstart', handleSelectStart)
    document.addEventListener('dragstart', handleDragStart)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('resize', preventDefault)

    addNoSelectStyle()
    detectScreenshot()

    return () => {
      document.removeEventListener('contextmenu', handleContextMenu)
      document.removeEventListener('copy', handleCopy)
      document.removeEventListener('cut', handleCopy)
      document.removeEventListener('keydown', handleKeyDown)
      document.removeEventListener('mousedown', handleMouseDown)
      document.removeEventListener('selectstart', handleSelectStart)
      document.removeEventListener('dragstart', handleDragStart)
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      window.removeEventListener('resize', preventDefault)

      if (blurOverlayRef.current && blurOverlayRef.current.parentNode) {
        blurOverlayRef.current.parentNode.removeChild(blurOverlayRef.current)
        blurOverlayRef.current = null
      }
    }
  }, [
    disableContextMenu,
    disableTextSelection,
    disableCopy,
    disablePrint,
    disableScreenshotKeys,
    enableScreenshotDetection,
    enableBlurOnVisibilityChange,
    customBlurStyle
  ])

  return {
    triggerScreenBlur: () => {
      const triggerScreenBlurLocal = () => {
        if (!blurOverlayRef.current) {
          const overlay = document.createElement('div')
          overlay.className = 'anti-screenshot-blur-overlay'
          overlay.style.cssText = customBlurStyle || `
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            z-index: 2147483647;
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(30px);
            -webkit-backdrop-filter: blur(30px);
            pointer-events: none;
            display: flex;
            align-items: center;
            justify-content: center;
          `
          const text = document.createElement('div')
          text.innerHTML = `
            <div style="text-align:center;">
              <div style="font-size: 48px; margin-bottom: 16px;">📛</div>
              <div style="font-size: 24px; font-weight: bold; color: #666;">内容受版权保护</div>
            </div>
          `
          overlay.appendChild(text)
          document.body.appendChild(overlay)
          blurOverlayRef.current = overlay
        }
        blurOverlayRef.current.style.display = 'flex'
      }
      triggerScreenBlurLocal()
    },
    removeScreenBlur: () => {
      if (blurOverlayRef.current) {
        blurOverlayRef.current.style.display = 'none'
      }
    }
  }
}

export default useAntiScreenCapture
