import React, { useEffect, useRef, useMemo } from 'react'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'
import { Card, Empty, Spin, Row, Col } from 'antd'
import { usePreviewStore } from '../../store/preview'
import { ChartConfig } from '../../utils/report'

interface ReportChartProps {
  charts?: ChartConfig[]
  height?: number | string
}

const getDefaultOption = (chart: ChartConfig): any => {
  const baseOption = {
    title: {
      text: chart.title || '',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 600
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    toolbox: {
      feature: {
        saveAsImage: { title: '保存为图片' },
        restore: { title: '还原' },
        dataView: { title: '数据视图', readOnly: true },
        magicType: { title: { line: '折线图', bar: '柱状图', stack: '堆叠', tiled: '平铺' }, type: ['line', 'bar'] }
      }
    }
  }

  switch (chart.type) {
    case 'bar':
      return {
        ...baseOption,
        xAxis: { type: 'category' },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', smooth: true }]
      }
    case 'line':
    case 'area':
      return {
        ...baseOption,
        xAxis: { type: 'category', boundaryGap: false },
        yAxis: { type: 'value' },
        series: [
          {
            type: 'line',
            smooth: true,
            areaStyle: chart.type === 'area' ? {} : undefined
          }
        ]
      }
    case 'pie':
      return {
        ...baseOption,
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 20,
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            }
          }
        ]
      }
    case 'scatter':
      return {
        ...baseOption,
        xAxis: { type: 'value' },
        yAxis: { type: 'value' },
        series: [{ type: 'scatter' }]
      }
    case 'radar':
      return {
        ...baseOption,
        tooltip: {},
        legend: {
          data: []
        },
        radar: {
          indicator: []
        },
        series: [{ type: 'radar' }]
      }
    case 'gauge':
      return {
        ...baseOption,
        tooltip: {
          formatter: '{a} <br/>{b} : {c}%'
        },
        series: [
          {
            type: 'gauge',
            progress: { show: true, width: 18 },
            axisLine: { lineStyle: { width: 18 } },
            axisTick: { show: false },
            splitLine: { length: 15, lineStyle: { width: 2, color: '#999' } },
            pointer: { width: 6 },
            detail: { valueAnimation: true, formatter: '{value}%', fontSize: 30, offsetCenter: [0, '35%'] }
          }
        ]
      }
    default:
      return baseOption
  }
}

const SingleChart: React.FC<{ chart: ChartConfig; height?: number | string }> = ({ chart, height = 300 }) => {
  const chartRef = useRef<ReactECharts>(null)

  const option = useMemo(() => {
    if (chart.option) {
      return chart.option
    }
    return getDefaultOption(chart)
  }, [chart])

  useEffect(() => {
    const handleResize = () => {
      chartRef.current?.getEchartsInstance()?.resize()
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return (
    <ReactECharts
      ref={chartRef}
      option={option}
      style={{ height: chart.height || height, width: chart.width || '100%' }}
      notMerge
      lazyUpdate
      opts={{ renderer: 'canvas' }}
    />
  )
}

const ReportChart: React.FC<ReportChartProps> = ({ charts, height = 300 }) => {
  const storeData = usePreviewStore((state) => state.reportData)
  const loading = usePreviewStore((state) => state.loading)

  const chartList = useMemo(() => {
    if (charts && charts.length > 0) return charts
    if (storeData?.charts && storeData.charts.length > 0) return storeData.charts
    return []
  }, [charts, storeData])

  if (chartList.length === 0) {
    return (
      <Card style={{ borderRadius: 8, marginBottom: 16 }}>
        <div style={{ padding: 40, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Empty description="暂无图表数据" />
        </div>
      </Card>
    )
  }

  return (
    <Spin spinning={loading}>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        {chartList.map((chart) => (
          <Col xs={24} sm={24} md={chartList.length > 1 ? 12 : 24} lg={chartList.length > 2 ? 8 : chartList.length > 1 ? 12 : 24} key={chart.id}>
            <Card
              title={chart.title}
              style={{ borderRadius: 8, height: '100%' }}
              bodyStyle={{ padding: 12 }}
            >
              <SingleChart chart={chart} height={height} />
            </Card>
          </Col>
        ))}
      </Row>
    </Spin>
  )
}

export default ReportChart
