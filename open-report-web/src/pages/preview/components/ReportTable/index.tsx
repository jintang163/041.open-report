import React, { useMemo, useState } from 'react'
import { Table, Empty, Spin, Input } from 'antd'
import type { TableProps, TablePaginationConfig } from 'antd'
import { usePreviewStore } from '../../store/preview'
import { TableData, TableColumn, parseHtmlTable } from '../../utils/report'

interface ReportTableProps {
  data?: TableData
  height?: number | string
  showSearch?: boolean
}

const ReportTable: React.FC<ReportTableProps> = ({ data, height, showSearch = false }) => {
  const storeData = usePreviewStore((state) => state.reportData)
  const loading = usePreviewStore((state) => state.loading)
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

  const handleTableChange = (newPagination: TablePaginationConfig) => {
    setPagination(newPagination)
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
