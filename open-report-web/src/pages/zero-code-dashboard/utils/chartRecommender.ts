import type { ChartType, FieldInfo, FieldCategory, ChartRecommendationResult } from '@/types'

export const categorizeField = (name: string, type: string, sampleValue: any): FieldCategory => {
  const lowerType = type?.toLowerCase() || ''
  const lowerName = name?.toLowerCase() || ''

  if (lowerType.includes('date') || lowerType.includes('time') || lowerType.includes('year')
      || lowerType.includes('month') || lowerType.includes('day')) {
    return 'DATE'
  }

  if (lowerType.includes('int') || lowerType.includes('bigint') || lowerType.includes('decimal')
      || lowerType.includes('double') || lowerType.includes('float') || lowerType.includes('number')
      || lowerType.includes('numeric')) {
    if (lowerName.includes('id') || lowerName.includes('code')) {
      return 'DIMENSION'
    }
    if (lowerName.includes('amount') || lowerName.includes('total') || lowerName.includes('sum')
        || lowerName.includes('count') || lowerName.includes('price') || lowerName.includes('sales')
        || lowerName.includes('quantity') || lowerName.includes('profit') || lowerName.includes('revenue')) {
      return 'MEASURE'
    }
    if (typeof sampleValue === 'number') {
      if (sampleValue >= 0 && sampleValue <= 100 && lowerName.includes('rate')) {
        return 'MEASURE'
      }
      if (sampleValue > 10000) {
        return 'MEASURE'
      }
    }
    return 'MEASURE'
  }

  if (lowerType.includes('string') || lowerType.includes('varchar') || lowerType.includes('char')
      || lowerType.includes('text')) {
    if (lowerName.includes('province') || lowerName.includes('city') || lowerName.includes('region')
        || lowerName.includes('country') || lowerName.includes('area') || lowerName.includes('address')
        || lowerName.includes('location')) {
      return 'GEO'
    }
    return 'DIMENSION'
  }

  if (lowerType.includes('bool')) {
    return 'DIMENSION'
  }

  return 'UNKNOWN'
}

export const analyzeFields = (sampleData: Record<string, any>[]): FieldInfo[] => {
  if (!sampleData || sampleData.length === 0) return []
  const firstRow = sampleData[0]
  const result: FieldInfo[] = []

  for (const name of Object.keys(firstRow)) {
    const val = firstRow[name]
    const type = typeof val === 'number' ? 'number'
      : val instanceof Date ? 'datetime'
      : typeof val === 'boolean' ? 'bool'
      : 'string'

    const distinct = new Set(sampleData.map(row => row[name]).filter(v => v !== null && v !== undefined))

    result.push({
      name,
      type,
      category: categorizeField(name, type, val),
      distinctCount: distinct.size,
      sampleValue: val
    })
  }

  return result
}

