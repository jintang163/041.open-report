import React, { useRef, useState, useEffect, useCallback, useMemo } from 'react'
import { Spin, Empty } from 'antd'
import { LoadingOutlined } from '@ant-design/icons'

export interface VirtualTableColumn {
  key: string
  dataIndex?: string
  title: string
  width?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right'
  render?: (value: any, record: any, index: number) => React.ReactNode
}

export interface VirtualTableProps {
  columns: VirtualTableColumn[]
  dataSource: any[]
  height?: number
  rowHeight?: number
  loading?: boolean
  hasMore?: boolean
  onLoadMore?: () => void
  rowKey?: string | ((record: any, index: number) => string)
  onScroll?: (e: React.UIEvent<HTMLDivElement>) => void
  showHeader?: boolean
  bordered?: boolean
  size?: 'small' | 'middle' | 'large'
  overscan?: number
}

const ROW_HEIGHT_MAP = {
  small: 32,
  middle: 42,
  large: 54
}

const VirtualScrollTable: React.FC<VirtualTableProps> = ({
  columns,
  dataSource,
  height = 500,
  rowHeight,
  loading = false,
  hasMore = false,
  onLoadMore,
  rowKey = 'id',
  onScroll,
  showHeader = true,
  bordered = true,
  size = 'middle',
  overscan = 10
}) => {
  const scrollRef = useRef<HTMLDivElement>(null)
  const [scrollTop, setScrollTop] = useState(0)
  const loadMoreTriggered = useRef(false)

  const actualRowHeight = rowHeight || ROW_HEIGHT_MAP[size]

  const totalHeight = dataSource.length * actualRowHeight

  const visibleCount = Math.ceil(height / actualRowHeight) + overscan * 2

  const startIndex = Math.max(0, Math.floor(scrollTop / actualRowHeight) - overscan)
  const endIndex = Math.min(dataSource.length, startIndex + visibleCount)

  const visibleData = useMemo(() => {
    return dataSource.slice(startIndex, endIndex).map((item, i) => ({
      index: startIndex + i,
      data: item
    }))
  }, [dataSource, startIndex, endIndex])

  const offsetY = startIndex * actualRowHeight

  const getRowKey = useCallback(
    (record: any, index: number): string => {
      if (typeof rowKey === 'function') {
        return rowKey(record, index)
      }
      const keyVal = record[rowKey]
      return keyVal != null ? String(keyVal) : String(index)
    },
    [rowKey]
  )

  const handleScroll = useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      const target = e.target as HTMLDivElement
      setScrollTop(target.scrollTop)

      if (onScroll) {
        onScroll(e)
      }

      const bottomOffset = target.scrollHeight - target.scrollTop - target.clientHeight
      if (bottomOffset < 100 && hasMore && !loading && onLoadMore && !loadMoreTriggered.current) {
        loadMoreTriggered.current = true
        onLoadMore()
        setTimeout(() => {
          loadMoreTriggered.current = false
        }, 300)
      }
    },
    [hasMore, loading, onLoadMore, onScroll]
  )

  const getColWidth = (col: VirtualTableColumn, index: number): string => {
    if (col.width != null) {
      return typeof col.width === 'number' ? `${col.width}px` : col.width
    }
    const flexCols = columns.filter(c => c.width == null)
    if (flexCols.length > 0) {
      return `${100 / flexCols.length}%`
    }
    return 'auto'
  }

  const renderCell = (col: VirtualTableColumn, record: any, rowIndex: number) => {
    const value = col.dataIndex != null ? record[col.dataIndex] : undefined
    if (col.render) {
      return col.render(value, record, rowIndex)
    }
    return value != null ? String(value) : ''
  }

  const sizePadding = {
    small: '4px 8px',
    middle: '8px 12px',
    large: '12px 16px'
  }[size]

  if (!dataSource || dataSource.length === 0) {
    return (
      <div
        style={{
          height,
          background: '#fff',
          border: bordered ? '1px solid #f0f0f0' : 'none',
          borderRadius: 8,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          overflow: 'hidden'
        }}
      >
        {loading ? <Spin /> : <Empty description="暂无数据" />}
      </div>
    )
  }

  return (
    <div
      style={{
        height,
        background: '#fff',
        border: bordered ? '1px solid #f0f0f0' : 'none',
        borderRadius: 8,
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column'
      }}
    >
      {showHeader && (
        <div
          style={{
            display: 'flex',
            background: '#fafafa',
            borderBottom: bordered ? '1px solid #f0f0f0' : 'none',
            fontWeight: 600,
            color: '#000000d9',
            flexShrink: 0
          }}
        >
          {columns.map((col, index) => (
            <div
              key={col.key || index}
              style={{
                width: getColWidth(col, index),
                padding: sizePadding,
                textAlign: col.align || 'left',
                flexShrink: 0,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap'
              }}
            >
              {col.title}
            </div>
          ))}
        </div>
      )}

      <div
        ref={scrollRef}
        style={{
          flex: 1,
          overflowY: 'auto',
          overflowX: 'hidden',
          position: 'relative'
        }}
        onScroll={handleScroll}
      >
        <div style={{ height: totalHeight, position: 'relative' }}>
          <div
            style={{
              position: 'absolute',
              top: offsetY,
              left: 0,
              right: 0,
              transform: 'translateZ(0)'
            }}
          >
            {visibleData.map(({ index, data }) => (
              <div
                key={getRowKey(data, index)}
                style={{
                  height: actualRowHeight,
                  display: 'flex',
                  alignItems: 'center',
                  borderBottom: bordered ? '1px solid #f0f0f0' : 'none',
                  boxSizing: 'border-box',
                  background: index % 2 === 0 ? '#fff' : '#fafafa',
                  transition: 'background 0.2s'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#e6f7ff'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = index % 2 === 0 ? '#fff' : '#fafafa'
                }}
              >
                {columns.map((col, colIndex) => (
                  <div
                    key={col.key || colIndex}
                    style={{
                      width: getColWidth(col, colIndex),
                      padding: sizePadding,
                      textAlign: col.align || 'left',
                      flexShrink: 0,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap'
                    }}
                    title={String(col.dataIndex ? data[col.dataIndex] ?? '' : '')}
                  >
                    {renderCell(col, data, index)}
                  </div>
                ))}
              </div>
            ))}
          </div>
        </div>

        {(loading || hasMore) && (
          <div
            style={{
              textAlign: 'center',
              padding: '12px 0',
              color: '#999',
              fontSize: 13
            }}
          >
            {loading ? (
              <span>
                <LoadingOutlined style={{ marginRight: 8 }} />
                加载中...
              </span>
            ) : hasMore ? (
              <span>上拉加载更多</span>
            ) : null}
          </div>
        )}
      </div>
    </div>
  )
}

export default VirtualScrollTable
