import React, { useEffect, useState } from 'react'
import { Modal, Descriptions, Tag, Space, Spin, Empty, Steps, Typography, Card, Alert } from 'antd'
import {
  DatabaseOutlined,
  TableOutlined,
  ApiOutlined,
  FileTextOutlined,
  FunctionOutlined
} from '@ant-design/icons'
import { getLineageTrace } from '@/api/report'
import type { LineageTraceResult, LineageTraceNode } from '@/types'

const { Text, Paragraph } = Typography

interface LineageTraceModalProps {
  visible: boolean
  reportId: number
  reportField: string
  fieldTitle?: string
  onClose: () => void
}

const LineageTraceModal: React.FC<LineageTraceModalProps> = ({
  visible,
  reportId,
  reportField,
  fieldTitle,
  onClose
}) => {
  const [loading, setLoading] = useState(false)
  const [traceResult, setTraceResult] = useState<LineageTraceResult | null>(null)

  useEffect(() => {
    if (visible && reportId && reportField) {
      loadTrace()
    } else {
      setTraceResult(null)
    }
  }, [visible, reportId, reportField])

  const loadTrace = async () => {
    setLoading(true)
    try {
      const result = await getLineageTrace(reportId, reportField)
      setTraceResult(result)
    } catch (err: any) {
      console.error('加载血缘追溯失败:', err)
      setTraceResult({
        success: false,
        message: err?.message || '加载失败'
      })
    } finally {
      setLoading(false)
    }
  }

  const getStepIcon = (type: string) => {
    switch (type) {
      case 'report':
        return <FileTextOutlined style={{ color: '#1890ff', fontSize: 20 }} />
      case 'dataSet':
        return <ApiOutlined style={{ color: '#52c41a', fontSize: 20 }} />
      case 'database':
        return <DatabaseOutlined style={{ color: '#fa8c16', fontSize: 20 }} />
      default:
        return <FileTextOutlined />
    }
  }

  const getStepTitle = (node: LineageTraceNode) => {
    const icon = getStepIcon(node.type)
    return (
      <Space>
        {icon}
        <span style={{ fontWeight: 500 }}>{node.name}</span>
        {node.title && node.title !== node.name && (
          <Text type="secondary">({node.title})</Text>
        )}
      </Space>
    )
  }

  const getStepDescription = (node: LineageTraceNode) => {
    const items: React.ReactNode[] = []

    if (node.type === 'report') {
      if (node.field) {
        items.push(
          <Tag key="field" color="blue">
            字段: {node.field}
          </Tag>
        )
      }
    }

    if (node.type === 'dataSet') {
      if (node.field) {
        items.push(
          <Tag key="dataset-field" color="green">
            数据集字段: {node.field}
          </Tag>
        )
      }
      if (node.lineageType) {
        const tagColor = node.lineageType === 'DIRECT' ? 'default' :
          node.lineageType === 'AGGREGATION' ? 'red' : 'gold'
        const tagText = node.lineageType === 'DIRECT' ? '直接映射' :
          node.lineageType === 'AGGREGATION' ? '聚合计算' : '表达式计算'
        items.push(
          <Tag key="lineage-type" color={tagColor}>
            {tagText}
          </Tag>
        )
      }
      if (node.expression && node.expression !== node.field) {
        items.push(
          <Tag key="expression" icon={<FunctionOutlined />} color="purple">
            {node.expression}
          </Tag>
        )
      }
    }

    if (node.type === 'database') {
      if (node.datasourceName) {
        items.push(
          <Tag key="datasource" color="orange">
            数据源: {node.datasourceName}
          </Tag>
        )
      }
      if (node.databaseName) {
        items.push(
          <Tag key="database" color="cyan">
            数据库: {node.databaseName}
          </Tag>
        )
      }
      if (node.tableName) {
        items.push(
          <Tag key="table" icon={<TableOutlined />} color="orange">
            表: {node.tableName}
          </Tag>
        )
      }
      if (node.columnName) {
        items.push(
          <Tag key="column" color="purple">
            字段: {node.columnName}
          </Tag>
        )
      }
    }

    return <Space wrap>{items}</Space>
  }

  const getStepStatus = (index: number, total: number) => {
    if (index < total - 1) return 'finish'
    if (index === total - 1) return 'finish'
    return 'wait'
  }

  const renderTraceContent = () => {
    if (!traceResult) return null

    if (!traceResult.success) {
      return (
        <Alert
          type="error"
          message="血缘追溯失败"
          description={traceResult.message || '未找到该字段的血缘关系'}
          showIcon
        />
      )
    }

    const trace = traceResult.trace
    if (!trace || trace.length === 0) {
      return <Empty description="暂无血缘追溯数据" />
    }

    const steps = trace.map((node, index) => ({
      title: getStepTitle(node),
      description: getStepDescription(node),
      status: getStepStatus(index, trace.length)
    }))

    return (
      <div>
        <Steps
          direction="vertical"
          current={trace.length}
          items={steps as any}
          style={{ marginBottom: 24 }}
        />

        {traceResult.lineage?.sqlText && (
          <Card
            title={
              <Space>
                <FunctionOutlined />
                <span>SQL 语句</span>
              </Space>
            }
            size="small"
          >
            <Paragraph
              copyable
              style={{
                fontFamily: 'monospace',
                backgroundColor: '#f6f8fa',
                padding: 12,
                borderRadius: 4,
                marginBottom: 0
              }}
            >
              {traceResult.lineage.sqlText}
            </Paragraph>
          </Card>
        )}

        {traceResult.lineage && (
          <Descriptions
            title="血缘详情"
            bordered
            size="small"
            column={2}
            style={{ marginTop: 16 }}
          >
            <Descriptions.Item label="报表">
              {traceResult.lineage.reportName}
            </Descriptions.Item>
            <Descriptions.Item label="报表字段">
              {traceResult.lineage.reportField}
              {traceResult.lineage.reportFieldTitle &&
                traceResult.lineage.reportFieldTitle !== traceResult.lineage.reportField && (
                  <Text type="secondary">
                    {' '}({traceResult.lineage.reportFieldTitle})
                  </Text>
                )}
            </Descriptions.Item>
            <Descriptions.Item label="数据集">
              {traceResult.lineage.dataSetName}
            </Descriptions.Item>
            <Descriptions.Item label="数据集字段">
              {traceResult.lineage.dataSetField}
            </Descriptions.Item>
            <Descriptions.Item label="血缘类型">
              <Tag color={
                traceResult.lineage.lineageType === 'DIRECT' ? 'default' :
                traceResult.lineage.lineageType === 'AGGREGATION' ? 'red' : 'gold'
              }>
                {traceResult.lineage.lineageType === 'DIRECT' ? '直接映射' :
                 traceResult.lineage.lineageType === 'AGGREGATION' ? '聚合计算' : '表达式计算'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="表达式">
              <Text code>{traceResult.lineage.expression || '-'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="数据库表" span={2}>
              <Space>
                {traceResult.lineage.datasourceName && (
                  <Tag color="orange">{traceResult.lineage.datasourceName}</Tag>
                )}
                {traceResult.lineage.databaseName && (
                  <Tag color="cyan">{traceResult.lineage.databaseName}</Tag>
                )}
                {traceResult.lineage.tableName && (
                  <Tag icon={<TableOutlined />} color="orange">
                    {traceResult.lineage.tableName}
                  </Tag>
                )}
                {traceResult.lineage.columnName && (
                  <Tag color="purple">{traceResult.lineage.columnName}</Tag>
                )}
              </Space>
            </Descriptions.Item>
          </Descriptions>
        )}
      </div>
    )
  }

  return (
    <Modal
      title={
        <Space>
          <DatabaseOutlined style={{ color: '#1890ff' }} />
          <span>字段血缘追溯</span>
          {fieldTitle && <Text type="secondary">- {fieldTitle}</Text>}
          {reportField && <Tag color="blue">{reportField}</Tag>}
        </Space>
      }
      open={visible}
      onCancel={onClose}
      width={720}
      footer={null}
      destroyOnClose
    >
      <Spin spinning={loading}>
        {renderTraceContent()}
      </Spin>
    </Modal>
  )
}

export default LineageTraceModal