export const recommendChartType = (fields: FieldInfo[]): ChartRecommendationResult => {
  if (!fields || fields.length === 0) {
    return {
      recommendedType: 'table',
      reason: '无可用字段，默认展示表格',
      alternatives: [],
      xFieldCandidates: [],
      yFieldCandidates: [],
      suggestedCardWidth: 800,
      suggestedCardHeight: 400,
      fields: []
    }
  }

  const dimensions = fields.filter(f => f.category === 'DIMENSION' || f.category === 'GEO')
  const measures = fields.filter(f => f.category === 'MEASURE')
  const dates = fields.filter(f => f.category === 'DATE')
  const dimCount = dimensions.length + dates.length
  const measureCount = measures.length

  const xFields = [...dates, ...dimensions]
  let recommended: ChartType
  let reason: string
  let alternatives: ChartType[] = []

  if (measureCount === 0 && dimCount === 1) {
    const singleDim = dates[0] || dimensions[0]
    recommended = 'table'
    reason = `单维度字段 '${singleDim.name}'，建议表格展示`
    alternatives = ['table']
  } else if (measureCount === 1 && dimCount === 0) {
    const measure = measures[0]
    recommended = 'bar'
    reason = `单度量字段 '${measure.name}'，建议 KPI 卡片展示`
    alternatives = ['bar', 'table']
  } else if (measureCount === 1 && dimCount === 1) {
    const dim = xFields[0]
    const measure = measures[0]

    if (dim.category === 'DATE' || dim.type.includes('date')) {
      recommended = 'line'
      reason = `日期维度 '${dim.name}' + 度量 '${measure.name}'，建议折线图展示趋势`
      alternatives = ['line', 'bar']
    } else if (dim.distinctCount <= 8) {
      recommended = 'pie'
      reason = `维度 '${dim.name}'（${dim.distinctCount}个分类） + 度量 '${measure.name}'，建议饼图展示占比`
      alternatives = ['pie', 'bar', 'radar']
    } else if (dim.distinctCount <= 15) {
      recommended = 'bar'
      reason = `维度 '${dim.name}'（${dim.distinctCount}个分类） + 度量 '${measure.name}'，建议柱状图比较`
      alternatives = ['bar', 'pie', 'table']
    } else {
      recommended = 'bar'
      reason = `维度 '${dim.name}'（较多分类） + 度量 '${measure.name}'，建议柱状图`
      alternatives = ['bar', 'table', 'line']
    }
  } else if (measureCount === 1 && dimCount >= 2) {
    recommended = 'radar'
    reason = `多维分析（${dimCount}维度 + ${measureCount}度量），建议雷达图综合比较`
    alternatives = ['radar', 'bar', 'table']
  } else if (measureCount >= 2 && dimCount === 1) {
    const dim = xFields[0]
    if (dim.category === 'DATE') {
      recommended = 'line'
      reason = `日期维度 '${dim.name}' + ${measureCount}个度量，建议折线图多系列对比趋势`
      alternatives = ['line', 'bar']
    } else {
      recommended = 'bar'
      reason = `维度 '${dim.name}' + ${measureCount}个度量，建议柱状图多系列对比`
      alternatives = ['bar', 'line', 'radar']
    }
  } else if (measureCount >= 2 && dimCount >= 2) {
    recommended = 'scatter'
    reason = `${dimCount}维度 + ${measureCount}度量，建议散点图查看相关性`
    alternatives = ['scatter', 'bar', 'table', 'radar']
  } else if (measureCount === 0 && dimCount >= 2) {
    recommended = 'table'
    reason = `${dimCount}个维度字段，建议表格明细展示`
    alternatives = ['table']
  } else {
    recommended = 'table'
    reason = `${fields.length}个字段，默认表格展示`
    alternatives = ['table', 'bar']
  }

  let suggestedCardWidth: number
  let suggestedCardHeight: number
  switch (recommended) {
    case 'pie':
    case 'radar':
      suggestedCardWidth = 420
      suggestedCardHeight = 380
      break
    case 'scatter':
      suggestedCardWidth = 480
      suggestedCardHeight = 360
      break
    case 'table':
      suggestedCardWidth = 800
      suggestedCardHeight = 400
      break
    default:
      suggestedCardWidth = 500
      suggestedCardHeight = 340
      break
  }

  return {
    recommendedType: recommended,
    reason,
    alternatives: alternatives as string[],
    xFieldCandidates: xFields.map(f => f.name),
    yFieldCandidates: measures.map(f => f.name),
    suggestedCardWidth,
    suggestedCardHeight,
    fields
  }
}

export const getFieldCategoryColor = (category: FieldCategory): string => {
  switch (category) {
    case 'MEASURE': return '#5470c6'
    case 'DIMENSION': return '#91cc75'
    case 'DATE': return '#fac858'
    case 'GEO': return '#73c0de'
    default: return '#999'
  }
}

export const getFieldCategoryLabel = (category: FieldCategory): string => {
  switch (category) {
    case 'MEASURE': return '度量'
    case 'DIMENSION': return '维度'
    case 'DATE': return '日期'
    case 'GEO': return '地理'
    default: return '未知'
  }
}

export const getChartTypeLabel = (type: ChartType): string => {
  switch (type) {
    case 'bar': return '柱状图'
    case 'line': return '折线图'
    case 'pie': return '饼图'
    case 'radar': return '雷达图'
    case 'scatter': return '散点图'
    case 'table': return '表格'
    default: return type
  }
}

export const getChartTypeIcon = (type: ChartType): string => {
  switch (type) {
    case 'bar': return '📊'
    case 'line': return '📈'
    case 'pie': return '🥧'
    case 'radar': return '🕸️'
    case 'scatter': return '⚪'
    case 'table': return '📋'
    default: return '📊'
  }
}
