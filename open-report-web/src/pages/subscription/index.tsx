import { useState, useEffect } from 'react'
import {
  Table, Button, Space, Form, Input, Select, Modal, message, Popconfirm,
  Card, Row, Col, Tag, Pagination, Tabs, Switch, Descriptions, InputNumber,
  Divider, Alert, TimePicker, Checkbox, Tooltip
} from 'antd'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined,
  SearchOutlined, BellOutlined, SendOutlined, EyeOutlined,
  CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined,
  DingtalkOutlined, MailOutlined, TeamOutlined, InfoCircleOutlined,
  FileExcelOutlined, FilePdfOutlined, WarningOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ReportSubscription, SubscriptionNotifyLog, ReportTemplate } from '@/types'
import { getReportAll } from '@/api/report'
import {
  getSubscriptionList, createSubscription, updateSubscription,
  deleteSubscription, manualPushSubscription, toggleSubscriptionStatus,
  getSubscriptionNotifyLogList
} from '@/api/subscription'
import dayjs from 'dayjs'

const mockReports: ReportTemplate[] = [
  { id: 1, templateName: '销售月报', status: 2 },
  { id: 2, templateName: '财务分析报表', status: 2 },
  { id: 3, templateName: '库存统计', status: 2 },
  { id: 4, templateName: '客户分析', status: 2 },
  { id: 6, templateName: '生产日报', status: 2 }
]

const channelOptions = [
  { label: '钉钉', value: 'DINGTALK', icon: <DingtalkOutlined /> },
  { label: '企业微信', value: 'WECOM', icon: <TeamOutlined /> },
  { label: '邮件', value: 'EMAIL', icon: <MailOutlined /> }
]

const frequencyOptions = [
  { label: '每天', value: 'DAILY' },
  { label: '每周', value: 'WEEKLY' },
  { label: '每月', value: 'MONTHLY' }
]

const messageFormatOptions = [
  { label: 'Markdown', value: 'MARKDOWN' },
  { label: '卡片消息', value: 'CARD' },
  { label: '纯文本', value: 'TEXT' }
]

const weekDayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]

const SubscriptionManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<ReportSubscription[]>([])
  const [logDataSource, setLogDataSource] = useState<SubscriptionNotifyLog[]>([])
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
  const [modalTitle, setModalTitle] = useState('新增订阅')
  const [currentSubId, setCurrentSubId] = useState<number | null>(null)
  const [activeTab, setActiveTab] = useState('subscription')
  const [selectedChannels, setSelectedChannels] = useState<string[]>([])

  const reportMap = new Map<number, string>()
  reportOptions.forEach(r => { reportMap.set(r.id!, r.templateName || '') })

  const fetchData = async () => {
    setLoading(true)
    try {
      const values = searchForm.getFieldsValue()
      const params: any = { pageNum, pageSize }
      if (values.reportId) params.reportId = values.reportId
      if (values.channel) params.channel = values.channel
      if (values.status !== undefined && values.status !== '') params.status = values.status

      const res: any = await getSubscriptionList(params)
      const list = (res?.list || []).map((item: ReportSubscription) => ({
        ...item,
        reportName: reportMap.get(item.reportId) || item.reportName || '-'
      }))
      setDataSource(list)
      setTotal(res?.total || 0)
    } catch (e) {
      console.error('获取订阅列表失败', e)
      setDataSource([])
      setTotal(0)
    } finally {
      setLoading(false)
    }
  }

  const fetchLogs = async (subscriptionId?: number) => {
    try {
      const params: any = { pageNum: logPageNum, pageSize: logPageSize }
      if (subscriptionId) params.subscriptionId = subscriptionId
      const res: any = await getSubscriptionNotifyLogList(params)
      setLogDataSource(res?.list || [])
      setLogTotal(res?.total || 0)
    } catch (e) {
      console.error('获取推送日志失败', e)
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

  useEffect(() => { fetchReports() }, [])

  useEffect(() => {
    if (reportOptions.length > 0) fetchData()
  }, [pageNum, pageSize, reportOptions])

  useEffect(() => {
    if (activeTab === 'log') fetchLogs()
  }, [activeTab, logPageNum, logPageSize])

  const handleSearch = () => { setPageNum(1); fetchData() }
  const handleReset = () => { searchForm.resetFields(); setPageNum(1); fetchData() }

  const handleAdd = () => {
    setEditingId(null)
    setModalTitle('新增订阅')
    setSelectedChannels([])
    modalForm.resetFields()
    modalForm.setFieldsValue({
      status: 1,
      frequency: 'DAILY',
      messageFormat: 'MARKDOWN',
      pushTime: dayjs('09:00', 'HH:mm'),
      maxRetryCount: 3,
      includeChart: false,
      includeAttachment: false,
      attachmentType: 'EXCEL',
      pushDayOfWeek: 1,
      pushDayOfMonth: 1
    })
    setModalVisible(true)
  }

  const handleEdit = (record: ReportSubscription) => {
    setEditingId(record.id!)
    setModalTitle('编辑订阅')
    const channels = (record.channels || '').split(',').filter(Boolean)
    setSelectedChannels(channels)
    modalForm.setFieldsValue({
      ...record,
      pushTime: record.pushTime ? dayjs(record.pushTime, 'HH:mm:ss') : dayjs('09:00', 'HH:mm'),
      channels
    })
    setModalVisible(true)
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      const payload: any = {
        ...values,
        channels: (values.channels || []).join(','),
        pushTime: values.pushTime ? values.pushTime.format('HH:mm:ss') : '09:00:00'
      }

      if (editingId) {
        payload.id = editingId
        await updateSubscription(payload)
        message.success('修改成功')
      } else {
        await createSubscription(payload)
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
      await deleteSubscription(id)
      message.success('删除成功')
      fetchData()
    } catch {
      message.error('删除失败')
    }
  }

  const handleToggleStatus = async (record: ReportSubscription, checked: boolean) => {
    try {
      await toggleSubscriptionStatus(record.id!, checked)
      message.success(checked ? '已启用' : '已停用')
      fetchData()
    } catch {
      message.error('操作失败')
    }
  }

  const handleManualPush = async (record: ReportSubscription) => {
    try {
      message.loading({ content: '正在推送...', key: 'push' })
      await manualPushSubscription(record.id!)
      message.success({ content: '推送任务已提交', key: 'push' })
    } catch (e: any) {
      message.error({ content: e.message || '推送失败', key: 'push' })
    }
  }

  const handleViewLog = (record: ReportSubscription) => {
    setCurrentSubId(record.id!)
    setLogVisible(true)
    fetchLogs(record.id!)
  }

  const renderChannelTags = (channels: string) => {
    if (!channels) return '-'
    return channels.split(',').filter(Boolean).map(ch => {
      switch (ch) {
        case 'DINGTALK': return <Tag key={ch} color="blue" icon={<DingtalkOutlined />}>钉钉</Tag>
        case 'WECOM': return <Tag key={ch} color="green" icon={<TeamOutlined />}>企微</Tag>
        case 'EMAIL': return <Tag key={ch} color="orange" icon={<MailOutlined />}>邮件</Tag>
        default: return <Tag key={ch}>{ch}</Tag>
      }
    })
  }

  const renderFrequency = (freq?: string, dayOfWeek?: number, dayOfMonth?: number) => {
    switch (freq) {
      case 'DAILY': return <Tag color="blue">每天</Tag>
      case 'WEEKLY': {
        const dayName = weekDayOptions.find(d => d.value === dayOfWeek)?.label || '周一'
        return <Tag color="purple">每周 {dayName}</Tag>
      }
      case 'MONTHLY': return <Tag color="cyan">每月 {dayOfMonth || 1}日</Tag>
      default: return <Tag>{freq || '-'}</Tag>
    }
  }

  const renderMessageFormat = (format?: string) => {
    switch (format) {
      case 'MARKDOWN': return <Tag color="blue">Markdown</Tag>
      case 'CARD': return <Tag color="purple">卡片消息</Tag>
      case 'TEXT': return <Tag>纯文本</Tag>
      default: return <Tag>{format || '-'}</Tag>
    }
  }

  const renderNotifyStatus = (status?: string) => {
    switch (status) {
      case 'SUCCESS': return <Tag color="green" icon={<CheckCircleOutlined />}>成功</Tag>
      case 'FAIL': return <Tag color="red" icon={<CloseCircleOutlined />}>失败</Tag>
      case 'PENDING': return <Tag color="blue" icon={<ClockCircleOutlined />}>待推送</Tag>
      case 'RETRY': return <Tag color="orange" icon={<WarningOutlined />}>重试中</Tag>
      default: return <Tag>{status || '-'}</Tag>
    }
  }

  const renderNotifyChannel = (channel?: string) => {
    switch (channel) {
      case 'DINGTALK': return <Tag color="blue" icon={<DingtalkOutlined />}>钉钉</Tag>
      case 'WECOM': return <Tag color="green" icon={<TeamOutlined />}>企微</Tag>
      case 'EMAIL': return <Tag color="orange" icon={<MailOutlined />}>邮件</Tag>
      default: return <Tag>{channel || '-'}</Tag>
    }
  }

  const subColumns: ColumnsType<ReportSubscription> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    {
      title: '订阅名称', dataIndex: 'name', key: 'name', width: 180,
      render: (text: string) => <Space><BellOutlined />{text}</Space>
    },
    { title: '关联报表', dataIndex: 'reportName', key: 'reportName', width: 140 },
    { title: '推送渠道', dataIndex: 'channels', key: 'channels', width: 180, render: renderChannelTags },
    {
      title: '推送频率', key: 'frequency', width: 120,
      render: (_, r) => renderFrequency(r.frequency, r.pushDayOfWeek, r.pushDayOfMonth)
    },
    { title: '消息格式', dataIndex: 'messageFormat', key: 'messageFormat', width: 100, render: renderMessageFormat },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 100, align: 'center' as const,
      render: (status: number, record) => (
        <Switch checked={status === 1} checkedChildren="启用" unCheckedChildren="停用"
          onChange={(checked) => handleToggleStatus(record, checked)} />
      )
    },
    { title: '上次推送', dataIndex: 'lastPushTime', key: 'lastPushTime', width: 170, render: (t: string) => t || '-' },
    { title: '下次推送', dataIndex: 'nextPushTime', key: 'nextPushTime', width: 170, render: (t: string) => t || '-' },
    {
      title: '操作', key: 'action', width: 260, fixed: 'right',
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" size="small" icon={<SendOutlined />} onClick={() => handleManualPush(record)}>推送</Button>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewLog(record)}>日志</Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除该订阅?" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const logColumns: ColumnsType<SubscriptionNotifyLog> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '推送渠道', dataIndex: 'channel', key: 'channel', width: 100, render: renderNotifyChannel },
    { title: '推送状态', dataIndex: 'status', key: 'status', width: 100, render: renderNotifyStatus },
    { title: '消息格式', dataIndex: 'messageFormat', key: 'messageFormat', width: 100, render: renderMessageFormat },
    { title: '重试次数', dataIndex: 'retryCount', key: 'retryCount', width: 80, align: 'center' as const },
    {
      title: '错误信息', dataIndex: 'errorMsg', key: 'errorMsg', ellipsis: true,
      render: (text: string) => text ? <Tooltip title={text}><span style={{ color: '#f56c6c' }}>{text}</span></Tooltip> : '-'
    },
    { title: '耗时', dataIndex: 'costTime', key: 'costTime', width: 100, align: 'center' as const,
      render: (d?: number) => {
        if (!d) return '-'
        return d < 1000 ? `${d}ms` : `${(d / 1000).toFixed(2)}s`
      }
    },
    { title: '推送时间', dataIndex: 'createTime', key: 'createTime', width: 170 }
  ]

  const detailLogs = currentSubId
    ? logDataSource.filter(l => l.subscriptionId === currentSubId)
    : logDataSource

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card size="small">
        <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
          { key: 'subscription', label: '订阅管理' },
          { key: 'log', label: '推送日志' }
        ]} />
      </Card>

      {activeTab === 'subscription' && (
        <>
          <Card size="small">
            <Form form={searchForm} layout="inline">
              <Row gutter={16}>
                <Col>
                  <Form.Item name="reportId" label="关联报表">
                    <Select placeholder="全部报表" allowClear style={{ width: 180 }}
                      options={reportOptions.filter(r => r.status === 2).map(r => ({
                        label: r.templateName, value: r.id
                      }))} />
                  </Form.Item>
                </Col>
                <Col>
                  <Form.Item name="channel" label="推送渠道">
                    <Select placeholder="全部渠道" allowClear style={{ width: 140 }}
                      options={channelOptions.map(c => ({ label: c.label, value: c.value }))} />
                  </Form.Item>
                </Col>
                <Col>
                  <Form.Item name="status" label="状态">
                    <Select placeholder="全部状态" allowClear style={{ width: 120 }}
                      options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
                  </Form.Item>
                </Col>
                <Col>
                  <Form.Item>
                    <Space>
                      <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>查询</Button>
                      <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
                    </Space>
                  </Form.Item>
                </Col>
              </Row>
            </Form>
          </Card>

          <Card size="small" title="订阅列表" extra={
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增订阅</Button>
          }>
            <Table rowKey="id" loading={loading} columns={subColumns} dataSource={dataSource}
              pagination={false} scroll={{ x: 1500 }} />
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Pagination current={pageNum} pageSize={pageSize} total={total}
                showSizeChanger showQuickJumper showTotal={(t) => `共 ${t} 条`}
                onChange={(page, size) => { setPageNum(page); setPageSize(size) }} />
            </div>
          </Card>
        </>
      )}

      {activeTab === 'log' && (
        <Card size="small" title="全部推送日志">
          <Table rowKey="id" columns={logColumns} dataSource={logDataSource}
            pagination={{
              current: logPageNum, pageSize: logPageSize, total: logTotal,
              showSizeChanger: true, showTotal: (t) => `共 ${t} 条`,
              onChange: (page, size) => { setLogPageNum(page); setLogPageSize(size) }
            }} scroll={{ x: 1000 }} />
        </Card>
      )}

      <Modal title={modalTitle} open={modalVisible} onOk={handleModalOk}
        onCancel={() => setModalVisible(false)} destroyOnClose width={800} okText="保存">
        <Form form={modalForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="name" label="订阅名称" rules={[{ required: true, message: '请输入订阅名称' }]}>
                <Input placeholder="如：销售月报每日钉钉推送" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="reportId" label="关联报表" rules={[{ required: true, message: '请选择报表' }]}>
                <Select placeholder="请选择报表"
                  options={reportOptions.filter(r => r.status === 2).map(r => ({
                    label: r.templateName, value: r.id
                  }))} />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="frequency" label="推送频率" rules={[{ required: true }]}>
                <Select options={frequencyOptions} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="pushTime" label="推送时间">
                <TimePicker format="HH:mm" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="messageFormat" label="消息格式" rules={[{ required: true }]}>
                <Select options={messageFormatOptions} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item noStyle shouldUpdate={(prev, cur) => prev.frequency !== cur.frequency}>
            {({ getFieldValue }) => {
              const freq = getFieldValue('frequency')
              return (
                <Row gutter={16}>
                  {freq === 'WEEKLY' && (
                    <Col span={8}>
                      <Form.Item name="pushDayOfWeek" label="推送日">
                        <Select options={weekDayOptions} />
                      </Form.Item>
                    </Col>
                  )}
                  {freq === 'MONTHLY' && (
                    <Col span={8}>
                      <Form.Item name="pushDayOfMonth" label="推送日">
                        <InputNumber min={1} max={31} style={{ width: '100%' }} placeholder="1-31" />
                      </Form.Item>
                    </Col>
                  )}
                  <Col span={8}>
                    <Form.Item name="maxRetryCount" label="最大重试次数" initialValue={3}>
                      <InputNumber min={0} max={10} style={{ width: '100%' }} />
                    </Form.Item>
                  </Col>
                  <Col span={8}>
                    <Form.Item name="status" label="状态" initialValue={1} valuePropName="checked">
                      <Switch checkedChildren="启用" unCheckedChildren="停用" />
                    </Form.Item>
                  </Col>
                </Row>
              )
            }}
          </Form.Item>

          <Divider orientation="left"><BellOutlined /> 推送渠道</Divider>
          <Form.Item name="channels" label="选择推送渠道" rules={[{ required: true, message: '请选择至少一个推送渠道' }]}>
            <Checkbox.Group onChange={(vals) => setSelectedChannels(vals as string[])}>
              {channelOptions.map(c => (
                <Checkbox key={c.value} value={c.value}>{c.icon} {c.label}</Checkbox>
              ))}
            </Checkbox.Group>
          </Form.Item>

          {selectedChannels.includes('DINGTALK') && (
            <>
              <Divider orientation="left"><DingtalkOutlined /> 钉钉配置</Divider>
              <Row gutter={16}>
                <Col span={16}>
                  <Form.Item name="dingtalkWebhook" label="Webhook地址"
                    rules={selectedChannels.includes('DINGTALK') ? [{ required: true, message: '请输入Webhook地址' }] : []}>
                    <Input placeholder="https://oapi.dingtalk.com/robot/send?access_token=..." />
                  </Form.Item>
                </Col>
                <Col span={8}>
                  <Form.Item name="dingtalkSecret" label="加签密钥">
                    <Input.Password placeholder="安全设置中的加签密钥" />
                  </Form.Item>
                </Col>
              </Row>
            </>
          )}

          {selectedChannels.includes('WECOM') && (
            <>
              <Divider orientation="left"><TeamOutlined /> 企业微信配置</Divider>
              <Form.Item name="wecomWebhook" label="Webhook地址"
                rules={selectedChannels.includes('WECOM') ? [{ required: true, message: '请输入Webhook地址' }] : []}>
                <Input placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..." />
              </Form.Item>
            </>
          )}

          {selectedChannels.includes('EMAIL') && (
            <>
              <Divider orientation="left"><MailOutlined /> 邮件配置</Divider>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="emailList" label="收件人"
                    rules={selectedChannels.includes('EMAIL') ? [{ required: true, message: '请输入收件人' }] : []}>
                    <Input placeholder="多个邮箱用逗号分隔" />
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
            </>
          )}

          <Divider orientation="left"><InfoCircleOutlined /> 推送内容</Divider>
          <Form.Item name="contentTemplate" label="自定义内容模板">
            <Input.TextArea
              placeholder={`支持变量：${reportName}、${date}、${dateTime}\n留空将使用默认模板`}
              rows={4} />
          </Form.Item>

          <Row gutter={16}>
            <Col span={6}>
              <Form.Item name="includeChart" label="包含图表" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="includeAttachment" label="包含附件" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item noStyle shouldUpdate={(prev, cur) => prev.includeAttachment !== cur.includeAttachment}>
                {({ getFieldValue }) =>
                  getFieldValue('includeAttachment') ? (
                    <Form.Item name="attachmentType" label="附件类型">
                      <Select style={{ width: '100%' }} options={[
                        { label: 'Excel 文件', value: 'EXCEL' },
                        { label: 'PDF 文件', value: 'PDF' }
                      ]} />
                    </Form.Item>
                  ) : null
                }
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left"><InfoCircleOutlined /> 参数配置</Divider>
          <Form.Item name="params" label="报表参数 (JSON)">
            <Input.TextArea placeholder='如：{"startDate": "2024-01-01", "endDate": "2024-01-31"}' rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="推送日志" open={logVisible} onCancel={() => setLogVisible(false)} width={1000}
        footer={[<Button key="close" onClick={() => setLogVisible(false)}>关闭</Button>]}>
        <Descriptions size="small" bordered column={2} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="订阅ID">{currentSubId}</Descriptions.Item>
          <Descriptions.Item label="订阅名称">
            {dataSource.find(s => s.id === currentSubId)?.name || '-'}
          </Descriptions.Item>
        </Descriptions>
        <Table size="small" rowKey="id" columns={logColumns} dataSource={detailLogs}
          pagination={{ pageSize: 5 }} />
      </Modal>
    </Space>
  )
}

export default SubscriptionManagement
