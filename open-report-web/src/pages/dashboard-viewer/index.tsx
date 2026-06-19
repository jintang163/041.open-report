import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Spin, Button, message } from 'antd'
import { FullscreenOutlined, FullscreenExitOutlined, ReloadOutlined, EditOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import { getDashboardDetail, getChartData } from '@/api/dashboard'
import { buildChartOption } from '../dashboard-designer/utils/chartOptions'
import Watermark from '@/components/Watermark'
import useAntiScreenCapture from '@/hooks/useAntiScreenCapture'
import type { ChartDashboard, ChartDashboardItem } from '@/types'

const DashboardViewer: React.FC = () => {
  useAntiScreenCapture({
    disableContextMenu: true,
    disableTextSelection: true,
    disableCopy: true,
    disablePrint: true,
    disableScreenshotKeys: true,
    enableScreenshotDetection: true,
    enableBlurOnVisibilityChange: true
  })

  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [dashboard, setDashboard] = useState<ChartDashboard | null>(null)
  const [items, setItems] = useState<ChartDashboardItem[]>([])
  const [chartDataMap, setChartDataMap] = useState<Record<number, Record<string, any>[]>>({})
  const [linkageState, setLinkageState] = useState<Record<number, { field: string; value: any }>>({})
  const [isFullscreen, setIsFullscreen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const refreshTimerRef = useRef<NodeJS.Timeout | null>(null)
  const [scale, setScale] = useState(1)

  const loadDashboard = useCallback(async () => {
    if (!id) return
    try {
      const res = await getDashboardDetail(Number(id))
      setDashboard(res.dashboard)
      const parsedItems = res.items.map(i => ({
        ...i,
        yFields: typeof (i as any).yFields === 'string'
          ? JSON.parse((i as any).yFields)
          : (i.yFields || []),
        chartConfig: typeof (i as any).chartConfig === 'string'
          ? JSON.parse((i as any).chartConfig)
          : (i.chartConfig || {})
      }))
      setItems(parsedItems)
      await loadAllChartData(parsedItems)
    } catch {
      message.error('加载大屏失败')
    } finally {
      setLoading(false)
    }
  }, [id])

  const loadAllChartData = async (itemList: ChartDashboardItem[]) => {
    const datasetIds = [...new Set(itemList.map(i => i.datasetId).filter(Boolean))] as number[]
    const newDataMap: Record<number, Record<string, any>[]> = {}
    await Promise.all(
      datasetIds.map(async dsId => {
        try {
          const data = await getChartData(dsId)
          newDataMap[dsId] = data
        } catch {}
      })
    )
    setChartDataMap(newDataMap)
  }

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  useEffect(() => {
    if (dashboard?.refreshInterval && dashboard.refreshInterval > 0) {
      refreshTimerRef.current = setInterval(() => {
        loadAllChartData(items)
      }, dashboard.refreshInterval * 1000)
    }
    return () => {
      if (refreshTimerRef.current) clearInterval(refreshTimerRef.current)
    }
  }, [dashboard?.refreshInterval, items])

  useEffect(() => {
    const calcScale = () => {
      if (!dashboard || !containerRef.current) return
      const containerW = containerRef.current.clientWidth
      const containerH = containerRef.current.clientHeight
      const scaleX = containerW / (dashboard.canvasWidth || 1920)
      const scaleY = containerH / (dashboard.canvasHeight || 1080)
      setScale(Math.min(scaleX, scaleY))
    }
    calcScale()
    window.addEventListener('resize', calcScale)
    return () => window.removeEventListener('resize', calcScale)
  }, [dashboard])

  const toggleFullscreen = () => {
    if (!isFullscreen) {
      containerRef.current?.requestFullscreen?.()
    } else {
      document.exitFullscreen?.()
    }
  }

  useEffect(() => {
    const onFullscreenChange = () => setIsFullscreen(!!document.fullscreenElement)
    document.addEventListener('fullscreenchange', onFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', onFullscreenChange)
  }, [])

  const handleChartClick = (item: ChartDashboardItem) => (params: any) => {
    if (!item.linkageField || !item.linkageTargetId) return
    const clickedName = params.name
    if (clickedName === undefined) return
    setLinkageState(prev => {
      const current = prev[item.linkageTargetId!]
      if (current && current.value === clickedName) {
        const { [item.linkageTargetId!]: _, ...rest } = prev
        return rest
      }
      return {
        ...prev,
        [item.linkageTargetId!]: { field: item.linkageField, value: clickedName }
      }
    })
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#0a0e1a' }}>
        <Spin size="large" tip="加载大屏..." />
      </div>
    )
  }

  return (
    <Watermark>
    <div
      ref={containerRef}
      style={{
        width: '100vw',
        height: '100vh',
        background: dashboard?.backgroundColor || '#0d1b2a',
        overflow: 'hidden',
        position: 'relative',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}
    >
      <div style={{
        transform: `scale(${scale})`,
        transformOrigin: 'center center',
        width: dashboard?.canvasWidth || 1920,
        height: dashboard?.canvasHeight || 1080,
        position: 'relative'
      }}>
        {items.map(item => {
          const data = item.datasetId ? (chartDataMap[item.datasetId] || []) : []
          const option = buildChartOption(
            item.chartType,
            item.title,
            data,
            item.xField,
            item.yFields,
            linkageState[item.id!]
          )
          return (
            <div
              key={item.id}
              style={{
                position: 'absolute',
                left: item.positionX,
                top: item.positionY,
                width: item.width,
                height: item.height,
                background: 'rgba(13,27,42,0.85)',
                borderRadius: 8,
                overflow: 'hidden',
                border: '1px solid rgba(255,255,255,0.1)',
                boxShadow: '0 2px 8px rgba(0,0,0,0.3)'
              }}
            >
              {item.title && (
                <div style={{
                  padding: '6px 12px',
                  color: '#e0e0e0',
                  fontSize: 13,
                  fontWeight: 600,
                  borderBottom: '1px solid rgba(255,255,255,0.06)',
                  background: 'rgba(255,255,255,0.03)'
                }}>
                  {item.title}
                </div>
              )}
              <div style={{ width: '100%', height: item.title ? 'calc(100% - 32px)' : '100%' }}>
                {data.length > 0 ? (
                  <ReactECharts
                    option={option}
                    style={{ width: '100%', height: '100%' }}
                    notMerge
                    lazyUpdate
                    onEvents={{ click: handleChartClick(item) }}
                  />
                ) : (
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    height: '100%',
                    color: '#666',
                    fontSize: 13
                  }}>
                    暂无数据
                  </div>
                )}
              </div>
            </div>
          )
        })}
      </div>

      <div style={{
        position: 'absolute',
        top: 12,
        right: 12,
        display: 'flex',
        gap: 8,
        zIndex: 100
      }}>
        <Button
          icon={<ReloadOutlined />}
          size="small"
          onClick={() => { loadAllChartData(items); message.success('已刷新') }}
          style={{ background: 'rgba(0,0,0,0.4)', border: 'none', color: '#ddd' }}
        />
        <Button
          icon={<EditOutlined />}
          size="small"
          onClick={() => navigate(`/dashboard/designer/${id}`)}
          style={{ background: 'rgba(0,0,0,0.4)', border: 'none', color: '#ddd' }}
        />
        <Button
          icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
          size="small"
          onClick={toggleFullscreen}
          style={{ background: 'rgba(0,0,0,0.4)', border: 'none', color: '#ddd' }}
        />
      </div>

      {dashboard?.refreshInterval && dashboard.refreshInterval > 0 && (
        <div style={{
          position: 'absolute',
          bottom: 12,
          right: 12,
          color: 'rgba(255,255,255,0.3)',
          fontSize: 11,
          zIndex: 100
        }}>
          每 {dashboard.refreshInterval} 秒自动刷新
        </div>
      )}
    </div>
    </Watermark>
  )
}

export default DashboardViewer
