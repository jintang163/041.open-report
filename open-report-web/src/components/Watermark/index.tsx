import React, { useEffect, useRef, useState } from 'react'
import { useUserStore } from '@/store/user'
import dayjs from 'dayjs'

interface WatermarkProps {
  children?: React.ReactNode
  zIndex?: number
  rotate?: number
  fontSize?: number
  opacity?: number
  gapX?: number
  gapY?: number
  color?: string
  fontFamily?: string
}

interface WatermarkText {
  username: string
  timestamp: string
  ip: string
}

const Watermark: React.FC<WatermarkProps> = ({
  children,
  zIndex = 9999,
  rotate = -22,
  fontSize = 14,
  opacity = 0.15,
  gapX = 180,
  gapY = 100,
  color = '#666666',
  fontFamily = 'Arial, sans-serif'
}) => {
  const containerRef = useRef<HTMLDivElement>(null)
  const watermarkRef = useRef<HTMLDivElement>(null)
  const observerRef = useRef<MutationObserver | null>(null)
  const [watermarkText, setWatermarkText] = useState<WatermarkText>({
    username: '',
    timestamp: '',
    ip: ''
  })

  const userInfo = useUserStore((state) => state.userInfo)

  useEffect(() => {
    const fetchClientIP = async () => {
      try {
        const response = await fetch('https://api.ipify.org?format=json')
        const data = await response.json()
        return data.ip
      } catch {
        return '0.0.0.0'
      }
    }

    const initWatermark = async () => {
      const ip = await fetchClientIP()
      setWatermarkText({
        username: userInfo?.username || userInfo?.nickname || '匿名用户',
        timestamp: dayjs().format('YYYY-MM-DD HH:mm:ss'),
        ip
      })
    }

    initWatermark()

    const timer = setInterval(() => {
      setWatermarkText((prev) => ({
        ...prev,
        timestamp: dayjs().format('YYYY-MM-DD HH:mm:ss')
      }))
    }, 60000)

    return () => clearInterval(timer)
  }, [userInfo])

  const createWatermarkImage = (): string => {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    if (!ctx) return ''

    const line1 = watermarkText.username
    const line2 = watermarkText.timestamp
    const line3 = `IP: ${watermarkText.ip}`

    const textPadding = 10
    const lineHeight = fontSize + textPadding
    const textWidth = Math.max(
      ctx.measureText(line1).width,
      ctx.measureText(line2).width,
      ctx.measureText(line3).width
    )

    const blockWidth = Math.max(gapX, textWidth + 80)
    const blockHeight = Math.max(gapY, lineHeight * 3 + 40)

    canvas.width = blockWidth
    canvas.height = blockHeight

    ctx.translate(blockWidth / 2, blockHeight / 2)
    ctx.rotate((rotate * Math.PI) / 180)
    ctx.translate(-blockWidth / 2, -blockHeight / 2)

    ctx.font = `${fontSize}px ${fontFamily}`
    ctx.fillStyle = color
    ctx.globalAlpha = opacity
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'

    const centerX = blockWidth / 2
    const centerY = blockHeight / 2

    ctx.fillText(line1, centerX, centerY - lineHeight)
    ctx.fillText(line2, centerX, centerY)
    ctx.fillText(line3, centerX, centerY + lineHeight)

    return canvas.toDataURL('image/png')
  }

  const applyWatermark = () => {
    if (!watermarkRef.current || !watermarkText.username) return

    const dataUrl = createWatermarkImage()
    watermarkRef.current.style.backgroundImage = `url('${dataUrl}')`
    watermarkRef.current.style.backgroundRepeat = 'repeat'
    watermarkRef.current.style.pointerEvents = 'none'
  }

  useEffect(() => {
    applyWatermark()
  }, [watermarkText, rotate, fontSize, opacity, gapX, gapY, color, fontFamily])

  useEffect(() => {
    if (!watermarkRef.current || !containerRef.current) return

    const observeMutations = () => {
      observerRef.current = new MutationObserver((mutations) => {
        for (const mutation of mutations) {
          if (mutation.type === 'childList') {
            const removedNodes = Array.from(mutation.removedNodes)
            if (removedNodes.includes(watermarkRef.current!)) {
              containerRef.current!.appendChild(watermarkRef.current!)
              applyWatermark()
            }
          }
          if (mutation.type === 'attributes' && mutation.target === watermarkRef.current) {
            applyWatermark()
          }
        }
      })

      observerRef.current.observe(containerRef.current, {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ['style', 'class', 'hidden']
      })
    }

    observeMutations()

    return () => {
      observerRef.current?.disconnect()
    }
  }, [])

  return (
    <div ref={containerRef} style={{ position: 'relative', width: '100%', height: '100%' }}>
      {children}
      <div
        ref={watermarkRef}
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          zIndex,
          pointerEvents: 'none',
          overflow: 'hidden'
        }}
      />
    </div>
  )
}

export default Watermark
