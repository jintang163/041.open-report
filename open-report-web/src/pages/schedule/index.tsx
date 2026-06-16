import { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Form,
  Input,
  Select,
  Modal,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  Tag,
  Pagination,
  Tabs,
  Radio,
  Tooltip,
  Switch,
  Descriptions,
  InputNumber
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  ScheduleOutlined,
  PlayCircleOutlined,
  EyeOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  InfoCircleOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ScheduleJob, ScheduleLog, ReportTemplate } from '@/types'
import { getReportAll } from '@/api/report'

interface Schedule extends ScheduleJob {
  id: number
}

const mockReports: ReportTemplate[] = [
  { id: 1, name: '销售月报', code: 'sales_monthly', status: 2 },
  { id: 2, name: '财务分析报表', code: 'finance_analysis', status: 2 },
  { id: 3, name: '库存统计', code: 'inventory_stat', status: 2 },
  { id: 4, name: '客户分析', code: 'customer_analysis', status: 2 },
  { id: 6, name: '生产日报', code: 'production_daily', status: 2 }
]

const mockSchedules: Schedule[] = [
  { id: 1, name: '销售月报-每日凌晨', reportId: 1, reportName: '销售月报', cron: '0 0 2 * * ?', status: 1, createTime: '2024-01-01 10:00:00', lastExecuteTime: '2024-01-15 02:00:00' },
  { id: 2, name: '财务报表-每周一', reportId: 2, reportName: '财务分析报表', cron: '0 0 3 ? * MON', status: 1, createTime: '2024-01-02 11:00:00', lastExecuteTime: '2024-01-15 03:00:00' },
  { id: 3, name: '库存统计-每小时', reportId: 3, reportName: '库存统计', cron: '0 0 * * * ?', status: 0, createTime: '2024-01-03 14:00:00', lastExecuteTime: '2024-01-10 15:00:00' },
  { id: 4, name: '客户分析-每月1号', reportId: 4, reportName: '客户分析', cron: '0 0 4 1 * ?', status: 1, createTime: '2024-01-04 09:30:00', lastExecuteTime: '2024-01-01 04:00:00' },
  { id: 5, name: '生产日报-每天8点', reportId: 6, reportName: '生产日报', cron: '0 0 8 * * ?', status: 1, createTime: '2024-01-05 16:20:00', lastExecuteTime: '2024-01-15 08:00:00' }
]

const mockLogs: ScheduleLog[] = [
  { id: 1, jobId: 1, status: 1, message: '执行成功，生成文件：销售月报_20240115.xlsx', executeTime: '2024-01-15 02:00:00', duration: 3200 },
  { id: 2, jobId: 2, status: 1, message: '执行成功，生成文件：财务分析报表_20240115.pdf', executeTime: '2024-01-15 03:00:00', duration: 5500 },
  { id: 3, jobId: 5, status: 1, message: '执行成功，已发送邮件通知', executeTime: '2024-01-15 08:00:00', duration: 2100 },
  { id: 4, jobId: 1, status: 0, message: '执行失败：数据库连接超时', executeTime: '2024-01-14 02:00:00', duration: 30000 },
  { id: 5, jobId: 3, status: 1, message: '执行成功', executeTime: '2024-01-10 15:00:00', duration: 1800 }
]

const cronPresets = [
  { label: '每分钟', value: '0 * * * * ?' },
  { label: '每小时', value: '0 0 * * * ?' },
  { label: '每天凌晨2点', value: '0 0 2 * * ?' },
  { label: '每天上午8点', value: '0 0 8 * * ?' },
  { label: '每天晚上10点', value: '0 0 22 * * ?' },
  { label: '每周一凌晨3点', value: '0 0 3 ? * MON' },
  { label: '每月1号凌晨4点', value: '0 0 4 1 * ?' },
  { label: '每月最后一天23点', value: '0 0 23 L * ?' }
]

const ScheduleManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<Schedule[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [reportOptions, setReportOptions] = useState<ReportTemplate[]>([])

  const [searchForm] = Form.useForm()
  const [modalForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [logVisible, setLogVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [modalTitle, setModalTitle] = useState('新增调度')
  const [currentJobId, setCurrentJobId] = useState<number | null>(null)
  const [cronMode, setCronMode] = useState<'preset' | 'custom'>('preset')
  const [activeTab, setActiveTab] = useState('job')

  const fetchData = async () => {
    setLoading(true)
    try {
      const values = searchForm.getFieldsValue()
      const filtered = mockSchedules.filter(s =>
        (!values.keyword || s.name.includes(values.keyword))
      )
      setDataSource(filtered.slice((pageNum - 1) * pageSize, pageNum * pageSize))
      setTotal(filtered.length)
    } finally {
      setLoading(false)
    }
  }

  const fetchReports = async () => {
    try {
      const res = await getReportAll()
      setReportOptions(res)
    } catch {
      setReportOptions(mockReports)
    }
  }

  useEffect(() => {
    fetchData()
    fetchReports()
  }, [pageNum, pageSize])

  const handleSearch = () => {
    setPageNum(1)
    fetchData()
  }

  const handleReset = () => {
    searchForm.resetFields()
    setPageNum(1)
    fetchData()
  }

  const handleAdd = () => {
    setEditingId(null)
    setModalTitle('新增调度')
    setCronMode('preset')
    modalForm.resetFields()
    modalForm.setFieldsValue({ status: 1, cron: '0 0 2 * * ?' })
    setModalVisible(true)
  }

  const handleEdit = (record: Schedule) => {
    setEditingId(record.id!)
    setModalTitle('编辑调度')
    setCronMode(cronPresets.some(p => p.value === record.cron) ? 'preset' : 'custom')
    modalForm.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      if (editingId) {
        message.success('修改成功')
      } else {
        message.success('新增成功')
      }
      setModalVisible(false)
      fetchData()
    } catch {}
  }

  const handleDelete = async (id: number) => {
    try {
      message.success('删除成功')
      fetchData()
    } catch {
      message.error('删除失败')
    }
  }

  const handleToggleStatus = async (record: Schedule, checked: boolean) => {
    try {
      setDataSource(prev => prev.map(item => item.id === record.id ? { ...item, status: checked ? 1 : 0 } : item))
      message.success(checked ? '已启用' : '已停用')
    } catch {
      message.error('操作失败')
    }
  }

  const handleExecute = async (record: Schedule) => {
    try {
      message.loading({ content: '正在执行...', key: 'exec' })
      await new Promise(resolve => setTimeout(resolve, 1500))
      message.success({ content: '执行成功', key: 'exec' })
      fetchData()
    } catch (e: any) {
      message.error({ content: e.message || '执行失败', key: 'exec' })
    }
  }

  const handleViewLog = (record: Schedule) => {
    setCurrentJobId(record.id!)
    setLogVisible(true)
  }

  const jobColumns: ColumnsType<Schedule> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70
    },
    {
      title: '任务名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      render: (text: string) => (
        <Space>
          <ScheduleOutlined />
          {text}
        </Space>
      )
    },
    {
      title: '关联报表',
      dataIndex: 'reportName',
      key: 'reportName',
      width: 180
    },
    {
      title: 'Cron 表达式',
      dataIndex: 'cron',
      key: 'cron',
      width: 180,
      render: (text: string) => (
        <Tooltip title={text}>
          <code style={{ fontSize: 12 }}>{text}</code>
        </Tooltip>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      align: 'center',
      render: (status: number, record) => (
        <Switch
          checked={status === 1}
          checkedChildren="启用"
          unCheckedChildren="停用"
          onChange={(checked) => handleToggleStatus(record, checked)}
        />
      )
    },
    {
      title: '上次执行',
      dataIndex: 'lastExecuteTime',
      key: 'lastExecuteTime',
      width: 180,
      render: (text: string) => text || '-'
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" size="small" icon={<PlayCircleOutlined />} onClick={() => handleExecute(record)}>
            执行
          </Button>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewLog(record)}>
            日志
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除该调度任务?" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const logColumns: ColumnsType<ScheduleLog> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70
    },
    {
      title: '执行状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      align: 'center',
      render: (status: number) => (
        <Space>
          {status === 1
            ? <CheckCircleOutlined style={{ color: '#52c41a' }} />
            : <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
          }
          <Tag color={status === 1 ? 'green' : 'red'}>
            {status === 1 ? '成功' : '失败'}
          </Tag>
        </Space>
      )
    },
    {
      title: '执行结果',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true
    },
    {
      title: '执行时间',
      dataIndex: 'executeTime',
      key: 'executeTime',
      width: 180,
      render: (text: string) => (
        <Space>
          <ClockCircleOutlined />
          {text}
        </Space>
      )
    },
    {
      title: '耗时',
      dataIndex: 'duration',
      key: 'duration',
      width: 120,
      align: 'center',
      render: (duration?: number) => {
        if (!duration) return '-'
        if (duration < 1000) return `${duration}ms`
        return `${(duration / 1000).toFixed(2)}s`
      }
    }
  ]

  const filterLogs = currentJobId ? mockLogs.filter(l => l.jobId === currentJobId) : mockLogs

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card size="small">
        <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
          {
            key: 'job',
            label: '调度任务',
            children: null
          },
          {
            key: 'log',
            label: '执行日志',
            children: null
          }
        ]} />
      </Card>

      {activeTab === 'job' && (
        <>
          <Card size="small">
            <Form form={searchForm} layout="inline">
              <Row gutter={16}>
                <Col>
                  <Form.Item name="keyword" label="关键字">
                    <Input placeholder="任务名称" allowClear style={{ width: 200 }} />
                  </Form.Item>
                </Col>
                <Col>
                  <Form.Item>
                    <Space>
                      <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                        查询
                      </Button>
                      <Button icon={<ReloadOutlined />} onClick={handleReset}>
                        重置
                      </Button>
                    </Space>
                  </Form.Item>
                </Col>
              </Row>
            </Form>
          </Card>

          <Card
            size="small"
            title="调度任务列表"
            extra={
              <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
                新增调度
              </Button>
            }
          >
            <Table
              rowKey="id"
              loading={loading}
              columns={jobColumns}
              dataSource={dataSource}
              pagination={false}
              rowSelection={{
                selectedRowKeys,
                onChange: setSelectedRowKeys
              }}
              scroll={{ x: 1250 }}
            />
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Pagination
                current={pageNum}
                pageSize={pageSize}
                total={total}
                showSizeChanger
                showQuickJumper
                showTotal={(total) => `共 ${total} 条`}
                onChange={(page, size) => {
                  setPageNum(page)
                  setPageSize(size)
                }}
              />
            </div>
          </Card>
        </>
      )}

      {activeTab === 'log' && (
        <Card size="small" title="全部执行日志">
          <Table
            rowKey="id"
            columns={logColumns}
            dataSource={mockLogs}
            pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
            scroll={{ x: 800 }}
          />
        </Card>
      )}

      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
        width={640}
        okText="保存"
      >
        <Form form={modalForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="任务名称"
                rules={[{ required: true, message: '请输入任务名称' }]}
              >
                <Input placeholder="请输入任务名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="reportId"
                label="关联报表"
                rules={[{ required: true, message: '请选择报表' }]}
              >
                <Select
                  placeholder="请选择报表"
                  options={reportOptions.filter(r => r.status === 2).map(r => ({
                    label: r.name,
                    value: r.id
                  }))}
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item label="Cron 表达式配置">
            <Radio.Group value={cronMode} onChange={(e) => setCronMode(e.target.value)} style={{ marginBottom: 12 }}>
              <Radio value="preset">预设表达式</Radio>
              <Radio value="custom">自定义</Radio>
            </Radio.Group>
            {cronMode === 'preset' ? (
              <Form.Item name="cron" noStyle rules={[{ required: true, message: '请选择 Cron 表达式' }]}>
                <Select
                  placeholder="选择常用的 Cron 表达式"
                  options={cronPresets.map(p => ({ label: `${p.label} (${p.value})`, value: p.value }))}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            ) : (
              <Space.Compact style={{ width: '100%' }}>
                <Form.Item name="cron" noStyle rules={[{ required: true, message: '请输入 Cron 表达式' }]}>
                  <Input placeholder="如：0 0 2 * * ?" />
                </Form.Item>
                <Tooltip title="Cron 表达式格式：秒 分 时 日 月 周 (年)，如 0 0 2 * * ? 表示每天凌晨2点">
                  <Button icon={<InfoCircleOutlined />} />
                </Tooltip>
              </Space.Compact>
            )}
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="status" label="状态" initialValue={1} valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="停用" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="params" label="参数配置 (JSON)">
            <Input.TextArea placeholder='{"param1": "value1"}' rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="执行日志"
        open={logVisible}
        onCancel={() => setLogVisible(false)}
        width={900}
        footer={[
          <Button key="close" onClick={() => setLogVisible(false)}>
            关闭
          </Button>
        ]}
      >
        <Descriptions size="small" bordered column={2} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="任务ID">{currentJobId}</Descriptions.Item>
          <Descriptions.Item label="任务名称">
            {dataSource.find(s => s.id === currentJobId)?.name || '-'}
          </Descriptions.Item>
        </Descriptions>
        <Table
          size="small"
          rowKey="id"
          columns={logColumns}
          dataSource={filterLogs}
          pagination={{ pageSize: 5 }}
        />
      </Modal>
    </Space>
  )
}

export default ScheduleManagement
