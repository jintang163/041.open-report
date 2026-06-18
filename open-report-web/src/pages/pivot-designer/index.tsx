import React, { useState, useEffect } from 'react'
import {
  Card,
  Select,
  Button,
  Checkbox,
  Space,
  Tag,
  Row,
  Col,
  Typography,
  message,
  Tooltip,
  Modal,
  Empty,
  Radio,
  Divider
} from 'antd'
import {
  PlayCircleOutlined,
  DeleteOutlined,
  ReloadOutlined,
  PlusOutlined,
  DatabaseOutlined,
  ApiOutlined,
  SettingOutlined,
  CodeOutlined
} from '@ant-design/icons'
import type { DataSet, PivotTableConfig, PivotField, PivotFieldType, AggregateFunction, PivotTableResult } from '@/types'
import { getDatasetAll, getDatasetPreview } from '@/api/dataset'
import { executePivot, generatePivotSql } from '@/api/pivot'
import { useDragDrop, type DragItem } from '@/hooks/useDragDrop'
import PivotTableRenderer from '@/components/PivotTableRenderer'

const { Title, Text } = Typography

interface DatasetField {
  name: string
  type: string
}

const mockDatasets: DataSet[] = [
  { id: 1, name: '销售数据', code: 'sales_data', datasourceId: 1, datasourceName: '生产库-MySQL', sql: 'SELECT * FROM sales_data', status: 1 },
  { id: 2, name: '用户行为数据', code: 'user_behavior', datasourceId: 1, datasourceName: '生产库-MySQL', sql: 'SELECT * FROM user_behavior', status: 1 },
  { id: 3, name: '库存数据', code: 'inventory', datasourceId: 2, datasourceName: '测试库-PostgreSQL', sql: 'SELECT * FROM inventory', status: 1 }
]

const aggregateFunctionOptions: { label: string; value: AggregateFunction }[] = [
  { label: '求和 (SUM)', value: 'SUM' },
  { label: '计数 (COUNT)', value: 'COUNT' },
  { label: '平均值 (AVG)', value: 'AVG' },
  { label: '最大值 (MAX)', value: 'MAX' },
  { label: '最小值 (MIN)', value: 'MIN' }
]

