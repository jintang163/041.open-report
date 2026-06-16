import React, { useMemo, useState } from 'react'
import { Card, Empty, Spin, List, Tag, Pagination, Input, Divider } from 'antd'
import { usePreviewStore } from '../../store/preview'
import { TableData, TableColumn, parseHtmlTable, ChartConfig } from '../../utils/report'
import ReportChart from '../ReportChart'

interface MobileReportViewProps {
  tableData?: TableData
  charts?: ChartConfig[]
}

const MobileReportView: React.FC<MobileReportViewProps> = ({ tableData, charts }) => {
  const storeData = usePreviewStore((state) => state.reportData)
  const loading = usePreviewStore((state) => state.loading)
  const [searchText, setSearchText] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  const data: TableData | undefined = useMemo(() => {
    if (tableData) return tableData
    if (storeData?.table) return storeData.table
    if (storeData?.html) {
      const parsed = parseHtmlTable(storeData.html)
      if (parsed) return parsed
    }
    return undefined
  }, [tableData, storeData])

  const filteredData = useMemo(() => {
    if (!data?.dataSource || !searchText) return data?.dataSource || []
    const lowerSearch = searchText.toLowerCase()
    return data.dataSource.filter((record) =>
      Object.values(record).some((value) =>
        String(value).toLowerCase().includes(lowerSearch)
      )
    )
  }, [data, searchText])

  const paginatedData = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize
    return filteredData.slice(startIndex, startIndex + pageSize)
  }, [filteredData, currentPage, pageSize])

  const renderCard = (record: any, index: number) => {
    if (!data?.columns) return null
    const columns = data.columns

    return (
      <Card
        key={index}
        size="small"
        style={{ marginBottom: 12, borderRadius: 8 }}
        bodyStyle={{ padding: 12 }}
      >
        <List
          size="small"
          dataSource={columns}
          renderItem={(col: TableColumn, colIndex) => {
            const value = record[col.dataIndex]
            return (
              <List.Item
                key={col.dataIndex}
                style={{
                  padding: '6px 0',
                  borderBottom: colIndex < columns.length - 1 ? '1px solid #f0f0f0' : 'none'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', width: '100%', gap: 8 }}>
                  <span style={{ color: '#666', flexShrink: 0, fontWeight: 500 }}>
                    {col.title}
                  </span>
                  <span style={{ color: '#333', textAlign: 'right', wordBreak: 'break-all' }}>
                    {value !== undefined && value !== null && value !== '' ? (
                      String(value)
                    ) : (
                      <Tag color="default">--</Tag>
                    )}
                  </span>
                </div>
              </List.Item>
            )
          }}
        />
      </Card>
    )
  }

  if (!data && (!charts || charts.length === 0) && (!storeData?.charts || storeData.charts.length === 0)) {
    return (
      <Card style={{ borderRadius: 8 }}>
        <div style={{ padding: 40, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Empty description="暂无数据，请先执行查询" />
        </div>
      </Card>
    )
  }

  return (
    <Spin spinning={loading}>
      {(charts || storeData?.charts) && (
        <>
          <ReportChart charts={charts || storeData?.charts} height={280} />
          <Divider style={{ margin: '8px 0 16px' }} />
        </>
      )}

      {data && data.columns && data.columns.length > 0 && (
        <Card style={{ borderRadius: 8 }} bodyStyle={{ padding: 12 }}>
          <Input.Search
            placeholder="搜索内容..."
            allowClear
            onChange={(e) => {
              setSearchText(e.target.value)
              setCurrentPage(1)
            }}
            style={{ marginBottom: 12 }}
          />

          {paginatedData.length > 0 ? (
            <>
              {paginatedData.map((record, index) => renderCard(record, (currentPage - 1) * pageSize + index))}
              <div style={{ display: 'flex', justifyContent: 'center', marginTop: 16 }}>
                <Pagination
                  current={currentPage}
                  pageSize={pageSize}
                  total={filteredData.length}
                  showSizeChanger={false}
                  showTotal={(total) => `共 ${total} 条`}
                  onChange={(page, size) => {
                    setCurrentPage(page)
                    setPageSize(size)
                  }}
                />
              </div>
            </>
          ) : (
            <Empty description="没有匹配的数据" style={{ padding: 20 }} />
          )}
        </Card>
      )}
    </Spin>
  )
}

export default MobileReportView
