import { useRef, useMemo, useState, useEffect } from 'react'
import ReactECharts from 'echarts-for-react'
import { useDesignerStore, type ChartConfig } from '../../store/designer'
import { getDatasetPreview } from '@/api/dataset'

const CHART_TYPE_LABELS: Record<string, string> = {
  bar: '柱状图',
  line: '折线图',
  pie: '饼图',
  area: '面积图',
  scatter: '散点图',
  radar: '雷达图'
}

const buildChartOption = (
  chart: ChartConfig,
  data: Record<string, any>[] = [],
  linkageFilter?: { field: string; value: any }
): Record<string, any> => {
  let filteredData = data
  if (linkageFilter && linkageFilter.field && linkageFilter.value !== undefined) {
    filteredData = data.filter(row =>
      String(row[linkageFilter.field]) === String(linkageFilter.value)
    )
  }

  const title = chart.title || CHART_TYPE_LABELS[chart.type] || '图表'

  if (chart.type === 'pie') {
    const xField = chart.xAxisField || ''
    const yField = (chart.yAxisFields || [])[0] || ''
    return {
      title: { text: title, left: 'center', textStyle: { fontSize: 13 } },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: '60%',
        itemStyle: { borderRadius: 4 },
        data: filteredData.map((d: any) => ({
          name: d[xField],
          value: d[yField]
        }))
      }]
    }
  }

  if (chart.type === 'radar') {
    const indicators = filteredData.map((d: any) => ({
      name: d[chart.xAxisField || ''],
      max: Math.max(...filteredData.map((r: any) => Number(r[(chart.yAxisFields || [])[0] || '']) || 0), 10) * 1.2
    }))
    return {
      title: { text: title, left: 'center', textStyle: { fontSize: 13 } },
      tooltip: {},
      radar: { indicator: indicators },
      series: [{
        type: 'radar',
        data: (chart.yAxisFields || []).map(yf => ({
          value: filteredData.map((d: any) => d[yf]),
          name: yf
        }))
      }]
    }
  }

  const xAxisData = filteredData.map((d: any) => d[chart.xAxisField || ''])
  const series = (chart.yAxisFields || []).map(yf => ({
    name: yf,
    type: chart.type === 'area' ? 'line' : chart.type,
    areaStyle: chart.type === 'area' ? {} : undefined,
    smooth: chart.type === 'area' || chart.type === 'line',
    data: filteredData.map((d: any) => d[yf])
  }))

  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 13 } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { top: 40, bottom: 30, left: 50, right: 20 },
    xAxis: { type: 'category', data: xAxisData },
    yAxis: { type: 'value' },
    series
  }
}

const ChartBox: React.FC<{
  chart: ChartConfig
  chartData: Record<string, any>[]
  isSelected: boolean
  linkageFilter?: { field: string; value: any }
  onSelect: () => void
  onMove: (dx: number, dy: number) => void
  onResize: (w: number, h: number) => void
  onChartClick?: (params: any) => void
}> = ({ chart, chartData, isSelected, linkageFilter, onSelect, onMove, onResize, onChartClick }) => {
  const moveRef = useRef<{ startX: number; startY: number; originX: number; originY: number } | null>(null)
  const resizeRef = useRef<{ startX: number; startY: number; originW: number; originH: number } | null>(null)

  const option = useMemo(() =>
    buildChartOption(chart, chartData, linkageFilter),
    [chart, chartData, linkageFilter]
  )

  const handleMouseDown = (e: React.MouseEvent) => {
    e.stopPropagation()
    e.preventDefault()
    onSelect()
    moveRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      originX: chart.x || 0,
      originY: chart.y || 0
    }

    const handleMouseMove = (ev: MouseEvent) => {
      if (!moveRef.current) return
      const dx = ev.clientX - moveRef.current.startX
      const dy = ev.clientY - moveRef.current.startY
      onMove(moveRef.current.originX + dx, moveRef.current.originY + dy)
    }

    const handleMouseUp = () => {
      moveRef.current = null
      window.removeEventListener('mousemove', handleMouseMove)
      window.removeEventListener('mouseup', handleMouseUp)
    }

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseup', handleMouseUp)
  }

  const handleResizeMouseDown = (e: React.MouseEvent) => {
    e.stopPropagation()
    e.preventDefault()
    resizeRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      originW: chart.width || 400,
      originH: chart.height || 300
    }

    const handleMouseMove = (ev: MouseEvent) => {
      if (!resizeRef.current) return
      const dw = ev.clientX - resizeRef.current.startX
      const dh = ev.clientY - resizeRef.current.startY
      onResize(
        Math.max(200, resizeRef.current.originW + dw),
        Math.max(150, resizeRef.current.originH + dh)
      )
    }

    const handleMouseUp = () => {
      resizeRef.current = null
      window.removeEventListener('mousemove', handleMouseMove)
      window.removeEventListener('mouseup', handleMouseUp)
    }

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseup', handleMouseUp)
  }

  const width = chart.width || 400
  const height = chart.height || 300

  return (
    <div
      style={{
        position: 'absolute',
        left: chart.x || 100,
        top: chart.y || 100,
        width,
        height,
        background: '#fff',
        border: isSelected ? '2px solid #1677ff' : '1px solid #d9d9d9',
        borderRadius: 6,
        boxShadow: isSelected ? '0 0 0 2px rgba(22,119,255,0.2)' : '0 2px 8px rgba(0,0,0,0.1)',
        cursor: 'move',
        overflow: 'hidden',
        zIndex: isSelected ? 100 : 10,
        userSelect: 'none'
      }}
      onMouseDown={handleMouseDown}
    >
      {chartData.length > 0 ? (
        <ReactECharts
          option={option}
          style={{ width: '100%', height: '100%' }}
          notMerge
          lazyUpdate
          onEvents={{ click: onChartClick }}
          opts={{ renderer: 'canvas' }}
        />
      ) : (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height: '100%',
          color: '#999',
          fontSize: 13,
          gap: 8
        }}>
          <div style={{ fontSize: 28 }}>📊</div>
          <div>{chart.title || CHART_TYPE_LABELS[chart.type]}</div>
          <div style={{ fontSize: 11, color: '#bbb' }}>请配置数据集</div>
        </div>
      )}
      {isSelected && (
        <div
          onMouseDown={handleResizeMouseDown}
          style={{
            position: 'absolute',
            right: 0,
            bottom: 0,
            width: 14,
            height: 14,
            cursor: 'nwse-resize',
            background: 'rgba(22,119,255,0.6)',
            borderRadius: '0 0 6px 0',
            zIndex: 10
          }}
        />
      )}
    </div>
  )
}

