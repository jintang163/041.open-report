import React, { useMemo, useState } from 'react'
import { Table, Empty, Spin, Input, Tag } from 'antd'
import type { TableProps, TablePaginationConfig } from 'antd'
import { usePreviewStore } from '../../store/preview'
import { TableData, TableColumn, parseHtmlTable } from '../../utils/report'
import VirtualScrollTable from '@/components/VirtualScrollTable'

interface ReportTableProps {
  data?: TableData
  height?: number | string
  showSearch?: boolean
}

const ReportTable: React.FC<ReportTableProps> = ({ data, height, showSearch = false }) => {
  const storeData = usePreviewStore((state) => state.reportData)
  const loading = usePreviewStore((state) => state.loading)
  const pageMode = usePreviewStore((state) => state.pageMode)
  const pageData = usePreviewStore((state) => state.pageData)
  const pageColumns = usePreviewStore((state) => state.pageColumns)
  const hasMore = usePreviewStore((state) => state.hasMore)
  const pageLoading = usePreviewStore((state) => state.pageLoading)
  const totalRows = usePreviewStore((state) => state.totalRows)
  const loadMoreData = usePreviewStore((state) => state.loadMoreData)
  const [searchText, setSearchText] = useState('')
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total) => `共 ${total} 条记录`
  })

  const tableData: TableData | undefined = useMemo(() => {
    if (data) return data
    if (storeData?.table) return storeData.table
    if (storeData?.html) {
      const parsed = parseHtmlTable(storeData.html)
      if (parsed) return parsed
    }
    return undefined
  }, [data, storeData])

  const columns = useMemo(() => {
    if (!tableData?.columns) return []
    return tableData.columns.map((col: TableColumn) => ({
      title: (
        <span style={{ whiteSpace: 'nowrap' }}>
          {col.title}
        </span>
      ),
      dataIndex: col.dataIndex,
      key: col.key || col.dataIndex,
      width: col.width,
      align: col.align,
      sorter: col.sorter !== false ? true : false,
      filters: col.filters,
      onFilter: col.filters
        ? (value: any, record: any) => record[col.dataIndex] === value
        : undefined,
      render: col.render
    }))
  }, [tableData])

  const virtualColumns = useMemo(() => {
    if (!pageColumns || pageColumns.length === 0) return []
    return pageColumns.map((col: any, index: number) => ({
      key: col.key || col.dataIndex || col.name || `col_${index}`,
      dataIndex: col.dataIndex || col.name || '',
      title: col.title || col.name || '',
      width: col.width,
      align: col.align
    }))
  }, [pageColumns])

  const filteredDataSource = useMemo(() => {
    if (!tableData?.dataSource || !searchText) return tableData?.dataSource || []
    const lowerSearch = searchText.toLowerCase()
    return tableData.dataSource.filter((record) =>
      Object.values(record).some((value) =>
        String(value).toLowerCase().includes(lowerSearch)
      )
    )
  }, [tableData, searchText])

  if (!tableData || !tableData.columns || tableData.columns.length === 0) {
    if (pageMode && pageColumns.length > 0) {
    } else {
      return (
        <div
          style={{
            background: '#fff',
            borderRadius: 8,
            padding: 40,
            minHeight: height || 300,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <Empty description="暂无数据，请先执行查询" />
        </div>
      )
    }
  }

  const handleTableChange = (newPagination: TablePaginationConfig) => {
    setPagination(newPagination)
  }

  const tableHeight = height ? Number(height) - 120 : 600

  if (pageMode && virtualColumns.length > 0) {
    return (
      <div style={{ background: '#fff', borderRadius: 8, padding: 16 }}>
        <div style={{ marginBottom: 12, display: 'flex', alignItems: 'center', gap: 12 }}>
          <Tag color="blue">大数据模式</Tag>
          <span style={{ color: '#666', fontSize: 13 }}>
            已加载 <b style={{ color: '#1890ff' }}>{pageData.length}</b> 行
            {totalRows > 0 && (
              <span> / 共 <b>{totalRows}</b> 行</span>
            )}
            {hasMore && <span>，滚动加载更多</span>}
          </span>
          {showSearch && (
            <Input.Search
              placeholder="搜索内容..."
              allowClear
              onChange={(e) => setSearchText(e.target.value)}
              style={{ marginLeft: 'auto', maxWidth: 280 }}
            />
          )}
        </div>

        <Spin spinning={loading && pageData.length === 0}>
          <VirtualScrollTable
            columns={virtualColumns}
            dataSource={pageData}
            height={tableHeight}
            rowHeight={42}
            loading={pageLoading}
            hasMore={hasMore}
            onLoadMore={loadMoreData}
            rowKey={(record, index) => String(index)}
            size="middle"
            overscan={10}
          />
        </Spin>
      </div>
    )
  }

  return (
    <div style={{ background: '#fff', borderRadius: 8, padding: 16 }}>
      {showSearch && (
        <Input.Search
          placeholder="搜索内容..."
          allowClear
          onChange={(e) => setSearchText(e.target.value)}
          style={{ marginBottom: 16, maxWidth: 300 }}
        />
      )}
      <Spin spinning={loading}>
        <Table
          columns={columns as TableProps['columns']}
          dataSource={filteredDataSource}
          rowKey={(record, index) => index?.toString() || Math.random().toString()}
          pagination={{
            ...pagination,
            total: filteredDataSource.length,
            onChange: (page, pageSize) =>
              handleTableChange({ ...pagination, current: page, pageSize })
          }}
          scroll={{ x: 'max-content', y: height ? Number(height) - 120 : undefined }}
          size="middle"
          bordered
          onChange={handleTableChange}
        />
      </Spin>
    </div>
  )
}

export default ReportTable