const PivotDesigner: React.FC = () => {
  const [datasets, setDatasets] = useState<DataSet[]>([])
  const [selectedDatasetId, setSelectedDatasetId] = useState<number | null>(null)
  const [fields, setFields] = useState<DatasetField[]>([])
  const [loading, setLoading] = useState(false)
  const [executing, setExecuting] = useState(false)
  const [pivotResult, setPivotResult] = useState<PivotTableResult | undefined>(undefined)

  const [rowFields, setRowFields] = useState<PivotField[]>([])
  const [columnFields, setColumnFields] = useState<PivotField[]>([])
  const [valueFields, setValueFields] = useState<PivotField[]>([])

  const [showSubtotal, setShowSubtotal] = useState(true)
  const [showGrandTotal, setShowGrandTotal] = useState(true)
  const [subtotalPosition, setSubtotalPosition] = useState<'top' | 'bottom'>('bottom')

  const [sqlModalVisible, setSqlModalVisible] = useState(false)
  const [generatedSql, setGeneratedSql] = useState('')

  const { draggedItem, handleDragStart, handleDrop, handleDragOver, setDraggedItem } = useDragDrop()

  useEffect(() => {
    fetchDatasets()
  }, [])

  useEffect(() => {
    if (selectedDatasetId) {
      fetchFields(selectedDatasetId)
    } else {
      setFields([])
    }
  }, [selectedDatasetId])

  const fetchDatasets = async () => {
    setLoading(true)
    try {
      const res = await getDatasetAll()
      setDatasets(res)
    } catch {
      setDatasets(mockDatasets)
    } finally {
      setLoading(false)
    }
  }

  const fetchFields = async (datasetId: number) => {
    setLoading(true)
    try {
      const res = await getDatasetPreview(datasetId, {}, 10)
      if (res?.columns) {
        setFields(res.columns)
      } else {
        setFields(getMockFields())
      }
    } catch {
      setFields(getMockFields())
    } finally {
      setLoading(false)
    }
  }

  const getMockFields = (): DatasetField[] => [
    { name: 'region', type: 'STRING' },
    { name: 'city', type: 'STRING' },
    { name: 'quarter', type: 'STRING' },
    { name: 'year', type: 'STRING' },
    { name: 'product', type: 'STRING' },
    { name: 'category', type: 'STRING' },
    { name: 'sales', type: 'NUMBER' },
    { name: 'orders', type: 'NUMBER' },
    { name: 'amount', type: 'NUMBER' },
    { name: 'quantity', type: 'NUMBER' },
    { name: 'profit', type: 'NUMBER' }
  ]

  const handleDatasetChange = (value: number) => {
    setSelectedDatasetId(value)
    setRowFields([])
    setColumnFields([])
    setValueFields([])
    setPivotResult(undefined)
  }

  const handleAddField = (fieldName: string, targetZone: PivotFieldType) => {
    const field = fields.find(f => f.name === fieldName)
    if (!field) return

    const isInUse = [
      ...rowFields.map(f => f.fieldName),
      ...columnFields.map(f => f.fieldName),
      ...valueFields.map(f => f.fieldName)
    ].includes(fieldName)

    if (isInUse) {
      message.warning('该字段已在使用中')
      return
    }

    const newField: PivotField = {
      fieldName,
      displayName: fieldName,
      fieldType: targetZone,
      sortOrder: 0
    }

    if (targetZone === 'VALUE') {
      newField.aggregateFunction = 'SUM'
    }

    switch (targetZone) {
      case 'ROW':
        setRowFields(prev => [...prev, { ...newField, sortOrder: prev.length }])
        break
      case 'COLUMN':
        setColumnFields(prev => [...prev, { ...newField, sortOrder: prev.length }])
        break
      case 'VALUE':
        setValueFields(prev => [...prev, { ...newField, sortOrder: prev.length }])
        break
    }
  }

  const handleRemoveField = (fieldName: string, fieldType: PivotFieldType) => {
    switch (fieldType) {
      case 'ROW':
        setRowFields(prev => prev.filter(f => f.fieldName !== fieldName))
        break
      case 'COLUMN':
        setColumnFields(prev => prev.filter(f => f.fieldName !== fieldName))
        break
      case 'VALUE':
        setValueFields(prev => prev.filter(f => f.fieldName !== fieldName))
        break
    }
  }

  const handleAggregateFunctionChange = (fieldName: string, value: AggregateFunction) => {
    setValueFields(prev =>
      prev.map(f => (f.fieldName === fieldName ? { ...f, aggregateFunction: value } : f))
    )
  }

  const handleDragDrop = (item: DragItem, targetZone: PivotFieldType) => {
    handleAddField(item.fieldName, targetZone)
  }

  const handleExecute = async () => {
    if (!selectedDatasetId) {
      message.warning('请先选择数据集')
      return
    }

    if (valueFields.length === 0) {
      message.warning('请至少添加一个指标字段')
      return
    }

    const config: PivotTableConfig = {
      dataSetId: selectedDatasetId,
      rowFields,
      columnFields,
      valueFields,
      showSubtotal,
      showGrandTotal,
      subtotalPosition
    }

    setExecuting(true)
    try {
      const res = await executePivot(config)
      if (res.success) {
        setPivotResult(res.data)
        message.success('执行成功')
      } else {
        message.error(res.message || '执行失败')
      }
    } catch (e: any) {
      message.error(e.message || '执行失败')
    } finally {
      setExecuting(false)
    }
  }

  const handleGenerateSql = async () => {
    if (!selectedDatasetId) {
      message.warning('请先选择数据集')
      return
    }

    const config: PivotTableConfig = {
      dataSetId: selectedDatasetId,
      rowFields,
      columnFields,
      valueFields,
      showSubtotal,
      showGrandTotal,
      subtotalPosition
    }

    try {
      const res = await generatePivotSql(config)
      if (res.success) {
        setGeneratedSql(res.data)
        setSqlModalVisible(true)
      } else {
        message.error(res.message || '生成SQL失败')
      }
    } catch (e: any) {
      message.error(e.message || '生成SQL失败')
    }
  }

  const handleReset = () => {
    setRowFields([])
    setColumnFields([])
    setValueFields([])
    setPivotResult(undefined)
    message.success('已重置配置')
  }

  const getFieldTypeColor = (type: string) => {
    return type === 'NUMBER' ? 'blue' : type === 'DATE' || type === 'DATETIME' ? 'green' : 'gold'
  }

  const renderFieldItem = (field: DatasetField) => {
    const isInUse = [
      ...rowFields.map(f => f.fieldName),
      ...columnFields.map(f => f.fieldName),
      ...valueFields.map(f => f.fieldName)
    ].includes(field.name)

    return (
      <div
        key={field.name}
        draggable={!isInUse}
        onDragStart={(e) => !isInUse && handleDragStart({ fieldName: field.name, displayName: field.name }, e)}
        onDragEnd={() => setDraggedItem(null)}
        style={{
          padding: '8px 12px',
          marginBottom: 6,
          background: isInUse ? '#f0f0f0' : '#fff',
          border: '1px solid #e8e8e8',
          borderRadius: 6,
          cursor: isInUse ? 'not-allowed' : 'grab',
          opacity: isInUse ? 0.5 : 1,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          transition: 'all 0.2s'
        }}
        onClick={() => !isInUse && handleQuickAdd(field.name)}
        title={isInUse ? '已在使用中' : '点击添加到行维度，或拖拽到对应区域'}
      >
        <Space>
          <DatabaseOutlined style={{ color: '#1890ff' }} />
          <span style={{ fontSize: 13 }}>{field.name}</span>
        </Space>
        <Tag color={getFieldTypeColor(field.type)} style={{ margin: 0 }}>
          {field.type}
        </Tag>
      </div>
    )
  }

  const handleQuickAdd = (fieldName: string) => {
    const field = fields.find(f => f.name === fieldName)
    if (!field) return

    const targetZone: PivotFieldType = field.type === 'NUMBER' ? 'VALUE' : 'ROW'
    handleAddField(fieldName, targetZone)
  }

  const renderPivotField = (field: PivotField, fieldType: PivotFieldType) => {
    return (
      <div
        key={field.fieldName}
        draggable
        onDragStart={(e) => handleDragStart({ fieldName: field.fieldName, displayName: field.displayName, sourceZone: fieldType }, e)}
        style={{
          padding: '8px 12px',
          marginBottom: 6,
          background: '#fff',
          border: '1px solid #d9d9d9',
          borderRadius: 6,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: 8
        }}
      >
        <Space>
          <ApiOutlined style={{ color: '#52c41a' }} />
          <span>{field.displayName || field.fieldName}</span>
        </Space>
        <Space size="small">
          {fieldType === 'VALUE' && (
            <Select
              size="small"
              value={field.aggregateFunction}
              style={{ width: 100 }}
              options={aggregateFunctionOptions}
              onChange={(v) => handleAggregateFunctionChange(field.fieldName, v)}
              onClick={(e) => e.stopPropagation()}
            />
          )}
          <Button
            type="text"
            danger
            size="small"
            icon={<DeleteOutlined />}
            onClick={(e) => {
              e.stopPropagation()
              handleRemoveField(field.fieldName, fieldType)
            }}
          />
        </Space>
      </div>
    )
  }

  const renderDropZone = (title: string, fields: PivotField[], fieldType: PivotFieldType, description: string) => {
    const isDragOver = draggedItem !== null
    return (
      <Card
        size="small"
        title={
          <Space>
            <SettingOutlined />
            <span>{title}</span>
            <Tag color="blue">{fields.length}</Tag>
          </Space>
        }
        style={{ height: '100%' }}
      >
        <div
          onDragOver={handleDragOver}
          onDrop={(e) => handleDrop(fieldType, e, handleDragDrop)}
          style={{
            minHeight: 80,
            padding: 12,
            border: `2px dashed ${isDragOver ? '#1890ff' : '#d9d9d9'}`,
            borderRadius: 6,
            background: isDragOver ? '#e6f7ff' : '#fafafa',
            transition: 'all 0.2s'
          }}
        >
          {fields.length === 0 ? (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <div style={{ color: '#999', fontSize: 12 }}>
                  {description}
                </div>
              }
            />
          ) : (
            fields.map(f => renderPivotField(f, fieldType))
          )}
        </div>
      </Card>
    )
  }

  const availableFields = fields.filter(
    f => ![...rowFields, ...columnFields, ...valueFields].some(pf => pf.fieldName === f.name)
  )

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card size="small">
        <Row gutter={16} align="middle">
          <Col>
            <Text strong style={{ fontSize: 14 }}>数据集：</Text>
          </Col>
          <Col span={6}>
            <Select
              placeholder="请选择数据集"
              loading={loading}
              style={{ width: '100%' }}
              value={selectedDatasetId}
              onChange={handleDatasetChange}
              options={datasets.map(d => ({ label: d.name, value: d.id! }))}
              allowClear
            />
          </Col>
          <Col flex="auto">
            <Space>
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                onClick={handleExecute}
                loading={executing}
                disabled={!selectedDatasetId || valueFields.length === 0}
              >
                执行
              </Button>
              <Button
                icon={<CodeOutlined />}
                onClick={handleGenerateSql}
                disabled={!selectedDatasetId}
              >
                生成SQL
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={handleReset}
              >
                重置
              </Button>
            </Space>
          </Col>
          <Col>
            <Space direction="vertical" size="small">
              <Checkbox checked={showSubtotal} onChange={e => setShowSubtotal(e.target.checked)}>
                显示小计
              </Checkbox>
              <Checkbox checked={showGrandTotal} onChange={e => setShowGrandTotal(e.target.checked)}>
                显示总计
              </Checkbox>
              <Space>
                <Text type="secondary">小计位置：</Text>
                <Radio.Group
                  size="small"
                  value={subtotalPosition}
                  onChange={e => setSubtotalPosition(e.target.value)}
                  disabled={!showSubtotal}
                >
                  <Radio.Button value="top">顶部</Radio.Button>
                  <Radio.Button value="bottom">底部</Radio.Button>
                </Radio.Group>
              </Space>
            </Space>
          </Col>
        </Row>
      </Card>

      <Row gutter={16} style={{ minHeight: 300 }}>
        <Col span={6}>
          <Card
            size="small"
            title={
              <Space>
                <DatabaseOutlined />
                <span>可用字段</span>
                <Tag color="green">{availableFields.length}</Tag>
              </Space>
            }
            extra={
              <Tooltip title="点击字段快速添加，数值类型自动添加到指标，其他类型添加到行维度">
                <Text type="secondary" style={{ fontSize: 12 }}>点击快速添加</Text>
              </Tooltip>
            }
            style={{ height: '100%' }}
          >
            <div style={{ maxHeight: 400, overflowY: 'auto', paddingRight: 4 }}>
              {!selectedDatasetId ? (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="请先选择数据集"
                />
              ) : availableFields.length === 0 ? (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="所有字段已使用"
                />
              ) : (
                availableFields.map(renderFieldItem)
              )}
            </div>
          </Card>
        </Col>

        <Col span={12}>
          <Row gutter={[16, 16]}>
            <Col span={24}>
              {renderDropZone(
                '行维度 (Rows)',
                rowFields,
                'ROW',
                '拖拽字段到此处，或点击左侧字段快速添加'
              )}
            </Col>
            <Col span={24}>
              {renderDropZone(
                '列维度 (Columns)',
                columnFields,
                'COLUMN',
                '拖拽字段到此处作为列维度'
              )}
            </Col>
            <Col span={24}>
              {renderDropZone(
                '指标 (Values)',
                valueFields,
                'VALUE',
                '拖拽数值字段到此处作为聚合指标'
              )}
            </Col>
          </Row>
        </Col>

        <Col span={6}>
          <Card
            size="small"
            title={
              <Space>
                <SettingOutlined />
                <span>配置说明</span>
              </Space>
            }
            style={{ height: '100%' }}
          >
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <div>
                <Text strong>行维度：</Text>
                <Text type="secondary">用于纵向分组的字段</Text>
              </div>
              <div>
                <Text strong>列维度：</Text>
                <Text type="secondary">用于横向分组的字段</Text>
              </div>
              <div>
                <Text strong>指标：</Text>
                <Text type="secondary">需要聚合计算的数值字段</Text>
              </div>
              <Divider style={{ margin: '12px 0' }} />
              <div>
                <Text strong>聚合函数：</Text>
              </div>
              <Tag color="blue">SUM - 求和</Tag>
              <Tag color="green">COUNT - 计数</Tag>
              <Tag color="orange">AVG - 平均值</Tag>
              <Tag color="red">MAX - 最大值</Tag>
              <Tag color="purple">MIN - 最小值</Tag>
              <Divider style={{ margin: '12px 0' }} />
              <div>
                <Text strong>操作说明：</Text>
                <ul style={{ paddingLeft: 16, margin: '8px 0' }}>
                  <li><Text type="secondary">点击左侧字段快速添加</Text></li>
                  <li><Text type="secondary">拖拽字段到对应区域</Text></li>
                  <li><Text type="secondary">点击 <DeleteOutlined /> 移除字段</Text></li>
                  <li><Text type="secondary">指标字段可切换聚合函数</Text></li>
                </ul>
              </div>
            </Space>
          </Card>
        </Col>
      </Row>

      <Card
        size="small"
        title={
          <Space>
            <PlayCircleOutlined />
            <span>交叉报表预览</span>
          </Space>
        }
        extra={
          pivotResult?.summary && (
            <Space>
              {Object.entries(pivotResult.summary).map(([key, value]) => (
                <Tag key={key} color="blue">
                  {key}: {typeof value === 'number' ? value.toLocaleString() : value}
                </Tag>
              ))}
            </Space>
          )
        }
      >
        <PivotTableRenderer data={pivotResult} height={500} />
      </Card>

      <Modal
        title="生成的 SQL"
        open={sqlModalVisible}
        onCancel={() => setSqlModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setSqlModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={800}
      >
        <pre
          style={{
            background: '#f5f5f5',
            padding: 16,
            borderRadius: 8,
            maxHeight: 400,
            overflow: 'auto',
            fontFamily: 'monospace',
            fontSize: 13
          }}
        >
          {generatedSql}
        </pre>
      </Modal>
    </Space>
  )
}

export default PivotDesigner
