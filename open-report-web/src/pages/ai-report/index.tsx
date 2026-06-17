import { useState, useEffect } from 'react'
import {
  Card,
  Input,
  Button,
  Select,
  Space,
  Typography,
  Alert,
  Tag,
  Tabs,
  List,
  Empty,
  Spin,
  message,
  Divider,
  Row,
  Col,
  Modal,
  Steps
} from 'antd'
import {
  BulbOutlined,
  ThunderboltOutlined,
  DatabaseOutlined,
  BarChartOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  CopyOutlined,
  EditOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import {
  getAiStatus,
  generateAiReport,
  createReportFromResult,
  type AiGenerateResult,
  type AiChartSuggestion,
  type GeneratedReportResult
} from '@/api/ai'
import { getDatasourceAll } from '@/api/datasource'
import { getDatasetPreview } from '@/api/dataset'
import { useNavigate } from 'react-router-dom'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input
const { Option } = Select

const examplePrompts = [
  '本季度各产品销售额TOP10',
  '近30天用户注册和活跃趋势',
  '各分类库存数量及预警分析',
  '月度销售对比分析',
  '各地区订单分布统计'
]

const chartTypeLabels: Record<string, string> = {
  bar: '柱状图',
  line: '折线图',
  pie: '饼图',
  area: '面积图',
  radar: '雷达图',
  scatter: '散点图'
}

const chartTypeIcons: Record<string, string> = {
  bar: '📊',
  line: '📈',
  pie: '🥧',
  area: '📉',
  radar: '🎯',
  scatter: '⚬'
}

const buildChartOption = (chart: AiChartSuggestion, data: Record<string, any>[] = []): Record<string, any> => {
  const title = chart.title || ''
  const xField = chart.xField || ''
  const yFields = chart.yFields || []

  if (chart.chartType === 'pie') {
    return {
      title: { text: title, left: 'center', textStyle: { fontSize: 13 } },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '60%'],
        itemStyle: { borderRadius: 4 },
        data: data.slice(0, 10).map((d: any) => ({
          name: d[xField],
          value: d[yFields[0] || '']
        }))
      }]
    }
  }

  if (chart.chartType === 'radar') {
    const indicators = data.slice(0, 8).map((d: any) => ({
      name: d[xField],
      max: Math.max(...data.map((r: any) => Number(r[yFields[0] || '']) || 0), 10) * 1.2
    }))
    return {
      title: { text: title, left: 'center', textStyle: { fontSize: 13 } },
      tooltip: {},
      radar: { indicator: indicators },
      series: [{
        type: 'radar',
        data: yFields.map(yf => ({
          value: data.slice(0, 8).map((d: any) => d[yf]),
          name: yf
        }))
      }]
    }
  }

  const isArea = chart.chartType === 'area'
  const seriesType = isArea ? 'line' : chart.chartType

  const xAxisData = data.map((d: any) => d[xField])
  const series = yFields.map((yf: string) => ({
    name: yf,
    type: seriesType,
    smooth: isArea || chart.chartType === 'line',
    areaStyle: isArea ? {} : undefined,
    itemStyle: chart.chartType === 'bar' ? { borderRadius: [4, 4, 0, 0] } : undefined,
    data: data.map((d: any) => d[yf])
  }))

  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 13 } },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { top: 40, bottom: 30, left: 50, right: 20 },
    xAxis: { type: 'category', data: xAxisData },
    yAxis: { type: 'value' },
    series
  }
}

const mockChartData = (chart: AiChartSuggestion): Record<string, any>[] => {
  const data: Record<string, any>[] = []
  const count = Math.floor(Math.random() * 5) + 6
  const categories = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月']

  for (let i = 0; i < count; i++) {
    const item: Record<string, any> = {}
    item[chart.xField || 'x'] = categories[i % 10]
    ;(chart.yFields || []).forEach((yf, idx) => {
      item[yf] = Math.floor(Math.random() * 1000) + 200 + idx * 100
    })
    data.push(item)
  }
  return data
}

