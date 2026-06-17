import dayjs from 'dayjs'

export type ParamType = 'input' | 'select' | 'date' | 'dateRange' | 'radio' | 'checkbox' | 'number' | 'textarea'

export interface ReportParamOption {
  label: string
  value: string | number | boolean
}

export interface ReportParam {
  name: string
  label: string
  type: ParamType
  required?: boolean
  defaultValue?: any
  options?: ReportParamOption[]
  placeholder?: string
  disabled?: boolean
}

export interface ChartConfig {
  id: string
  title?: string
  type: 'bar' | 'line' | 'pie' | 'area' | 'scatter' | 'radar' | 'gauge'
  option?: any
  height?: number | string
  width?: number | string
  datasetId?: number
  datasetName?: string
  xAxisField?: string
  yAxisFields?: string[]
  x?: number
  y?: number
  linkageField?: string
  linkageTargetId?: string
  data?: Record<string, any>[]
}

export interface TableColumn {
  title: string
  dataIndex: string
  key?: string
  width?: number | string
  align?: 'left' | 'center' | 'right'
  sorter?: boolean
  filters?: Array<{ text: string; value: any }>
  render?: (value: any, record: any, index: number) => React.ReactNode
}

export interface TableData {
  columns: TableColumn[]
  dataSource: any[]
  total?: number
  pageNum?: number
  pageSize?: number
}

export interface ReportRenderResult {
  html?: string
  table?: TableData
  charts?: ChartConfig[]
  title?: string
  summary?: string
}

export const getDefaultParamValue = (param: ReportParam): any => {
  if (param.defaultValue !== undefined) {
    return param.defaultValue
  }
  switch (param.type) {
    case 'input':
    case 'textarea':
      return ''
    case 'number':
      return null
    case 'select':
      return undefined
    case 'date':
      return null
    case 'dateRange':
      return null
    case 'radio':
      return undefined
    case 'checkbox':
      return []
    default:
      return ''
  }
}

export const initParamValues = (params: ReportParam[]): Record<string, any> => {
  const values: Record<string, any> = {}
  params.forEach((param) => {
    values[param.name] = getDefaultParamValue(param)
  })
  return values
}

export const formatParamValue = (params: ReportParam[], values: Record<string, any>): Record<string, any> => {
  const result: Record<string, any> = {}
  params.forEach((param) => {
    const value = values[param.name]
    if (value === undefined || value === null || value === '') {
      return
    }
    switch (param.type) {
      case 'date':
        if (dayjs.isDayjs(value)) {
          result[param.name] = value.format('YYYY-MM-DD')
        } else {
          result[param.name] = value
        }
        break
      case 'dateRange':
        if (Array.isArray(value) && value.length === 2) {
          const [start, end] = value
          result[param.name + 'Start'] = dayjs.isDayjs(start) ? start.format('YYYY-MM-DD') : start
          result[param.name + 'End'] = dayjs.isDayjs(end) ? end.format('YYYY-MM-DD') : end
        }
        break
      case 'checkbox':
        if (Array.isArray(value)) {
          result[param.name] = value.join(',')
        }
        break
      default:
        result[param.name] = value
    }
  })
  return result
}

export const downloadBlob = (blob: Blob, filename: string): void => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export const parseHtmlTable = (html: string): TableData | null => {
  if (!html) return null
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')
  const table = doc.querySelector('table')
  if (!table) return null

  const columns: TableColumn[] = []
  const dataSource: any[] = []

  const headers = table.querySelectorAll('thead th')
  headers.forEach((th, index) => {
    columns.push({
      title: th.textContent?.trim() || `列${index + 1}`,
      dataIndex: `col_${index}`,
      key: `col_${index}`
    })
  })

  const rows = table.querySelectorAll('tbody tr')
  rows.forEach((row) => {
    const cells = row.querySelectorAll('td')
    const record: any = {}
    cells.forEach((cell, index) => {
      record[`col_${index}`] = cell.textContent?.trim() || ''
    })
    dataSource.push(record)
  })

  return {
    columns,
    dataSource,
    total: dataSource.length
  }
}

export const isMobileDevice = (): boolean => {
  const userAgent = navigator.userAgent.toLowerCase()
  const mobileKeywords = ['android', 'iphone', 'ipad', 'ipod', 'blackberry', 'windows phone', 'mobile']
  return mobileKeywords.some((keyword) => userAgent.includes(keyword))
}

export const buildChartOption = (
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

  const title = chart.title || ''
  const xField = chart.xAxisField || ''
  const yFields = chart.yAxisFields || []

  if (chart.type === 'pie') {
    return {
      title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '65%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        data: filteredData.map((d: any) => ({
          name: d[xField],
          value: d[yFields[0] || '']
        }))
      }]
    }
  }

  if (chart.type === 'radar') {
    const indicators = filteredData.map((d: any) => ({
      name: d[xField],
      max: Math.max(...filteredData.map((r: any) => Number(r[yFields[0] || '']) || 0), 10) * 1.2
    }))
    return {
      title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
      tooltip: {},
      legend: { bottom: 0 },
      radar: { indicator: indicators },
      series: [{
        type: 'radar',
        data: yFields.map(yf => ({
          value: filteredData.map((d: any) => d[yf]),
          name: yf
        }))
      }]
    }
  }

  if (chart.type === 'scatter') {
    return {
      title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'item' },
      grid: { top: 40, bottom: 30, left: 50, right: 20 },
      xAxis: { type: 'value' },
      yAxis: { type: 'value' },
      series: [{
        type: 'scatter',
        symbolSize: 10,
        data: filteredData.map((d: any) => [d[xField], d[yFields[0] || '']])
      }]
    }
  }

  const isArea = chart.type === 'area'
  const seriesType = isArea ? 'line' : chart.type

  const xAxisData = filteredData.map((d: any) => d[xField])
  const series = yFields.map((yf: string) => ({
    name: yf,
    type: seriesType,
    smooth: isArea || chart.type === 'line',
    areaStyle: isArea ? {} : undefined,
    itemStyle: chart.type === 'bar' ? { borderRadius: [4, 4, 0, 0] } : undefined,
    data: filteredData.map((d: any) => d[yf])
  }))

  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { top: 40, bottom: 30, left: 50, right: 20 },
    xAxis: { type: 'category', data: xAxisData },
    yAxis: { type: 'value' },
    series
  }
}

export const handlePrint = (contentId: string = 'report-content'): void => {
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    return
  }

  const content = document.getElementById(contentId)
  if (!content) {
    printWindow.close()
    return
  }

  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
      <head>
        <title>报表打印</title>
        <style>
          body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 20px; }
          table { width: 100%; border-collapse: collapse; margin: 10px 0; }
          th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; font-size: 14px; }
          th { background-color: #f5f5f5; font-weight: 600; }
          @media print {
            body { margin: 0; }
          }
        </style>
      </head>
      <body>
        ${content.innerHTML}
      </body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 300)
}
