import type { ChartType } from '../../../types'

export function buildChartOption(
  chartType: ChartType,
  title: string | undefined,
  data: Record<string, any>[],
  xField: string | undefined,
  yFields: string[] | undefined,
  linkageFilter?: { field: string; value: any }
): Record<string, any> {
  let filteredData = data
  if (linkageFilter && linkageFilter.field && linkageFilter.value !== undefined) {
    filteredData = data.filter(row => String(row[linkageFilter.field]) === String(linkageFilter.value))
  }

  const base = {
    title: {
      text: title || '',
      left: 'center',
      textStyle: { color: '#e0e0e0', fontSize: 14 }
    },
    tooltip: { trigger: chartType === 'pie' ? 'item' : 'axis' },
    grid: { top: 50, bottom: 50, left: 60, right: 30 },
    backgroundColor: 'transparent'
  }

  switch (chartType) {
    case 'bar':
      return {
        ...base,
        xAxis: { type: 'category', data: filteredData.map(d => d[xField || '']), axisLabel: { color: '#aaa' }, axisLine: { lineStyle: { color: '#444' } } },
        yAxis: { type: 'value', axisLabel: { color: '#aaa' }, splitLine: { lineStyle: { color: '#2a2a3a' } } },
        series: (yFields || []).map(yf => ({
          name: yf,
          type: 'bar' as const,
          data: filteredData.map(d => d[yf]),
          itemStyle: { borderRadius: [4, 4, 0, 0] }
        }))
      }
    case 'line':
      return {
        ...base,
        xAxis: { type: 'category', data: filteredData.map(d => d[xField || '']), axisLabel: { color: '#aaa' }, axisLine: { lineStyle: { color: '#444' } } },
        yAxis: { type: 'value', axisLabel: { color: '#aaa' }, splitLine: { lineStyle: { color: '#2a2a3a' } } },
        series: (yFields || []).map(yf => ({
          name: yf,
          type: 'line' as const,
          smooth: true,
          data: filteredData.map(d => d[yf])
        }))
      }
    case 'pie':
      return {
        ...base,
        legend: { bottom: 0, textStyle: { color: '#aaa' } },
        series: [{
          name: title || '',
          type: 'pie' as const,
          radius: ['35%', '60%'],
          itemStyle: { borderRadius: 6, borderColor: '#0d1b2a', borderWidth: 2 },
          label: { color: '#e0e0e0' },
          data: filteredData.map(d => ({
            name: d[xField || ''],
            value: d[(yFields || [])[0] || '']
          }))
        }]
      }
    case 'radar': {
      const indicators = filteredData.map(d => ({
        name: d[xField || ''],
        max: Math.max(...filteredData.map(r => Number(r[(yFields || [])[0] || '']) || 0), 10) * 1.2
      }))
      return {
        ...base,
        radar: {
          indicator: indicators,
          axisName: { color: '#aaa' },
          splitArea: { areaStyle: { color: ['rgba(255,255,255,0.02)', 'rgba(255,255,255,0.05)'] } },
          splitLine: { lineStyle: { color: '#2a2a3a' } }
        },
        series: [{
          type: 'radar' as const,
          data: (yFields || []).map(yf => ({
            value: filteredData.map(d => d[yf]),
            name: yf
          }))
        }]
      }
    }
    case 'scatter':
      return {
        ...base,
        xAxis: { type: 'value', axisLabel: { color: '#aaa' }, splitLine: { lineStyle: { color: '#2a2a3a' } } },
        yAxis: { type: 'value', axisLabel: { color: '#aaa' }, splitLine: { lineStyle: { color: '#2a2a3a' } } },
        series: [{
          name: title || '',
          type: 'scatter' as const,
          data: filteredData.map(d => [d[xField || ''], d[(yFields || [])[0] || '']]),
          symbolSize: 8
        }]
      }
    default:
      return base
  }
}

export const CHART_COLORS = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#48b8d0'
]