const AiReportGenerator = () => {
  const navigate = useNavigate()
  const [prompt, setPrompt] = useState('')
  const [dsId, setDsId] = useState<number | undefined>()
  const [datasourceList, setDatasourceList] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [aiResult, setAiResult] = useState<AiGenerateResult | null>(null)
  const [aiEnabled, setAiEnabled] = useState(false)
  const [currentStep, setCurrentStep] = useState(0)
  const [createLoading, setCreateLoading] = useState(false)
  const [showSql, setShowSql] = useState(false)
  const [previewData, setPreviewData] = useState<Record<string, any>[]>([])

  useEffect(() => {
    loadDatasourceList()
    checkAiStatus()
  }, [])

  const loadDatasourceList = async () => {
    try {
      const list = await getDatasourceAll()
      setDatasourceList(list as any[])
      if (list.length > 0 && !dsId) {
        setDsId(list[0].id)
      }
    } catch (error) {
      console.error('加载数据源失败:', error)
    }
  }

  const checkAiStatus = async () => {
    try {
      const status = await getAiStatus()
      setAiEnabled(status.enabled)
    } catch (error) {
      console.error('检查AI状态失败:', error)
    }
  }

  const handleExampleClick = (example: string) => {
    setPrompt(example)
  }

  const handleGenerate = async () => {
    if (!prompt.trim()) {
      message.warning('请输入报表描述')
      return
    }
    if (!dsId) {
      message.warning('请选择数据源')
      return
    }

    setGenerating(true)
    setAiResult(null)
    setCurrentStep(1)

    try {
      const result = await generateAiReport({ prompt, dsId })
      setAiResult(result)
      setCurrentStep(2)
      setPreviewData(mockChartData(result.charts?.[0] || { xField: 'x', yFields: ['y'], chartType: 'bar', title: '' } as any))
    } catch (error: any) {
      message.error(error.message || '生成失败')
    } finally {
      setGenerating(false)
    }
  }

  const handleCreateReport = async () => {
    if (!aiResult || !dsId) return

    setCreateLoading(true)
    try {
      const result: GeneratedReportResult = await createReportFromResult(aiResult, dsId)
      message.success('报表生成成功！')
      setCurrentStep(3)

      Modal.confirm({
        title: '报表生成成功',
        icon: <CheckCircleOutlined style={{ color: '#52c41a' }} />,
        content: (
          <div>
            <p>报表名称：{result.reportName}</p>
            <p>数据集：{result.dataSetName}</p>
            <p>是否立即前往设计器编辑？</p>
          </div>
        ),
        okText: '去设计器',
        cancelText: '留在本页',
        onOk: () => {
          navigate(`/report/designer/${result.reportId}`)
        }
      })
    } catch (error: any) {
      message.error(error.message || '生成失败')
    } finally {
      setCreateLoading(false)
    }
  }

  const handleCopySql = () => {
    if (aiResult?.sql) {
      navigator.clipboard.writeText(aiResult.sql)
      message.success('SQL已复制')
    }
  }

  const stepItems = [
    { title: '输入需求', icon: <BulbOutlined /> },
    { title: 'AI生成', icon: <ThunderboltOutlined /> },
    { title: '完成', icon: <CheckCircleOutlined /> }
  ]

  return (
    <div style={{ padding: 0 }}>
      <Card
        style={{
          marginBottom: 16,
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: '#fff',
          borderRadius: 12
        }}
        bodyStyle={{ padding: 32 }}
      >
        <Title level={2} style={{ color: '#fff', margin: '0 0 8px 0' }}>
          <ThunderboltOutlined style={{ marginRight: 12 }} />
          AI 智能报表生成
        </Title>
        <Text style={{ color: 'rgba(255,255,255,0.85)', fontSize: 14 }}>
          用自然语言描述你想要的报表，AI 自动生成 SQL 和图表，一键生成完整报表模板
        </Text>
        {!aiEnabled && (
          <Tag color="warning" style={{ marginTop: 12 }}>
            当前为演示模式，使用模拟数据展示功能
          </Tag>
        )}
      </Card>

      <Steps
        current={currentStep}
        items={stepItems}
        style={{ marginBottom: 24, background: '#fff', padding: '20px 40px', borderRadius: 8 }}
      />

      <Row gutter={16}>
        <Col span={24}>
          <Card title="描述你的报表需求" style={{ marginBottom: 16 }}>
            <Space direction="vertical" style={{ width: '100%' }} size={16}>
              <Space.Compact style={{ width: '100%' }}>
                <Select
                  style={{ width: 200 }}
                  value={dsId}
                  onChange={setDsId}
                  placeholder="选择数据源"
                  prefix={<DatabaseOutlined />}
                >
                  {datasourceList.map((ds) => (
                    <Option key={ds.id} value={ds.id}>
                      {ds.dsName} ({ds.dsType})
                    </Option>
                  ))}
                </Select>
                <Input
                  placeholder="例如：本季度各产品销售TOP10"
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  onPressEnter={handleGenerate}
                  style={{ flex: 1 }}
                  size="large"
                  prefix={<BulbOutlined />}
                />
                <Button
                  type="primary"
                  size="large"
                  icon={<ThunderboltOutlined />}
                  onClick={handleGenerate}
                  loading={generating}
                >
                  智能生成
                </Button>
              </Space.Compact>

              <div>
                <Text type="secondary" style={{ marginRight: 8 }}>示例：</Text>
                <Space wrap>
                  {examplePrompts.map((example, idx) => (
                    <Tag
                      key={idx}
                      style={{ cursor: 'pointer', padding: '4px 10px', fontSize: 12 }}
                      onClick={() => handleExampleClick(example)}
                    >
                      {example}
                    </Tag>
                  ))}
                </Space>
              </div>
            </Space>
          </Card>
        </Col>
      </Row>

      {aiResult && (
        <>
          <Card
            title={
              <Space>
                <FileTextOutlined />
                <span>生成结果</span>
                <Tag color="green">{aiResult.reportTitle}</Tag>
              </Space>
            }
            style={{ marginBottom: 16 }}
            extra={
              <Space>
                <Button icon={<CopyOutlined />} onClick={handleCopySql}>
                  复制SQL
                </Button>
                <Button
                  type="primary"
                  icon={<EditOutlined />}
                  onClick={handleCreateReport}
                  loading={createLoading}
                >
                  生成报表模板
                </Button>
              </Space>
            }
          >
            <Tabs
              defaultActiveKey="charts"
              items={[
                {
                  key: 'charts',
                  label: (
                    <span>
                      <BarChartOutlined /> 图表建议
                    </span>
                  ),
                  children: (
                    <Spin spinning={loading}>
                      {aiResult.charts && aiResult.charts.length > 0 ? (
                        <Row gutter={[16, 16]}>
                          {aiResult.charts.map((chart, idx) => (
                            <Col xs={24} md={12} lg={8} key={idx}>
                              <Card
                                size="small"
                                title={
                                  <Space>
                                    <span>{chartTypeIcons[chart.chartType] || '📊'}</span>
                                    <span>{chart.title}</span>
                                    <Tag color="blue" style={{ marginLeft: 8 }}>
                                      {chartTypeLabels[chart.chartType] || chart.chartType}
                                    </Tag>
                                  </Space>
                                }
                              >
                                <div style={{ height: 220 }}>
                                  <ReactECharts
                                    option={buildChartOption(chart, previewData)}
                                    style={{ height: '100%', width: '100%' }}
                                    opts={{ renderer: 'canvas' }}
                                  />
                                </div>
                                {chart.description && (
                                  <div style={{ marginTop: 8, fontSize: 12, color: '#999' }}>
                                    {chart.description}
                                  </div>
                                )}
                                <div style={{ marginTop: 8, fontSize: 12 }}>
                                  <Text type="secondary">分类轴：</Text>
                                  <Tag style={{ fontSize: 11 }}>{chart.xField}</Tag>
                                  <Text type="secondary" style={{ marginLeft: 8 }}>数值轴：</Text>
                                  {(chart.yFields || []).map((yf, i) => (
                                    <Tag key={i} color="green" style={{ fontSize: 11 }}>
                                      {yf}
                                    </Tag>
                                  ))}
                                </div>
                              </Card>
                            </Col>
                          ))}
                        </Row>
                      ) : (
                        <Empty description="暂无图表建议" />
                      )}
                    </Spin>
                  )
                },
                {
                  key: 'sql',
                  label: (
                    <span>
                      <DatabaseOutlined /> SQL 语句
                    </span>
                  ),
                  children: (
                    <div>
                      <pre
                        style={{
                          background: '#f6f8fa',
                          padding: 16,
                          borderRadius: 6,
                          fontSize: 13,
                          lineHeight: 1.6,
                          overflowX: 'auto'
                        }}
                      >
                        {aiResult.sql}
                      </pre>
                      {aiResult.description && (
                        <Alert
                          type="info"
                          showIcon
                          message="说明"
                          description={aiResult.description}
                          style={{ marginTop: 12 }}
                        />
                      )}
                    </div>
                  )
                },
                {
                  key: 'fields',
                  label: (
                    <span>
                      <FileTextOutlined /> 字段信息
                    </span>
                  ),
                  children: (
                    <List
                      dataSource={aiResult.fields}
                      renderItem={(field) => (
                        <List.Item>
                          <List.Item.Meta
                            title={field.label || field.name}
                            description={
                              <Space>
                                <Tag>{field.name}</Tag>
                                <Tag color="blue">{field.type}</Tag>
                              </Space>
                            }
                          />
                        </List.Item>
                      )}
                    />
                  )
                }
              ]}
            />
          </Card>

          <Card
            style={{
              textAlign: 'center',
              padding: '24px 0',
              background: '#f9f9ff'
            }}
            bodyStyle={{ padding: 24 }}
          >
            <Title level={4} style={{ marginBottom: 16 }}>
              满意吗？一键生成完整报表模板
            </Title>
            <Paragraph type="secondary" style={{ marginBottom: 20 }}>
              AI 将自动创建数据集和报表模板，包含表格和图表，你可以在设计器中进一步调整
            </Paragraph>
            <Space>
              <Button size="large" onClick={() => setCurrentStep(0)}>
                重新生成
              </Button>
              <Button
                type="primary"
                size="large"
                icon={<EditOutlined />}
                onClick={handleCreateReport}
                loading={createLoading}
              >
                生成报表模板
              </Button>
            </Space>
          </Card>
        </>
      )}
    </div>
  )
}

export default AiReportGenerator
