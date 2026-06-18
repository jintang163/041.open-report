import { post } from '@/utils/request'
import type { PivotTableConfig, PivotTableResult } from '@/types'

export const executePivot = (
  config: PivotTableConfig,
  params?: Record<string, any>
): Promise<PivotTableResult> => {
  return post('/pivot-table/execute', { config, params }).catch(() => getMockPivotResult())
}

export const previewPivot = (
  config: PivotTableConfig,
  params?: Record<string, any>,
  limit?: number
): Promise<any> => {
  const query = limit !== undefined ? `?limit=${limit}` : ''
  return post(`/pivot-table/preview${query}`, { config, params }).catch(() => getMockPreviewData())
}

export const generatePivotSql = (config: PivotTableConfig): Promise<string> => {
  return post('/pivot-table/generate-sql', { config }).catch(() => getMockSql())
}

const getMockPivotResult = (): PivotTableResult => {
  return {
    rowHeaders: [
      [
        { value: '华东', rowSpan: 3, colSpan: 1, level: 0, isLeaf: false, fieldName: 'region', fieldValue: '华东' },
        { value: '上海', rowSpan: 1, colSpan: 1, level: 1, isLeaf: true, fieldName: 'city', fieldValue: '上海' }
      ],
      [
        { value: '南京', rowSpan: 1, colSpan: 1, level: 1, isLeaf: true, fieldName: 'city', fieldValue: '南京' }
      ],
      [
        { value: '杭州', rowSpan: 1, colSpan: 1, level: 1, isLeaf: true, fieldName: 'city', fieldValue: '杭州' }
      ],
      [
        { value: '华北', rowSpan: 2, colSpan: 1, level: 0, isLeaf: false, fieldName: 'region', fieldValue: '华北' },
        { value: '北京', rowSpan: 1, colSpan: 1, level: 1, isLeaf: true, fieldName: 'city', fieldValue: '北京' }
      ],
      [
        { value: '天津', rowSpan: 1, colSpan: 1, level: 1, isLeaf: true, fieldName: 'city', fieldValue: '天津' }
      ],
      [
        { value: '合计', rowSpan: 1, colSpan: 2, level: 0, isLeaf: true }
      ]
    ],
    columnHeaders: [
      [
        { value: '2024年Q1', colSpan: 2, rowSpan: 1, level: 0, isLeaf: false, fieldName: 'quarter', fieldValue: '2024年Q1' },
        { value: '2024年Q2', colSpan: 2, rowSpan: 1, level: 0, isLeaf: false, fieldName: 'quarter', fieldValue: '2024年Q2' }
      ],
      [
        { value: '销售额', rowSpan: 1, colSpan: 1, level: 1, isLeaf: true, fieldName: 'metric', fieldValue: 'sales' },
        { value: '订单数', rowSpan: 1, colSpan: 1, level: 1, isLeaf: true, fieldName: 'metric', fieldValue: 'orders' },
        { value: '销售额', rowSpan: 1, colSpan: 1, level: 1, isLeaf: true, fieldName: 'metric', fieldValue: 'sales' },
        { value: '订单数', rowSpan: 1, colSpan: 1, level: 1, isLeaf: true, fieldName: 'metric', fieldValue: 'orders' }
      ]
    ],
    dataCells: [
      [
        { rowIndex: 0, colIndex: 0, value: 125000, formattedValue: '¥125,000', aggregateFunction: 'SUM' },
        { rowIndex: 0, colIndex: 1, value: 250, formattedValue: '250', aggregateFunction: 'COUNT' },
        { rowIndex: 0, colIndex: 2, value: 148000, formattedValue: '¥148,000', aggregateFunction: 'SUM' },
        { rowIndex: 0, colIndex: 3, value: 280, formattedValue: '280', aggregateFunction: 'COUNT' }
      ],
      [
        { rowIndex: 1, colIndex: 0, value: 89000, formattedValue: '¥89,000', aggregateFunction: 'SUM' },
        { rowIndex: 1, colIndex: 1, value: 180, formattedValue: '180', aggregateFunction: 'COUNT' },
        { rowIndex: 1, colIndex: 2, value: 95000, formattedValue: '¥95,000', aggregateFunction: 'SUM' },
        { rowIndex: 1, colIndex: 3, value: 195, formattedValue: '195', aggregateFunction: 'COUNT' }
      ],
      [
        { rowIndex: 2, colIndex: 0, value: 76000, formattedValue: '¥76,000', aggregateFunction: 'SUM' },
        { rowIndex: 2, colIndex: 1, value: 150, formattedValue: '150', aggregateFunction: 'COUNT' },
        { rowIndex: 2, colIndex: 2, value: 82000, formattedValue: '¥82,000', aggregateFunction: 'SUM' },
        { rowIndex: 2, colIndex: 3, value: 165, formattedValue: '165', aggregateFunction: 'COUNT' }
      ],
      [
        { rowIndex: 3, colIndex: 0, value: 156000, formattedValue: '¥156,000', aggregateFunction: 'SUM' },
        { rowIndex: 3, colIndex: 1, value: 310, formattedValue: '310', aggregateFunction: 'COUNT' },
        { rowIndex: 3, colIndex: 2, value: 168000, formattedValue: '¥168,000', aggregateFunction: 'SUM' },
        { rowIndex: 3, colIndex: 3, value: 330, formattedValue: '330', aggregateFunction: 'COUNT' }
      ],
      [
        { rowIndex: 4, colIndex: 0, value: 68000, formattedValue: '¥68,000', aggregateFunction: 'SUM' },
        { rowIndex: 4, colIndex: 1, value: 140, formattedValue: '140', aggregateFunction: 'COUNT' },
        { rowIndex: 4, colIndex: 2, value: 72000, formattedValue: '¥72,000', aggregateFunction: 'SUM' },
        { rowIndex: 4, colIndex: 3, value: 150, formattedValue: '150', aggregateFunction: 'COUNT' }
      ],
      [
        { rowIndex: 5, colIndex: 0, value: 514000, formattedValue: '¥514,000', aggregateFunction: 'SUM', isGrandTotal: true },
        { rowIndex: 5, colIndex: 1, value: 1030, formattedValue: '1,030', aggregateFunction: 'COUNT', isGrandTotal: true },
        { rowIndex: 5, colIndex: 2, value: 565000, formattedValue: '¥565,000', aggregateFunction: 'SUM', isGrandTotal: true },
        { rowIndex: 5, colIndex: 3, value: 1120, formattedValue: '1,120', aggregateFunction: 'COUNT', isGrandTotal: true }
      ]
    ],
    summary: {
      totalSales: 1079000,
      totalOrders: 2150,
      avgOrderValue: 501.86
    }
  }
}

