import React, { useEffect, useRef, useMemo, useState, useCallback } from 'react'
import ReactECharts from 'echarts-for-react'
import { Card, Empty, Spin, Row, Col } from 'antd'
import { usePreviewStore } from '../../store/preview'
import { useCommentStore } from '../../store/comment'
import { ChartConfig, buildChartOption } from '../../utils/report'
import { getDatasetPreview } from '@/api/dataset'
import CommentBubble from '../CommentBubble'

interface ReportChartProps {
  charts?: ChartConfig[]
  height?: number | string
}

const SingleChart: React.FC<{
  chart: ChartConfig
  data: Record<string, any>[]
  height?: number | string
  commentCount: number
  isActive: boolean
  linkageFilter?: { field: string; value: any }
  onChartClick?: (params: any) => void
  onChartSelect?: (chartId: string) => void
}> = ({ chart, data, height = 300, commentCount, isActive, linkageFilter, onChartClick, onChartSelect }) => {
  const chartRef = useRef<ReactECharts>(null)

  const option = useMemo(() => {
    if (chart.option) {
      return chart.option
    }
    if (data.length > 0 && chart.xAxisField && chart.yAxisFields) {
      return buildChartOption(chart, data, linkageFilter)
    }
    return null
  }, [chart, data, linkageFilter])

  useEffect(() => {
    const handleResize = () => {
      chartRef.current?.getEchartsInstance()?.resize()
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  const handleClick = useCallback((params: any) => {
    if (onChartSelect) {
      onChartSelect(String(chart.id))
    }
    onChartClick?.(params)
  }, [chart.id, onChartSelect, onChartClick])

  if (!option) {
    return (
      <div style={{
        height: chart.height || height,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}>
        <Empty description="暂无数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </div>
    )
  }

  return (
    <div style={{ position: 'relative' }}>
      <ReactECharts
        ref={chartRef}
        option={option}
        style={{
          height: chart.height || height,
          width: chart.width || '100%',
          cursor: 'pointer',
          border: isActive ? '2px solid #1677ff' : '2px solid transparent',
          borderRadius: 8,
          transition: 'border 0.2s'
        }}
        notMerge
        lazyUpdate
        opts={{ renderer: 'canvas' }}
        onEvents={{ click: handleClick }}
      />
      <CommentBubble
        count={commentCount}
        active={isActive}
        onClick={() => onChartSelect?.(String(chart.id))}
      />
    </div>
  )
}

const ReportChart: React.FC<ReportChartProps> = ({ charts, height = 300 }) => {
  const storeData = usePreviewStore((state) => state.reportData)
  const loading = usePreviewStore((state) => state.loading)
  const [chartDataMap, setChartDataMap] = useState<Record<number, Record<string, any>[]>>({})
  const [dataLoading, setDataLoading] = useState(false)
  const [linkageState, setLinkageState] = useState<Record<string, { field: string; value: any }>>({})

  const getChartCommentCount = useCommentStore((state) => state.getChartCommentCount)
  const selectedChartId = useCommentStore((state) => state.selectedChartId)
  const setSelectedChartId = useCommentStore((state) => state.setSelectedChartId)
  const setSelectedCellRef = useCommentStore((state) => state.setSelectedCellRef)

  const chartList = useMemo(() => {
    if (charts && charts.length > 0) return charts
    if (storeData?.charts && storeData.charts.length > 0) return storeData.charts
    return []
  }, [charts, storeData])

  useEffect(() => {
    const loadData = async () => {
      const datasetIds = [...new Set(chartList.map(c => c.datasetId).filter(Boolean))] as number[]
      if (datasetIds.length === 0) return

      setDataLoading(true)
      const newDataMap: Record<number, Record<string, any>[]> = {}
      try {
        await Promise.all(
          datasetIds.map(async (dsId) => {
            try {
              const res = await getDatasetPreview(dsId, {}, 500)
              newDataMap[dsId] = res.rows || []
            } catch {
              newDataMap[dsId] = []
            }
          })
        )
        setChartDataMap(newDataMap)
      } finally {
        setDataLoading(false)
      }
    }

    if (chartList.length > 0) {
      loadData()
    }
  }, [chartList])

  const handleChartSelect = useCallback((chartId: string) => {
    setSelectedCellRef(null)
    setSelectedChartId(chartId)
  }, [setSelectedCellRef, setSelectedChartId])

  const handleChartClick = (chart: ChartConfig) => (params: any) => {
    if (!chart.linkageField || !chart.linkageTargetId) return
    const clickedName = params.name
    if (clickedName === undefined) return
    setLinkageState(prev => {
      const current = prev[chart.linkageTargetId!]
      if (current && current.value === clickedName) {
        const { [chart.linkageTargetId!]: _, ...rest } = prev
        return rest
      }
      return {
        ...prev,
        [chart.linkageTargetId!]: { field: chart.linkageField, value: clickedName }
      }
    })
  }

  if (chartList.length === 0) {
    return (
      <Card style={{ borderRadius: 8, marginBottom: 16 }}>
        <div style={{ padding: 40, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Empty description="暂无图表数据" />
        </div>
      </Card>
    )
  }

  const isLoading = loading || dataLoading

  return (
    <Spin spinning={isLoading}>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        {chartList.map((chart) => (
          <Col
            xs={24}
            sm={24}
            md={chartList.length > 1 ? 12 : 24}
            lg={chartList.length > 2 ? 8 : chartList.length > 1 ? 12 : 24}
            key={chart.id}
          >
            <Card
              title={chart.title}
              style={{
                borderRadius: 8,
                height: '100%',
                border: selectedChartId === String(chart.id) ? '2px solid #1677ff' : '1px solid #f0f0f0'
              }}
              bodyStyle={{ padding: 12 }}
              extra={
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  {chart.linkageField && (
                    <span style={{ fontSize: 11, color: '#999' }}>支持联动</span>
                  )}
                </div>
              }
            >
              <SingleChart
                chart={chart}
                data={chart.datasetId ? (chartDataMap[chart.datasetId] || []) : (chart.data || [])}
                height={height}
                commentCount={getChartCommentCount(String(chart.id))}
                isActive={selectedChartId === String(chart.id)}
                linkageFilter={linkageState[chart.id]}
                onChartClick={handleChartClick(chart)}
                onChartSelect={handleChartSelect}
              />
            </Card>
          </Col>
        ))}
      </Row>
    </Spin>
  )
}

export default ReportChart
