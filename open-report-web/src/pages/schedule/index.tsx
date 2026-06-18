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
  InputNumber,
  Divider,
  Alert
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
  InfoCircleOutlined,
  MailOutlined,
  FileExcelOutlined,
  FilePdfOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ScheduleJob, ScheduleLog, ReportTemplate } from '@/types'
import { getReportAll } from '@/api/report'
import {
  getScheduleJobList,
  getScheduleLogList,
  createScheduleJob,
  updateScheduleJob,
  deleteScheduleJob,
  executeScheduleJob,
  toggleScheduleJobStatus
} from '@/api/schedule'

interface Schedule extends ScheduleJob {
  id: number
  reportName?: string
}

const mockReports: ReportTemplate[] = [
  { id: 1, templateName: '销售月报', status: 2 },
  { id: 2, templateName: '财务分析报表', status: 2 },
  { id: 3, templateName: '库存统计', status: 2 },
  { id: 4, templateName: '客户分析', status: 2 },
  { id: 6, templateName: '生产日报', status: 2 }
]

const cronPresets = [
  { label: '每分钟', value: '0 * * * * ?' },
  { label: '每小时', value: '0 0 * * * ?' },
  { label: '每天凌晨2点', value: '0 0 2 * * ?' },
  { label: '每天上午8点', value: '0 0 8 * * ?' },
  { label: '每天上午9点', value: '0 0 9 * * ?' },
  { label: '每天晚上10点', value: '0 0 22 * * ?' },
  { label: '每周一凌晨3点', value: '0 0 3 ? * MON' },
  { label: '每周一上午9点', value: '0 0 9 ? * MON' },
  { label: '每月1号凌晨4点', value: '0 0 4 1 * ?' },
  { label: '每月最后一天23点', value: '0 0 23 L * ?' }
]

const outputTypeOptions = [
  { label: 'Excel 文件', value: 'EXCEL', icon: <FileExcelOutlined /> },
  { label: 'PDF 文件', value: 'PDF', icon: <FilePdfOutlined /> },
  { label: '邮件发送（Excel）', value: 'EMAIL', icon: <MailOutlined /> }
]

const ScheduleManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<Schedule[]>([])
  const [logDataSource, setLogDataSource] = useState<ScheduleLog[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [total, setTotal] = useState(0)
  const [logTotal, setLogTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [logPageNum, setLogPageNum] = useState(1)
  const [logPageSize, setLogPageSize] = useState(10)
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

  const reportMap = new Map<number, string>()
  reportOptions.forEach(r => { reportMap.set(r.id!, r.templateName || '') })

  const fetchData = async () => {
    setLoading(true)
    try {
      const values = searchForm.getFieldsValue()
      const params: any = {
        pageNum,
        pageSize
      }
      if (values.reportId) params.reportId = values.reportId
      if (values.status !== undefined && values.status !== '') params.status = values.status

      const res: any = await getScheduleJobList(params)
      const list = (res?.list || []).map((item: Schedule) => ({
        ...item,
        reportName: reportMap.get(item.reportId) || item.reportName || '-'
      }))
      setDataSource(list)
      setTotal(res?.total || 0)
    } catch (e) {
      console.error('获取调度列表失败', e)
      setDataSource([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }

  const fetchLogs = async () => {
    try {
      const res: any = await getScheduleLogList({ pageNum: logPageNum, pageSize: logPageSize })
      setLogDataSource(res?.list || [])
      setLogTotal(res?.total || 0)
    } catch (e) {
      console.error('获取日志列表失败', e)
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
    fetchReports()
  }, [])

  useEffect(() => {
    if (reportOptions.length > 0) {
      fetchData()
    }
  }, [pageNum, pageSize, reportOptions])

  useEffect(() => {
    if (activeTab === 'log') {
      fetchLogs()
    }
  }, [activeTab, logPageNum, logPageSize])

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
    modalForm.setFieldsValue({
      status: 1,
      cron: '0 0 9 * * ?',
      outputType: 'EMAIL',
      maxRetryCount: 3
    })
    setModalVisible(true)
  }

  const handleEdit = (record: Schedule) => {
    setEditingId(record.id!)
    setModalTitle('编辑调度')
    const cron = record.cronExpression || record.cron
    setCronMode(cronPresets.some(p => p.value === cron) ? 'preset' : 'custom')
    modalForm.setFieldsValue({
      ...record,
      cron
    })
    setModalVisible(true)
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      const payload: any = {
        ...values,
        cronExpression: values.cron
      }
      delete payload.cron

      if (editingId) {
        payload.id = editingId
        await updateScheduleJob(payload)
        message.success('修改成功')
      } else {
        await createScheduleJob(payload)
        message.success('新增成功')
      }
      setModalVisible(false)
      fetchData()
    } catch (e: any) {
      if (e?.errorFields) return
      message.error(e?.message || '保存失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteScheduleJob(id)
      message.success('删除成功')
      fetchData()
    } catch {
      message.error('删除失败')
    }
  }

  const handleToggleStatus = async (record: Schedule, checked: boolean) => {
    try {
      await toggleScheduleJobStatus(record.id!, checked)
      message.success(checked ? '已启用' : '已停用')
      fetchData()
    } catch {
      message.error('操作失败')
    }
  }

  const handleExecute = async (record: Schedule) => {
    try {
      message.loading({ content: '正在执行...', key: 'exec' })
      await executeScheduleJob(record.id!)
      message.success({ content: '执行任务已提交', key: 'exec' })
    } catch (e: any) {
      message.error({ content: e.message || '执行失败', key: 'exec' })
    }
  }

  const handleViewLog = (record: Schedule) => {
    setCurrentJobId(record.id!)
    setLogVisible(true)
  }

  const renderStatusTag = (status: string | number | undefined) => {
    if (status === 'SUCCESS' || status === 1) {
      return <Tag color="green" icon={<CheckCircleOutlined />}>成功</Tag>
    }
    if (status === 'FAIL' || status === 0) {
      return <Tag color="red" icon={<CloseCircleOutlined />}>失败</Tag>
    }
    if (status === 'RUNNING') {
      return <Tag color="blue" icon={<ClockCircleOutlined />}>执行中</Tag>
    }
    return <Tag>未知</Tag>
  }

  const renderExecuteType = (type: string | undefined) => {
    switch (type) {
      case 'MANUAL': return <Tag>手动</Tag>
      case 'SCHEDULE': return <Tag color="blue">定时</Tag>
      case 'RETRY': return <Tag color="orange">重试</Tag>
      default: return <Tag>{type || '-'}</Tag>
    }
  }

  const renderOutputType = (type: string | undefined) => {
    switch (type) {
      case 'EXCEL': return <Tag icon={<FileExcelOutlined />} color="green">Excel</Tag>
      case 'PDF': return <Tag icon={<FilePdfOutlined />} color="red">PDF</Tag>
      case 'EMAIL': return <Tag icon={<MailOutlined />} color="blue">邮件</Tag>
      default: return <Tag>{type || '-'}</Tag>
    }
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
      width: 180,
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
      width: 160
    },
    {
      title: '输出类型',
      dataIndex: 'outputType',
      key: 'outputType',
      width: 100,
      align: 'center',
      render: renderOutputType
    },
    {
      title: 'Cron 表达式',
      dataIndex: 'cronExpression',
      key: 'cronExpression',
      width: 160,
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
      width: 170,
      render: (text: string) => text || '-'
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170
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
      title: '执行类型',
      dataIndex: 'executeType',
      key: 'executeType',
      width: 90,
      align: 'center',
      render: renderExecuteType
    },
    {
      title: '重试次数',
      dataIndex: 'retryCount',
      key: 'retryCount',
      width: 90,
      align: 'center',
      render: (v: number) => v || 0
    },
    {
      title: '执行状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      align: 'center',
      render: renderStatusTag
    },
    {
      title: '执行结果',
      dataIndex: 'errorMsg',
      key: 'errorMsg',
      ellipsis: true,
      render: (text: string, record) => text || record.message || (record.status === 'SUCCESS' ? '执行成功' : '-')
    },
    {
      title: '输出文件',
      dataIndex: 'outputPath',
      key: 'outputPath',
      width: 220,
      ellipsis: true,
      render: (text: string) => text ? <Tooltip title={text}><code style={{ fontSize: 11 }}>{text.split(/[\\/]/).pop()}</code></Tooltip> : '-'
    },
    {
      title: '执行时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 170,
      render: (text: string) => (
        <Space>
          <ClockCircleOutlined />
          {text}
        </Space>
      )
    },
    {
      title: '耗时',
      dataIndex: 'costTime',
      key: 'costTime',
      width: 100,
      align: 'center',
      render: (duration?: number) => {
        const d = duration ?? 0
        if (!d) return '-'
        if (d < 1000) return `${d}ms`
        return `${(d / 1000).toFixed(2)}s`
      }
    }
  ]

  const detailLogs = currentJobId ? logDataSource.filter(l => l.scheduleId === currentJobId || l.jobId === currentJobId) : logDataSource

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card size="small">
        <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
          { key: 'job', label: '调度任务' },
          { key: 'log', label: '执行日志' }
        ]} />
      </Card>

      {activeTab === 'job' && (
        <>
          <Card size="small">
            <Form form={searchForm} layout="inline">
              <Row gutter={16}>
                <Col>
                  <Form.Item name="reportId" label="关联报表">
                    <Select
                      placeholder="全部报表"
                      allowClear
                      style={{ width: 200 }}
                      options={reportOptions.filter(r => r.status === 2).map(r => ({
                        label: r.templateName,
                        value: r.id
                      }))}
                    />
                  </Form.Item>
                </Col>
                <Col>
                  <Form.Item name="status" label="状态">
                    <Select
                      placeholder="全部状态"
                      allowClear
                      style={{ width: 140 }}
                      options={[
                        { label: '启用', value: 1 },
                        { label: '停用', value: 0 }
                      ]}
                    />
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
              scroll={{ x: 1300 }}
            />
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Pagination
                current={pageNum}
                pageSize={pageSize}
                total={total}
                showSizeChanger
                showQuickJumper
                showTotal={(t) => `共 ${t} 条`}
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
            dataSource={logDataSource}
            pagination={{
              current: logPageNum,
              pageSize: logPageSize,
              total: logTotal,
              showSizeChanger: true,
              showTotal: (t) => `共 ${t} 条`,
              onChange: (page, size) => { setLogPageNum(page); setLogPageSize(size) }
            }}
            scroll={{ x: 1100 }}
          />
        </Card>
      )}

      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
        width={720}
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
                <Input placeholder="请输入任务名称，如：销售月报每日推送" />
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
                    label: r.templateName,
                    value: r.id
                  }))}
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item label="Cron 表达式配置" required>
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
                  <Input placeholder="如：0 0 9 * * ? (每天上午9点)" />
                </Form.Item>
                <Tooltip title="Cron 表达式格式：秒 分 时 日 月 周 (年)，如 0 0 9 * * ? 表示每天上午9点">
                  <Button icon={<InfoCircleOutlined />} />
                </Tooltip>
              </Space.Compact>
            )}
          </Form.Item>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="outputType"
                label="输出类型"
                rules={[{ required: true, message: '请选择输出类型' }]}
              >
                <Select
                  options={outputTypeOptions.map(o => ({ label: o.label, value: o.value }))}
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                name="maxRetryCount"
                label="最大重试次数"
                initialValue={3}
              >
                <InputNumber min={0} max={10} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="status" label="状态" initialValue={1} valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="停用" />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left"><MailOutlined /> 邮件配置</Divider>
          <Alert
            type="info"
            showIcon
            message="邮件相关配置仅在输出类型为「邮件发送」或填写了收件人时生效"
            style={{ marginBottom: 16 }}
          />

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="emailList" label="收件人">
                <Input placeholder="多个邮箱用逗号分隔，如 a@example.com,b@example.com" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="emailCcList" label="抄送">
                <Input placeholder="多个邮箱用逗号分隔" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="emailSubject" label="邮件主题">
            <Input placeholder="留空将使用默认主题" />
          </Form.Item>
          <Form.Item name="emailContent" label="邮件正文">
            <Input.TextArea placeholder="留空将使用默认正文，支持换行" rows={3} />
          </Form.Item>

          <Divider orientation="left"><InfoCircleOutlined /> 参数配置</Divider>
          <Form.Item name="params" label="报表参数 (JSON)">
            <Input.TextArea placeholder='如：{"startDate": "2024-01-01", "endDate": "2024-01-31"}' rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="执行日志"
        open={logVisible}
        onCancel={() => setLogVisible(false)}
        width={1000}
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
          dataSource={detailLogs}
          pagination={{ pageSize: 5 }}
        />
      </Modal>
    </Space>
  )
}

export default ScheduleManagement
