import { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Card,
  Row,
  Col,
  Statistic,
  Tag,
  Tabs,
  Modal,
  Form,
  InputNumber,
  Switch,
  message,
  Progress,
  Descriptions,
  Empty,
  Tooltip,
  Popconfirm,
  List,
  Alert
} from 'antd'
import {
  ReloadOutlined,
  PlayCircleOutlined,
  DeleteOutlined,
  FireOutlined,
  ThunderboltOutlined,
  DashboardOutlined,
  ClockCircleOutlined,
  SettingOutlined,
  DatabaseOutlined,
  ClearOutlined,
  SafetyOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import type { ColumnsType } from 'antd/es/table'
import {
  getOverallCacheInfo,
  getTemplateCacheInfo,
  warmupSingleReport,
  warmupHotReports,
  warmupHotParamCombos,
  evictTemplateCache,
  evictAllCache,
  getHotReports,
  getHotParamCombos,
  getOverallStats,
  getWarmupConfig,
  updateWarmupConfig,
  cleanupExpiredCache,
  type HotReportItem,
  type CacheWarmupConfig,
  type WarmupResult
} from '@/api/cache'

const CacheManager = () => {
  const [loading, setLoading] = useState(false)
  const [overallInfo, setOverallInfo] = useState<any>(null)
  const [config, setConfig] = useState<CacheWarmupConfig | null>(null)
  const [hotReports, setHotReports] = useState<HotReportItem[]>([])
  const [hotCombos, setHotCombos] = useState<any[]>([])
  const [cacheStats, setCacheStats] = useState<any>(null)
  const [templateCacheInfo, setTemplateCacheInfo] = useState<any>(null)
  const [configModalVisible, setConfigModalVisible] = useState(false)
  const [warmupModalVisible, setWarmupModalVisible] = useState(false)
  const [warmupType, setWarmupType] = useState<'template' | 'hot' | 'combos'>('hot')
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null)
  const [warmupResult, setWarmupResult] = useState<WarmupResult[]>([])
  const [warmupLoading, setWarmupLoading] = useState(false)
  const [form] = Form.useForm()

  const loadData = async () => {
    setLoading(true)
    try {
      const [info, hotRep, hotComb, stats, cfg] = await Promise.all([
        getOverallCacheInfo(),
        getHotReports(7, 10),
        getHotParamCombos(7, 10, 20),
        getOverallStats(
          dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
          dayjs().format('YYYY-MM-DD')
        ),
        getWarmupConfig()
      ])
      setOverallInfo(info?.cache)
      setConfig(cfg)
      setHotReports(hotRep || [])
      setHotCombos(hotComb || [])
      setCacheStats(stats)
      form.setFieldsValue(cfg || {})
    } catch (e) {
      message.error('加载缓存信息失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleWarmupHot = async (type: 'hot' | 'combos') => {
    setWarmupLoading(true)
    setWarmupResult([])
    try {
      const result = type === 'hot'
        ? await warmupHotReports(20, 10, 7)
        : await warmupHotParamCombos(50, 10, 7)
      setWarmupResult(result || [])
      const success = (result || []).filter(r => r.success).length
      const fail = (result || []).filter(r => !r.success).length
      message.success(`预热完成: 成功 ${success} 个, 失败 ${fail} 个`)
      loadData()
    } catch (e) {
      message.error('预热失败')
    } finally {
      setWarmupLoading(false)
    }
  }

  const handleWarmupSingle = async () => {
    if (!selectedTemplateId) return
    setWarmupLoading(true)
    try {
      const result = await warmupSingleReport(selectedTemplateId)
      setWarmupResult([result])
      if (result.success) {
        message.success('预热成功')
        loadData()
      } else {
        message.error(result.message || '预热失败')
      }
    } catch (e) {
      message.error('预热失败')
    } finally {
      setWarmupLoading(false)
    }
  }

  const handleCleanup = async () => {
    try {
      const result = await cleanupExpiredCache()
      message.success(result?.message || '清理完成')
      loadData()
    } catch (e) {
      message.error('清理失败')
    }
  }

  const handleEvictAll = async () => {
    try {
      await evictAllCache()
      message.success('已清除全部缓存')
      loadData()
    } catch (e) {
      message.error('清除失败')
    }
  }

  const handleSaveConfig = async (values: any) => {
    try {
      const result = await updateWarmupConfig(values)
      setConfig(result)
      setConfigModalVisible(false)
      message.success('配置已保存')
      loadData()
    } catch (e) {
      message.error('保存失败')
    }
  }

  const loadTemplateCacheInfo = async (templateId: number) => {
    try {
      const info = await getTemplateCacheInfo(templateId)
      setTemplateCacheInfo(info)
    } catch (e) {
      message.error('加载缓存详情失败')
    }
  }

  const hotReportColumns: ColumnsType<HotReportItem> = [
    {
      title: '排名',
      dataIndex: 'index',
      key: 'index',
      width: 60,
      render: (_v, _r, idx) => {
        const colors = ['#f5222d', '#fa541c', '#fa8c16']
        if (idx < 3) {
          return <Tag color={colors[idx]} style={{ fontWeight: 600 }}>Top {idx + 1}</Tag>
        }
        return <span style={{ color: '#8c8c8c' }}>#{idx + 1}</span>
      }
    },
    {
      title: '模板ID',
      dataIndex: 'template_id',
      key: 'template_id',
      width: 100
    },
    {
      title: '报表名称',
      dataIndex: 'template_name',
      key: 'template_name',
      ellipsis: true
    },
    {
      title: '访问次数',
      dataIndex: 'access_count',
      key: 'access_count',
      width: 120,
      render: (v) => <Tag color="geekblue"><FireOutlined /> {v}</Tag>
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_v, record) => (
        <Space>
          <Tooltip title="预热该报表">
            <Button
              size="small"
              type="link"
              icon={<PlayCircleOutlined />}
              onClick={() => {
                setSelectedTemplateId(record.template_id as number)
                setWarmupType('template')
                setWarmupModalVisible(true)
              }}
            >
              预热
            </Button>
          </Tooltip>
          <Tooltip title="查看缓存详情">
            <Button
              size="small"
              type="link"
              icon={<DashboardOutlined />}
              onClick={() => loadTemplateCacheInfo(record.template_id as number)}
            >
              详情
            </Button>
          </Tooltip>
          <Popconfirm
            title="确认清除该报表所有缓存？"
            onConfirm={async () => {
              await evictTemplateCache(record.template_id as number)
              message.success('缓存已清除')
              loadData()
            }}
          >
            <Button size="small" type="link" danger icon={<DeleteOutlined />}>
              清除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const hotComboColumns: ColumnsType<any> = [
    {
      title: '排名',
      dataIndex: 'index',
      key: 'index',
      width: 60,
      render: (_v, _r, idx) => {
        const colors = ['#f5222d', '#fa541c', '#fa8c16']
        if (idx < 3) {
          return <Tag color={colors[idx]} style={{ fontWeight: 600 }}>Top {idx + 1}</Tag>
        }
        return <span style={{ color: '#8c8c8c' }}>#{idx + 1}</span>
      }
    },
    {
      title: '模板ID',
      dataIndex: 'template_id',
      key: 'template_id',
      width: 100
    },
    {
      title: '参数哈希',
      dataIndex: 'params_hash',
      key: 'params_hash',
      width: 120,
      render: (v) => <code style={{ fontSize: 12 }}>{v}</code>
    },
    {
      title: '访问次数',
      dataIndex: 'access_count',
      key: 'access_count',
      width: 120,
      render: (v) => <Tag color="geekblue"><ThunderboltOutlined /> {v}</Tag>
    }
  ]

  const hitRate = cacheStats?.hitRate ? parseFloat(cacheStats.hitRate) : 0
  const hitRateColor = hitRate >= 80 ? '#52c41a' : hitRate >= 50 ? '#faad14' : '#f5222d'

  return (
    <div style={{ padding: 16 }}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ margin: 0 }}>
          <DatabaseOutlined style={{ marginRight: 8 }} />
          智能缓存管理
        </h2>
        <Space>
          <Tooltip title="刷新数据">
            <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
              刷新
            </Button>
          </Tooltip>
          <Tooltip title="按模板+参数哈希预热">
            <Button
              type="primary"
              icon={<ThunderboltOutlined />}
              onClick={() => handleWarmupHot('combos')}
            >
              批量预热热门组合
            </Button>
          </Tooltip>
          <Tooltip title="清理过期键">
            <Button
              icon={<ClearOutlined />}
              onClick={handleCleanup}
            >
              清理过期
            </Button>
          </Tooltip>
          <Tooltip title="修改配置">
            <Button
              icon={<SettingOutlined />}
              onClick={() => setConfigModalVisible(true)}
            >
              配置
            </Button>
          </Tooltip>
          <Popconfirm
            title="确认清除全部缓存？"
            description="此操作会删除所有报表缓存数据，可能导致短期内数据库压力上升。"
            onConfirm={handleEvictAll}
          >
            <Button danger icon={<DeleteOutlined />}>
              清除全部
            </Button>
          </Popconfirm>
        </Space>
      </div>

      {config && config.enabled === 0 && (
        <Alert
          type="warning"
          showIcon
          message="缓存预热功能已禁用"
          description="请在配置中启用缓存预热功能以享受自动预热带来的性能提升。"
          style={{ marginBottom: 16 }}
        />
      )}

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} sm={6}>
          <Card loading={loading}>
            <Statistic
              title="缓存键总数"
              value={overallInfo?.cacheCount || 0}
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card loading={loading}>
            <Statistic
              title="缓存大小"
              value={overallInfo?.totalSizeMB || '0.00'}
              suffix="MB"
              prefix={<SafetyOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card loading={loading}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 14, color: 'rgba(0,0,0,0.45)', marginBottom: 8 }}>
                7日缓存命中率
              </div>
              <Progress
                type="dashboard"
                percent={hitRate}
                strokeColor={hitRateColor}
                width={100}
              />
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card loading={loading}>
            <Statistic
              title="7日平均响应"
              value={cacheStats?.avgResponseTimeMs || 0}
              suffix="ms"
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      {config && (
        <Card
          size="small"
          title={<><SettingOutlined style={{ marginRight: 8 }} />当前预热配置</>}
          style={{ marginBottom: 16 }}
          extra={
            <Button size="small" onClick={() => setConfigModalVisible(true)}>
              修改
            </Button>
          }
        >
          <Descriptions size="small" column={4}>
            <Descriptions.Item label="功能状态">
              {config.enabled === 1
                ? <Tag color="green">已启用</Tag>
                : <Tag color="red">已禁用</Tag>}
            </Descriptions.Item>
            <Descriptions.Item label="热门阈值">
              {config.hotThreshold} 次/窗口
            </Descriptions.Item>
            <Descriptions.Item label="统计窗口">
              {config.statsWindowDays} 天
            </Descriptions.Item>
            <Descriptions.Item label="最大预热数">
              {config.maxHotReports} 个
            </Descriptions.Item>
            <Descriptions.Item label="低峰期">
              {config.lowPeakStartHour}:00 - {config.lowPeakEndHour}:00
            </Descriptions.Item>
            <Descriptions.Item label="缓存TTL">
              {(config.cacheTtlSeconds / 3600).toFixed(1)} 小时
            </Descriptions.Item>
          </Descriptions>
        </Card>
      )}

      <Tabs
        defaultActiveKey="hot-reports"
        items={[
          {
            key: 'hot-reports',
            label: (
              <span><FireOutlined /> 热门报表排行</span>
            ),
            children: (
              <Card size="small" loading={loading}>
                <Table
                  size="small"
                  rowKey={(r: any) => r.template_id}
                  columns={hotReportColumns}
                  dataSource={hotReports}
                  pagination={false}
                  locale={{ emptyText: <Empty description="暂无访问数据" /> }}
                />
              </Card>
            )
          },
          {
            key: 'hot-combos',
            label: (
              <span><ThunderboltOutlined /> 热门参数组合</span>
            ),
            children: (
              <Card size="small" loading={loading}>
                <Table
                  size="small"
                  rowKey={(r: any) => `${r.template_id}-${r.params_hash}`}
                  columns={hotComboColumns}
                  dataSource={hotCombos}
                  pagination={false}
                  locale={{ emptyText: <Empty description="暂无参数组合访问数据" /> }}
                />
              </Card>
            )
          },
          {
            key: 'template-detail',
            label: (
              <span><DashboardOutlined /> 模板缓存详情</span>
            ),
            children: (
              <Card size="small" loading={loading}>
                {templateCacheInfo ? (
                  <div>
                    <Descriptions size="small" column={3} style={{ marginBottom: 16 }}>
                      <Descriptions.Item label="模板ID">
                        {templateCacheInfo.templateId}
                      </Descriptions.Item>
                      <Descriptions.Item label="缓存数量">
                        {templateCacheInfo.cacheCount} 个
                      </Descriptions.Item>
                      <Descriptions.Item label="总大小">
                        {templateCacheInfo.totalSizeMB} MB
                      </Descriptions.Item>
                    </Descriptions>
                    <List
                      size="small"
                      dataSource={templateCacheInfo.items || []}
                      locale={{ emptyText: <Empty description="该模板暂无缓存" /> }}
                      renderItem={(item: any) => (
                        <List.Item
                          actions={[
                            <Tag key="ttl">TTL: {item.ttlSeconds}s</Tag>,
                            item.sizeBytes && (
                              <Tag key="size" color="blue">{(item.sizeBytes / 1024).toFixed(1)} KB</Tag>
                            )
                          ]}
                        >
                          <code style={{ fontSize: 12 }}>{item.key}</code>
                        </List.Item>
                      )}
                    />
                  </div>
                ) : (
                  <Empty description="请从热门报表中点击「详情」查看" />
                )}
              </Card>
            )
          },
          {
            key: 'stats',
            label: (
              <span><ReloadOutlined /> 历史趋势</span>
            ),
            children: (
              <Card size="small" loading={loading}>
                {cacheStats?.daily && cacheStats.daily.length > 0 ? (
                  <Table
                    size="small"
                    dataSource={cacheStats.daily}
                    pagination={false}
                    columns={[
                      { title: '日期', dataIndex: 'stat_date', key: 'stat_date', width: 120 },
                      { title: '总请求', dataIndex: 'total_requests', key: 'total' },
                      { title: '命中', dataIndex: 'cache_hits', key: 'hits' },
                      { title: '未命中', dataIndex: 'cache_misses', key: 'misses' },
                      {
                        title: '命中率',
                        key: 'rate',
                        render: (_v, r: any) => {
                          const rate = r.total_requests > 0
                            ? (r.cache_hits / r.total_requests * 100).toFixed(2)
                            : '0.00'
                          return `${rate}%`
                        }
                      },
                      { title: '平均响应(ms)', dataIndex: 'avg_response_time_ms', key: 'avg' }
                    ]}
                  />
                ) : (
                  <Empty description="暂无统计数据" />
                )}
              </Card>
            )
          }
        ]}
      />

      <Modal
        title="缓存预热配置"
        open={configModalVisible}
        onCancel={() => setConfigModalVisible(false)}
        footer={null}
        width={560}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSaveConfig}
          initialValues={config || {}}
        >
          <Form.Item name="enabled" label="启用缓存预热" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="hotThreshold" label="热门报表阈值（次/窗口）" rules={[{ required: true, message: '请输入' }]}>
                <InputNumber min={1} max={10000} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="statsWindowDays" label="访问统计窗口（天）" rules={[{ required: true, message: '请输入' }]}>
                <InputNumber min={1} max={30} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="maxHotReports" label="最大预热报表数" rules={[{ required: true, message: '请输入' }]}>
                <InputNumber min={1} max={500} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="cacheTtlSeconds" label="缓存生存时间（秒）" rules={[{ required: true, message: '请输入' }]}>
                <InputNumber min={60} max={2592000} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="lowPeakStartHour" label="低峰期开始（小时）" rules={[{ required: true, message: '请输入' }]}>
                <InputNumber min={0} max={23} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="lowPeakEndHour" label="低峰期结束（小时）" rules={[{ required: true, message: '请输入' }]}>
                <InputNumber min={0} max={23} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={() => setConfigModalVisible(false)}>取消</Button>
              <Button type="primary" htmlType="submit">保存</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={
          warmupType === 'template'
            ? '预热单个报表'
            : warmupType === 'combos'
            ? '批量预热热门参数组合'
            : '批量预热热门报表'
        }
        open={warmupModalVisible}
        onCancel={() => setWarmupModalVisible(false)}
        width={600}
        footer={[
          <Button key="close" onClick={() => setWarmupModalVisible(false)}>
            关闭
          </Button>
        ]}
      >
        {warmupType === 'template' && (
          <div style={{ marginBottom: 16 }}>
            <p>模板ID: <strong>{selectedTemplateId}</strong></p>
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              onClick={handleWarmupSingle}
              loading={warmupLoading}
            >
              开始预热
            </Button>
          </div>
        )}

        {warmupLoading && <Alert type="info" showIcon message="正在预热中，请耐心等待..." />}

        {warmupResult.length > 0 && (
          <div style={{ marginTop: 16, maxHeight: 400, overflow: 'auto' }}>
            <List
              size="small"
              dataSource={warmupResult}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    item.success
                      ? <Tag key="ok" color="green">成功 ({item.elapsedMs}ms)</Tag>
                      : <Tag key="fail" color="red">{item.message}</Tag>
                  ]}
                >
                  <Space>
                    {item.success
                      ? <PlayCircleOutlined style={{ color: '#52c41a' }} />
                      : <DeleteOutlined style={{ color: '#f5222d' }} />}
                    <span>
                      templateId: <strong>{item.templateId}</strong>
                      {item.paramsHash && item.paramsHash !== 'default' && (
                        <code style={{ marginLeft: 8, fontSize: 12 }}>#{item.paramsHash}</code>
                      )}
                      {item.accessCount && (
                        <Tag style={{ marginLeft: 8 }}>{item.accessCount} 次</Tag>
                      )}
                    </span>
                  </Space>
                </List.Item>
              )}
            />
          </div>
        )}
      </Modal>
    </div>
  )
}

export default CacheManager
