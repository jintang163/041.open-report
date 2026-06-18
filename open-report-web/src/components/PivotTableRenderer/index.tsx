import React, { useMemo } from 'react'
import type { PivotTableResult, PivotHeaderCell, PivotDataCell } from '@/types'
import { Empty } from 'antd'

interface PivotTableRendererProps {
  data?: PivotTableResult
  height?: number | string
}

const PivotTableRenderer: React.FC<PivotTableRendererProps> = ({ data, height = 500 }) => {
  const tableData = useMemo(() => {
    if (!data) return null

    const { rowHeaders, columnHeaders, dataCells } = data

    const processedRowHeaders = rowHeaders.map(row => row.map(cell => ({ ...cell })))

    for (let rowIdx = 0; rowIdx < processedRowHeaders.length; rowIdx++) {
      const row = processedRowHeaders[rowIdx]
      for (let colIdx = 0; colIdx < row.length; colIdx++) {
        const cell = row[colIdx]
        if (cell.rowSpan && cell.rowSpan > 1) {
          for (let i = 1; i < cell.rowSpan; i++) {
            if (processedRowHeaders[rowIdx + i]) {
              if (processedRowHeaders[rowIdx + i][colIdx] === undefined) {
                processedRowHeaders[rowIdx + i][colIdx] = { value: null, rowSpan: 0, colSpan: 0 }
              }
            }
          }
        }
      }
    }

    const colHeaderRows = columnHeaders.map((row, rowIdx) => {
      const cells: React.ReactNode[] = []
      const rowHeaderCellCount = processedRowHeaders[0]?.length || 0

      if (rowIdx === columnHeaders.length - 1) {
        for (let i = 0; i < rowHeaderCellCount; i++) {
          cells.push(
            <th
              key={`empty-${i}`}
              style={{
                position: 'sticky',
                left: i * 120,
                zIndex: 3,
                background: '#fafafa',
                border: '1px solid #f0f0f0',
                padding: '8px 12px',
                fontWeight: 600,
                minWidth: 120
              }}
            />
          )
        }
      } else {
        cells.push(
          <th
            key="corner"
            colSpan={rowHeaderCellCount}
            style={{
              position: 'sticky',
              left: 0,
              zIndex: 3,
              background: '#fafafa',
              border: '1px solid #f0f0f0',
              padding: '8px 12px',
              fontWeight: 600
            }}
          />
        )
      }

      row.forEach((cell: PivotHeaderCell, colIdx: number) => {
        cells.push(
          <th
            key={`col-${rowIdx}-${colIdx}`}
            colSpan={cell.colSpan || 1}
            rowSpan={cell.rowSpan || 1}
            style={{
              background: '#fafafa',
              border: '1px solid #f0f0f0',
              padding: '8px 12px',
              fontWeight: 600,
              textAlign: 'center',
              minWidth: 100,
              whiteSpace: 'nowrap'
            }}
          >
            {cell.value}
          </th>
        )
      })

      return <tr key={`col-header-${rowIdx}`}>{cells}</tr>
    })

    const dataRows = processedRowHeaders.map((rowHeaderRow, rowIdx) => {
      const cells: React.ReactNode[] = []

      rowHeaderRow.forEach((cell: PivotHeaderCell, colIdx: number) => {
        if (cell.rowSpan === 0 && cell.colSpan === 0) return

        const isGrandTotal = cell.value === '合计' || cell.value === '总计'
        const isSubtotal = !isGrandTotal && (cell.isLeaf === false || cell.value?.includes('小计'))

        const cellStyle: React.CSSProperties = {
          position: 'sticky',
          left: colIdx * 120,
          zIndex: 2,
          background: isGrandTotal ? '#e6f4ff' : isSubtotal ? '#f0f5ff' : '#fff',
          border: '1px solid #f0f0f0',
          padding: '8px 12px',
          fontWeight: isGrandTotal || isSubtotal ? 'bold' : 'normal',
          minWidth: 120,
          whiteSpace: 'nowrap'
        }

        cells.push(
          <th
            key={`row-${rowIdx}-${colIdx}`}
            rowSpan={cell.rowSpan || 1}
            colSpan={cell.colSpan || 1}
            style={cellStyle}
          >
            {cell.value}
          </th>
        )
      })

      const dataRow = dataCells[rowIdx]
      if (dataRow) {
        dataRow.forEach((cell: PivotDataCell, colIdx: number) => {
          const cellStyle: React.CSSProperties = {
            background: cell.isGrandTotal ? '#e6f4ff' : cell.isSubtotal ? '#f0f5ff' : '#fff',
            border: '1px solid #f0f0f0',
            padding: '8px 12px',
            textAlign: 'right',
            fontWeight: cell.isGrandTotal || cell.isSubtotal ? 'bold' : 'normal',
            whiteSpace: 'nowrap'
          }

          cells.push(
            <td
              key={`data-${rowIdx}-${colIdx}`}
              style={cellStyle}
            >
              {cell.formattedValue ?? cell.value}
            </td>
          )
        })
      }

      return <tr key={`data-row-${rowIdx}`}>{cells}</tr>
    })

    return { colHeaderRows, dataRows }
  }, [data])

  if (!data || !tableData) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height, background: '#fafafa', borderRadius: 8 }}>
        <Empty description="暂无数据，请先配置交叉报表参数" />
      </div>
    )
  }

  return (
    <div
      style={{
        overflow: 'auto',
        height,
        border: '1px solid #f0f0f0',
        borderRadius: 8
      }}
    >
      <table
        style={{
          borderCollapse: 'collapse',
          width: 'max-content',
          minWidth: '100%',
          fontSize: 14
        }}
      >
        <thead style={{ position: 'sticky', top: 0, zIndex: 4 }}>
          {tableData.colHeaderRows}
        </thead>
        <tbody>
          {tableData.dataRows}
        </tbody>
      </table>
    </div>
  )
}

export default PivotTableRenderer
