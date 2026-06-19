import React, { useEffect, useState } from 'react'
import {
  Modal,
  Form,
  Select,
  Input,
  Button,
  Spin,
  Alert,
  Card,
  Tag,
  Space,
  Table,
  Typography,
  Divider,
  Statistic,
  Row,
  Col,
  List
} from 'antd'
import {
  WarningOutlined,
  DatabaseOutlined,
  TableOutlined,
  FileTextOutlined,
  ApiOutlined,
  SearchOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { analyzeImpact } from '@/api/report'
import { getDatasourceAll } from '@/api/datasource'
import type { ImpactAnalysisResult, DataSourceConfig } from '@/types'

const { Text, Title } = Typography
const { Option } = Select

interface ImpactAnalysisModalProps {
  visible: boolean
  defaultDatasourceId?: number
  defaultTableName?: string
  defaultColumnName?: string
  onClose: () => void
  onReportClick?: (reportId: number) => void
}

const ImpactAnalysisModal: React.FC<ImpactAnalysisModalProps> = ({
  visible,
  defaultDatasourceId,
  defaultTableName,
  defaultColumnName,
  onClose,
  onReportClick
}) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [analyzing, setAnalyzing] = useState(false)
  const [datasources, setDatasources] = useState<DataSourceConfig[]>([])
  const [result, setResult] = useState<ImpactAnalysisResult | null>(null)
  const [hasSearched, setHasSearched] = useState(false)

  useEffect(() => {
    if (visible) {
      loadDatasources()
      if (defaultDatasourceId) {
        form.setFieldsValue({
          datasourceId: defaultDatasourceId,
          tableName: defaultTableName,
          columnName: defaultColumnName
        })
      }
    } else {
      setResult(null)
      setHasSearched(false)
    }
  }, [visible, defaultDatasourceId, defaultTableName, defaultColumnName])

  const loadDatasources = async () => {
    setLoading(true)
    try {
      const list = await getDatasourceAll()
      setDatasources(list || [])
    } catch (err: any) {
      console.error('加载数据源列表失败:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleAnalyze = async (values: any) => {
    setAnalyzing(true)
    setHasSearched(true)
    try {
      const analysisResult = await analyzeImpact(
        values.datasourceId,
        values.tableName,
        values.columnName
      )
      setResult(analysisResult)
    } catch (err: any) {
      console.error('影响分析失败:', err)
      setResult({
        success: false,
        message: err?.message || '分析失败'
      })
    } finally {
      setAnalyzing(false)
    }
  }

  const reportColumns = [
    {
      title: '报表名称',
      dataIndex: 'reportName',
      key: 'reportName',
      render: (text: string, record: any) => (
        <Space>
          <FileTextOutlined style={{ color: '#1890ff' }} />
          <a onClick={() => onReportClick?.(record.reportId)}>{text}</a>
        </Space>
      )
    },
    {
      title: '报表ID',
      dataIndex: 'reportId',
      key: 'reportId',
      width: 100
    },
    {
      title: '受影响字段',
      key: 'fields',
      render: (_: any, record: any) => {
        const fields = result?.lineageByReport?.[record.reportName] || []
        return (
          <Space wrap size={4}>
            {fields.slice(0, 5).map((f: any, idx: number) => (
              <Tag key={idx} color="blue" size="small">
                {f.reportFieldTitle || f.reportField}
              </Tag>
            ))}
            {fields.length > 5 && (
              <Tag size="small">+{fields.length - 5}</Tag>
            )}
          </Space>
        )
      }
    }
  ]

  const dataSetColumns = [
    {
      title: '数据集名称',
      dataIndex: 'dataSetName',
      key: 'dataSetName',
      render: (text: string) => (
        <Space>
          <ApiOutlined style={{ color: '#52c41a' }} />
          {text}
        </Space>
      )
    },
    {
      title: '数据集ID',
      dataIndex: 'dataSetId',
      key: 'dataSetId',
      width: 100
    }
  ]

  const renderResult = () => {
    if (!hasSearched) {
      return (
        <Alert
          type="info"
          message="请选择数据源、输入表名和字段名进行影响分析"
          showIcon
        />
      )
    }

    if (!result) return null

    if (!result.success) {
      return (
        <Alert
          type="error"
          message="影响分析失败"
          description={result.message}
          showIcon
        />
      )
    }

    const hasImpact = (result.affectedReportCount || 0) > 0

    return (
      <div>
        {hasImpact ? (
          <Alert
            type="warning"
            showIcon
            icon={<WarningOutlined />}
            message={
              <Space>
                <span>检测到影响范围！</span>
                <Tag color="red">{result.affectedReportCount} 个报表</Tag>
                <Tag color="orange">{result.affectedDataSetCount} 个数据集</Tag>
                <Tag color="blue">{result.affectedFieldCount} 个字段</Tag>
              </Space>
            }
            description="修改此字段将影响以下报表和数据集，请谨慎操作！"
            style={{ marginBottom: 16 }}
          />
        ) : (
          <Alert
            type="success"
            message="无影响范围"
            description="修改此字段不会影响任何报表或数据集"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={8}>
            <Card size="small">
              <Statistic
                title="受影响报表"
                value={result.affectedReportCount || 0}
                prefix={<FileTextOutlined style={{ color: '#1890ff' }} />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small">
              <Statistic
                title="受影响数据集"
                value={result.affectedDataSetCount || 0}
                prefix={<ApiOutlined style={{ color: '#52c41a' }} />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card size="small">
              <Statistic
                title="受影响字段"
                value={result.affectedFieldCount || 0}
                prefix={<DatabaseOutlined style={{ color: '#722ed1' }} />}
                valueStyle={{ color: '#722ed1' }}
              />
            </Card>
          </Col>
        </Row>

        {result.affectedReports && result.affectedReports.length > 0 && (
          <>
            <Divider orientation="left">
              <Space>
                <FileTextOutlined style={{ color: '#1890ff' }} />
                <span>受影响的报表列表</span>
              </Space>
            </Divider>
            <Table
              size="small"
              dataSource={result.affectedReports}
              columns={reportColumns}
              rowKey="reportId"
              pagination={{ pageSize: 5 }}
            />
          </>
        )}

        {result.affectedDataSets && result.affectedDataSets.length > 0 && (
          <>
            <Divider orientation="left">
              <Space>
                <ApiOutlined style={{ color: '#52c41a' }} />
                <span>受影响的数据集列表</span>
              </Space>
            </Divider>
            <Table
              size="small"
              dataSource={result.affectedDataSets}
              columns={dataSetColumns}
              rowKey="dataSetId"
              pagination={{ pageSize: 5 }}
            />
          </>
        )}

        {result.affectedFields && result.affectedFields.length > 0 && (
          <>
            <Divider orientation="left">
              <Space>
                <DatabaseOutlined style={{ color: '#722ed1' }} />
                <span>受影响的字段列表</span>
              </Space>
            </Divider>
            <List
              size="small"
              dataSource={result.affectedFields}
              renderItem={(item: string) => (
                <List.Item>
                  <Tag color="blue">{item}</Tag>
                </List.Item>
              )}
              style={{ maxHeight: 200, overflow: 'auto' }}
            />
          </>
        )}
      </div>
    )
  }

  return (
    <Modal
      title={
        <Space>
          <WarningOutlined style={{ color: '#fa8c16' }} />
          <span>影响分析</span>
          <Text type="secondary">- 分析表/字段变更影响范围</Text>
        </Space>
      }
      open={visible}
      onCancel={onClose}
      width={900}
      footer={null}
      destroyOnClose
    >
      <Card size="small" style={{ marginBottom: 16 }}>
        <Form
          form={form}
          layout="inline"
          onFinish={handleAnalyze}
          initialValues={{
            datasourceId: defaultDatasourceId,
            tableName: defaultTableName,
            columnName: defaultColumnName
          }}
        >
          <Form.Item
            name="datasourceId"
            label="数据源"
            rules={[{ required: true, message: '请选择数据源' }]}
          >
            <Select
              placeholder="请选择数据源"
              style={{ width: 200 }}
              loading={loading}
            >
              {datasources.map(ds => (
                <Option key={ds.id} value={ds.id}>
                  {ds.dsName} ({ds.dsType})
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="tableName"
            label="表名"
            rules={[{ required: true, message: '请输入表名' }]}
          >
            <Input
              placeholder="例如: t_user"
              prefix={<TableOutlined />}
              style={{ width: 180 }}
            />
          </Form.Item>
          <Form.Item name="columnName" label="字段名">
            <Input
              placeholder="例如: user_name (可选)"
              prefix={<DatabaseOutlined />}
              style={{ width: 180 }}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button
                type="primary"
                htmlType="submit"
                icon={<SearchOutlined spin={analyzing} />}
                loading={analyzing}
              >
                分析影响
              </Button>
              <Button
                icon={<ReloadOutlined />}
                onClick={loadDatasources}
              >
                刷新
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Spin spinning={analyzing}>
        {renderResult()}
      </Spin>
    </Modal>
  )
}

export default ImpactAnalysisModal
