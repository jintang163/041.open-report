import React, { useEffect, useState } from 'react'
import { Card, Tag, Space, Spin, Alert, Button, Empty, Typography, Divider, Table, Row, Col, Statistic } from 'antd'
import {
  DatabaseOutlined,
  TableOutlined,
  ColumnHeightOutlined,
  FunctionOutlined,
  ReloadOutlined,
  InfoCircleOutlined,
  ApiOutlined
} from '@ant-design/icons'
import { parseSqlLineage, refreshLineageForDataSet } from '@/api/report'
import type { SqlParseResult } from '@/types'

const { Text, Paragraph } = Typography

interface SqlLineagePreviewProps {
  dataSetId: number
  sqlText?: string
  autoParse?: boolean
  showRefresh?: boolean
  onRefresh?: () => void
}

const SqlLineagePreview: React.FC<SqlLineagePreviewProps> = ({
  dataSetId,
  sqlText,
  autoParse = true,
  showRefresh = true,
  onRefresh
}) => {
  const [loading, setLoading] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [parseResult, setParseResult] = useState<SqlParseResult | null>(null)
  const [hasParsed, setHasParsed] = useState(false)

  useEffect(() => {
    if (dataSetId && autoParse) {
      handleParse()
    }
  }, [dataSetId, autoParse])

  const handleParse = async () => {
    if (!dataSetId) return
    setLoading(true)
    setHasParsed(true)
    try {
      const result = await parseSqlLineage(dataSetId)
      setParseResult(result)
    } catch (err: any) {
      console.error('SQL解析失败:', err)
      setParseResult({
        success: false,
        message: err?.message || '解析失败'
      })
    } finally {
      setLoading(false)
    }
  }

  const handleRefreshLineage = async () => {
    if (!dataSetId) return
    setRefreshing(true)
    try {
      await refreshLineageForDataSet(dataSetId)
      await handleParse()
      onRefresh?.()
    } catch (err: any) {
      console.error('刷新血缘失败:', err)
    } finally {
      setRefreshing(false)
    }
  }

  const getTagColor = (type: string) => {
    const colors: Record<string, string> = {
      SUM: 'red',
      COUNT: 'red',
      AVG: 'red',
      MIN: 'orange',
      MAX: 'orange',
      DISTINCT: 'purple',
      GROUP_CONCAT: 'cyan',
      CONCAT: 'blue',
      SUBSTRING: 'green',
      TRIM: 'green',
      UPPER: 'green',
      LOWER: 'green',
      CAST: 'gold',
      DATE_FORMAT: 'gold',
      DATE_ADD: 'gold',
      DATE_SUB: 'gold'
    }
    return colors[type] || 'default'
  }

  const columnColumns = [
    {
      title: '位置',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type: string) => (
        <Tag color={type === 'select' ? 'blue' : 'orange'}>
          {type === 'select' ? 'SELECT' : 'WHERE'}
        </Tag>
      )
    },
    {
      title: '字段名',
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => <Text code>{name}</Text>
    }
  ]

  const getColumnData = () => {
    const data: { type: string; name: string }[] = []
    parseResult?.selectColumns?.forEach(col => {
      if (col !== '*') {
        data.push({ type: 'select', name: col })
      }
    })
    parseResult?.whereColumns?.forEach(col => {
      data.push({ type: 'where', name: col })
    })
    return data
  }

  const renderContent = () => {
    if (!hasParsed) {
      return (
        <Empty
          description="点击解析按钮查看SQL血缘"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        >
          <Button type="primary" icon={<DatabaseOutlined />} onClick={handleParse}>
            解析SQL
          </Button>
        </Empty>
      )
    }

    if (!parseResult) return null

    if (!parseResult.success) {
      return (
        <Alert
          type="error"
          message="SQL解析失败"
          description={parseResult.message}
          showIcon
        />
      )
    }

    return (
      <div>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="涉及表数"
                value={parseResult.tables?.length || 0}
                prefix={<TableOutlined style={{ color: '#fa8c16' }} />}
                valueStyle={{ color: '#fa8c16' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="涉及字段数"
                value={parseResult.columns?.length || 0}
                prefix={<ColumnHeightOutlined style={{ color: '#722ed1' }} />}
                valueStyle={{ color: '#722ed1' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="SELECT字段"
                value={parseResult.selectColumns?.filter(c => c !== '*').length || 0}
                prefix={<ApiOutlined style={{ color: '#1890ff' }} />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="是否聚合"
                value={parseResult.hasAggregation ? '是' : '否'}
                prefix={<FunctionOutlined style={{ color: parseResult.hasAggregation ? '#f5222d' : '#52c41a' }} />}
                valueStyle={{ color: parseResult.hasAggregation ? '#f5222d' : '#52c41a' }}
              />
            </Card>
          </Col>
        </Row>

        {parseResult.tables && parseResult.tables.length > 0 && (
          <>
            <Divider orientation="left" style={{ margin: '12px 0' }}>
              <Space>
                <TableOutlined style={{ color: '#fa8c16' }} />
                <span>涉及的数据库表</span>
              </Space>
            </Divider>
            <Space wrap size={[8, 8]} style={{ marginBottom: 16 }}>
              {parseResult.tables.map((table, idx) => (
                <Tag key={idx} color="orange" icon={<TableOutlined />}>
                  {table}
                </Tag>
              ))}
            </Space>
            {parseResult.mainTable && (
              <Alert
                type="info"
                showIcon
                message={
                  <Space>
                    <InfoCircleOutlined />
                    <span>主表:</span>
                    <Tag color="blue">{parseResult.mainTable}</Tag>
                  </Space>
                }
                style={{ marginBottom: 16 }}
              />
            )}
          </>
        )}

        {parseResult.tableAliases && Object.keys(parseResult.tableAliases).length > 0 && (
          <>
            <Divider orientation="left" style={{ margin: '12px 0' }}>
              <Space>
                <InfoCircleOutlined />
                <span>表别名映射</span>
              </Space>
            </Divider>
            <Space wrap size={[8, 8]} style={{ marginBottom: 16 }}>
              {Object.entries(parseResult.tableAliases).map(([alias, table]) => (
                <Tag key={alias} color="purple">
                  {alias} → {table}
                </Tag>
              ))}
            </Space>
          </>
        )}

        {parseResult.aggregations && parseResult.aggregations.length > 0 && (
          <>
            <Divider orientation="left" style={{ margin: '12px 0' }}>
              <Space>
                <FunctionOutlined style={{ color: '#f5222d' }} />
                <span>使用的聚合函数</span>
              </Space>
            </Divider>
            <Space wrap size={[8, 8]} style={{ marginBottom: 16 }}>
              {parseResult.aggregations.map((func, idx) => (
                <Tag key={idx} color={getTagColor(func)} icon={<FunctionOutlined />}>
                  {func}
                </Tag>
              ))}
            </Space>
          </>
        )}

        {getColumnData().length > 0 && (
          <>
            <Divider orientation="left" style={{ margin: '12px 0' }}>
              <Space>
                <ColumnHeightOutlined style={{ color: '#722ed1' }} />
                <span>涉及的字段列表</span>
              </Space>
            </Divider>
            <Table
              size="small"
              dataSource={getColumnData()}
              columns={columnColumns}
              pagination={false}
              rowKey={(record) => `${record.type}-${record.name}`}
            />
          </>
        )}

        {parseResult.datasourceName && (
          <>
            <Divider orientation="left" style={{ margin: '12px 0' }}>
              <Space>
                <DatabaseOutlined style={{ color: '#13c2c2' }} />
                <span>数据源信息</span>
              </Space>
            </Divider>
            <Space wrap size={[8, 8]}>
              <Tag color="cyan">数据源: {parseResult.datasourceName}</Tag>
              <Tag color="blue">类型: {parseResult.datasourceType}</Tag>
              {parseResult.databaseName && (
                <Tag color="green">数据库: {parseResult.databaseName}</Tag>
              )}
              {parseResult.schemaName && (
                <Tag color="purple">Schema: {parseResult.schemaName}</Tag>
              )}
            </Space>
          </>
        )}

        {parseResult.sqlText && (
          <>
            <Divider orientation="left" style={{ margin: '12px 0' }}>
              <Space>
                <ApiOutlined />
                <span>原始SQL</span>
              </Space>
            </Divider>
            <Paragraph
              copyable
              style={{
                fontFamily: 'monospace',
                backgroundColor: '#f6f8fa',
                padding: 12,
                borderRadius: 4,
                marginBottom: 0,
                maxHeight: 200,
                overflow: 'auto'
              }}
            >
              {parseResult.sqlText}
            </Paragraph>
          </>
        )}
      </div>
    )
  }

  return (
    <Card
      title={
        <Space>
          <DatabaseOutlined style={{ color: '#1890ff' }} />
          <span>SQL血缘分析</span>
        </Space>
      }
      extra={
        <Space>
          {showRefresh && (
            <Button
              size="small"
              icon={<ReloadOutlined spin={refreshing} />}
              onClick={handleRefreshLineage}
            >
              刷新血缘
            </Button>
          )}
          <Button
            size="small"
            type="primary"
            icon={<DatabaseOutlined spin={loading} />}
            onClick={handleParse}
            loading={loading}
          >
            重新解析
          </Button>
        </Space>
      }
      size="small"
    >
      <Spin spinning={loading}>
        {renderContent()}
      </Spin>
    </Card>
  )
}

export default SqlLineagePreview