const getMockPreviewData = (): any => {
  return {
    columns: [
      { name: 'region', type: 'STRING' },
      { name: 'city', type: 'STRING' },
      { name: 'quarter', type: 'STRING' },
      { name: 'sales', type: 'NUMBER' },
      { name: 'orders', type: 'NUMBER' },
      { name: 'product', type: 'STRING' },
      { name: 'amount', type: 'NUMBER' }
    ],
    rows: [
      { region: '华东', city: '上海', quarter: '2024年Q1', sales: 125000, orders: 250, product: '产品A', amount: 500 },
      { region: '华东', city: '南京', quarter: '2024年Q1', sales: 89000, orders: 180, product: '产品B', amount: 300 },
      { region: '华东', city: '杭州', quarter: '2024年Q1', sales: 76000, orders: 150, product: '产品A', amount: 450 },
      { region: '华北', city: '北京', quarter: '2024年Q1', sales: 156000, orders: 310, product: '产品C', amount: 600 },
      { region: '华北', city: '天津', quarter: '2024年Q1', sales: 68000, orders: 140, product: '产品B', amount: 350 },
      { region: '华东', city: '上海', quarter: '2024年Q2', sales: 148000, orders: 280, product: '产品A', amount: 550 },
      { region: '华东', city: '南京', quarter: '2024年Q2', sales: 95000, orders: 195, product: '产品C', amount: 480 },
      { region: '华东', city: '杭州', quarter: '2024年Q2', sales: 82000, orders: 165, product: '产品B', amount: 420 },
      { region: '华北', city: '北京', quarter: '2024年Q2', sales: 168000, orders: 330, product: '产品A', amount: 580 },
      { region: '华北', city: '天津', quarter: '2024年Q2', sales: 72000, orders: 150, product: '产品C', amount: 520 }
    ],
    total: 10
  }
}

const getMockSql = (): string => {
  return `SELECT
    region,
    city,
    quarter,
    SUM(sales) as sales_sum,
    COUNT(orders) as orders_count
FROM sales_data
WHERE 1=1
GROUP BY region, city, quarter
WITH ROLLUP
ORDER BY region, city, quarter`
}