const ChartCanvasOverlay: React.FC = () => {
  const {
    charts,
    selectedChartId,
    setSelectedChartId,
    updateChart,
    dataSources
  } = useDesignerStore()

  const [chartDataMap, setChartDataMap] = useState<Record<number, Record<string, any>[]>>({})
  const [linkageState, setLinkageState] = useState<Record<string, { field: string; value: any }>>({})
  const [loadingMap, setLoadingMap] = useState<Record<number, boolean>>({})

  const getAllDatasets = () => {
    const datasets: any[] = []
    dataSources.forEach((ds: any) => {
      if (ds.dataSets) datasets.push(...ds.dataSets)
    })
    return datasets
  }

  const loadChartData = async (datasetId: number) => {
    if (chartDataMap[datasetId] || loadingMap[datasetId]) return
    setLoadingMap(prev => ({ ...prev, [datasetId]: true }))
    try {
      const res = await getDatasetPreview(datasetId, {}, 200)
      setChartDataMap(prev => ({ ...prev, [datasetId]: res.rows || [] }))
    } catch {
      setChartDataMap(prev => ({ ...prev, [datasetId]: [] }))
    } finally {
      setLoadingMap(prev => ({ ...prev, [datasetId]: false }))
    }
  }

  useEffect(() => {
    charts.forEach(chart => {
      if (chart.datasetId) loadChartData(chart.datasetId)
    })
  }, [charts])

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

  const handleCanvasClick = () => {
    setSelectedChartId(null)
  }

  const handleMove = (id: string, x: number, y: number) => {
    const chart = charts.find(c => c.id === id)
    if (chart) {
      updateChart({ ...chart, x, y })
    }
  }

  const handleResize = (id: string, width: number, height: number) => {
    const chart = charts.find(c => c.id === id)
    if (chart) {
      updateChart({ ...chart, width, height })
    }
  }

  return (
    <div
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        pointerEvents: 'none',
        zIndex: 5
      }}
      onClick={handleCanvasClick}
    >
      <div style={{ position: 'relative', width: '100%', height: '100%', pointerEvents: 'auto' }}>
        {charts.map(chart => (
          <ChartBox
            key={chart.id}
            chart={chart}
            chartData={chart.datasetId ? (chartDataMap[chart.datasetId] || []) : []}
            isSelected={selectedChartId === chart.id}
            linkageFilter={linkageState[chart.id]}
            onSelect={() => setSelectedChartId(chart.id)}
            onMove={(x, y) => handleMove(chart.id, x, y)}
            onResize={(w, h) => handleResize(chart.id, w, h)}
            onChartClick={handleChartClick(chart)}
          />
        ))}
      </div>
    </div>
  )
}

export default ChartCanvasOverlay
