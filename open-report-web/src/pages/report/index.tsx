import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
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
  Upload,
  Switch,
  Descriptions,
  InputNumber,
  Alert
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  CopyOutlined,
  EyeOutlined,
  DesignOutlined,
  CloudUploadOutlined,
  CloudDownloadOutlined,
  HistoryOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  UndoOutlined,
  SyncOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ReportTemplate, PageParams, ReportApproval } from '@/types'
import {
  getReportListV2,
  createReport,
  updateReport,
  deleteReport,
  batchDeleteReport,
  copyReport,
  exportReportExcel,
  exportReportPdf,
  submitApproval,
  cancelApproval,
  getApprovalByTemplateId
} from '@/api/report'
import VersionManagement from '@/components/VersionManagement'
import { useReportWebSocket } from '@/hooks/useWebSocket'

interface Report extends ReportTemplate {
  id: number
  name?: string
  code?: string
  type?: number
  remark?: string
}

const ReportManagement = () => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<Report[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  const [searchForm] = Form.useForm()
  const [modalForm] = Form.useForm()
  const [approvalForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [previewVisible, setPreviewVisible] = useState(false)
  const [approvalModalVisible, setApprovalModalVisible] = useState(false)
  const [versionModalVisible, setVersionModalVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [modalTitle, setModalTitle] = useState('新增报表')
  const [previewReport, setPreviewReport] = useState<Report | null>(null)
  const [currentReport, setCurrentReport] = useState<Report | null>(null)
  const [approvalHistory, setApprovalHistory] = useState<ReportApproval[]>([])
  const refreshLockRef = useRef(false)

  const { isConnected, shouldRefresh, acknowledgeRefresh, lastMessage } = useReportWebSocket(
    undefined,
    () => {
      if (!refreshLockRef.current) {
        refreshLockRef.current = true
        setTimeout(() => {
          fetchData().finally(() => {
            refreshLockRef.current = false
            acknowledgeRefresh()
          })
        }, 300)
      }
    }
  )

  const fetchData = async () => {
    setLoading(true)
    try {
      const values = searchForm.getFieldsValue()
      const params = {
        pageNum,
        pageSize,
        keyword: values.keyword,
        status: values.status
      }
      const res = await getReportListV2(params)
      setDataSource((res.list || []).map(item => ({
        ...item,
        name: item.templateName,
        code: item.templateCode,
        type: item.templateType,
        remark: item.description
      })))
      setTotal(res.total || 0)
    } catch (e) {
      message.error('获取列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
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
    setModalTitle('新增报表')
    modalForm.resetFields()
    modalForm.setFieldsValue({ templateType: 1, status: 0 })
    setModalVisible(true)
  }

  const handleEdit = (record: Report) => {
    setEditingId(record.id!)
    setModalTitle('编辑报表')
    modalForm.setFieldsValue({
      ...record,
      templateName: record.templateName,
      templateCode: record.templateCode,
      templateType: record.templateType,
      description: record.description
    })
    setModalVisible(true)
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      const templateData = {
        ...values,
        templateName: values.templateName,
        templateCode: values.templateCode,
        templateType: values.templateType,
        description: values.description
      }
      if (editingId) {
        await updateReport({ ...templateData, id: editingId })
        message.success('修改成功')
      } else {
        await createReport(templateData)
        message.success('新增成功')
      }
      setModalVisible(false)
      fetchData()
    } catch {
      message.error('操作失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteReport(id)
      message.success('删除成功')
      fetchData()
    } catch {
      message.error('删除失败')
    }
  }

  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的记录')
      return
    }
    try {
      await batchDeleteReport(selectedRowKeys.map(k => Number(k)))
      message.success(`批量删除成功，共删除 ${selectedRowKeys.length} 条记录`)
      setSelectedRowKeys([])
      fetchData()
    } catch {
      message.error('批量删除失败')
    }
  }

  const handleCopy = async (record: Report) => {
    try {
      await copyReport(record.id!)
      message.success('复制成功')
      fetchData()
    } catch {
      message.error('复制失败')
    }
  }

  const handleSubmitApproval = (record: Report) => {
    setCurrentReport(record)
    approvalForm.resetFields()
    setApprovalModalVisible(true)
  }

  const handleApprovalOk = async () => {
    try {
      const values = await approvalForm.validateFields()
      if (currentReport) {
        await submitApproval(currentReport.id!, values.remark)
        message.success('提交审批成功')
        setApprovalModalVisible(false)
        fetchData()
      }
    } catch {
      message.error('提交审批失败')
    }
  }

  const handleCancelApproval = async (record: Report) => {
    try {
      const approvals = await getApprovalByTemplateId(record.id!)
      const pendingApproval = approvals.find(a => a.approvalStatus === 0)
      if (pendingApproval) {
        await cancelApproval(pendingApproval.id!)
        message.success('已撤销审批')
        fetchData()
      }
    } catch {
      message.error('撤销审批失败')
    }
  }

  const handleViewApprovalHistory = async (record: Report) => {
    try {
      const res = await getApprovalByTemplateId(record.id!)
      setApprovalHistory(res)
      setCurrentReport(record)
    } catch {
      message.error('获取审批历史失败')
    }
  }

  const handleDesign = (record: Report) => {
    navigate(`/designer/${record.id}`)
  }

  const handlePreview = (record: Report) => {
    setPreviewReport(record)
    setPreviewVisible(true)
  }

  const handleVersionManagement = (record: Report) => {
    setCurrentReport(record)
    setVersionModalVisible(true)
  }

  const handleExportExcel = async (record: Report) => {
    try {
      await exportReportExcel(record.id!)
      message.success('导出成功')
    } catch {
      message.error('导出失败')
    }
  }

  const handleExportPdf = async (record: Report) => {
    try {
      await exportReportPdf(record.id!)
      message.success('导出成功')
    } catch {
      message.error('导出失败')
    }
  }

  const getStatusTag = (status?: number) => {
    const statusMap: Record<number, { color: string; text: string }> = {
      0: { color: 'default', text: '草稿' },
      1: { color: 'orange', text: '待审批' },
      2: { color: 'green', text: '已发布' },
      3: { color: 'blue', text: '已下线' },
      4: { color: 'red', text: '已驳回' }
    }
    const info = statusMap[status || 0]
    return <Tag color={info.color}>{info.text}</Tag>
  }

  const getApprovalStatusTag = (status?: number) => {
    const statusMap: Record<number, { color: string; text: string }> = {
      0: { color: 'orange', text: '待审批' },
      1: { color: 'green', text: '已通过' },
      2: { color: 'red', text: '已驳回' },
      3: { color: 'default', text: '已撤销' }
    }
    const info = statusMap[status || 0]
    return <Tag color={info.color}>{info.text}</Tag>
  }

  const getTypeTag = (type?: number) => {
    const typeMap: Record<number, { color: string; text: string }> = {
      1: { color: 'blue', text: '表格报表' },
      2: { color: 'purple', text: '图表报表' },
      3: { color: 'cyan', text: '混合报表' }
    }
    const info = typeMap[type || 1]
    return <Tag color={info.color}>{info.text}</Tag>
  }

  const getActionButtons = (record: Report) => {
    const buttons = []

    buttons.push(
      <Button key="design" type="link" size="small" icon={<DesignOutlined />} onClick={() => handleDesign(record)}>
        设计
      </Button>
    )

    buttons.push(
      <Button key="preview" type="link" size="small" icon={<EyeOutlined />} onClick={() => handlePreview(record)}>
        预览
      </Button>
    )

    buttons.push(
      <Button key="version" type="link" size="small" icon={<HistoryOutlined />} onClick={() => handleVersionManagement(record)}>
        版本
      </Button>
    )

    if (record.status === 0) {
      buttons.push(
        <Button key="submit" type="link" size="small" icon={<CheckCircleOutlined />} onClick={() => handleSubmitApproval(record)}>
          提交审批
        </Button>
      )
    }

    if (record.status === 1) {
      buttons.push(
        <Button key="cancel" type="link" size="small" icon={<CloseCircleOutlined />} onClick={() => handleCancelApproval(record)}>
          撤销审批
        </Button>
      )
    }

    if (record.status === 2) {
      buttons.push(
        <Button key="offline" type="link" size="small" icon={<PauseCircleOutlined />}>
          申请下线
        </Button>
      )
    }

    if (record.status === 4) {
      buttons.push(
        <Button key="resubmit" type="link" size="small" icon={<UndoOutlined />} onClick={() => handleSubmitApproval(record)}>
          重新提交
        </Button>
      )
    }

    buttons.push(
      <Button key="copy" type="link" size="small" icon={<CopyOutlined />} onClick={() => handleCopy(record)}>
        复制
      </Button>
    )

    buttons.push(
      <Button key="edit" type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
        编辑
      </Button>
    )

    buttons.push(
      <Popconfirm key="delete" title="确定删除该报表?" onConfirm={() => handleDelete(record.id!)}>
        <Button type="link" size="small" danger icon={<DeleteOutlined />}>
          删除
        </Button>
      </Popconfirm>
    )

    return buttons
  }

  const columns: ColumnsType<Report> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70
    },
    {
      title: '报表名称',
      dataIndex: 'templateName',
      key: 'templateName',
      width: 180,
      render: (text: string) => (
        <Space>
          <FileTextOutlined />
          {text}
        </Space>
      )
    },
    {
      title: '编码',
      dataIndex: 'templateCode',
      key: 'templateCode',
      width: 160,
      render: (text: string) => <code>{text || '-'}</code>
    },
    {
      title: '类型',
      dataIndex: 'templateType',
      key: 'templateType',
      width: 110,
      render: (type: number) => getTypeTag(type)
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      align: 'center',
      render: (status: number) => getStatusTag(status)
    },
    {
      title: '创建人',
      dataIndex: 'createByName',
      key: 'createByName',
      width: 100
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 180
    },
    {
      title: '操作',
      key: 'action',
      width: 500,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small" wrap>
          {getActionButtons(record)}
        </Space>
      )
    }
  ]

  const approvalHistoryColumns: ColumnsType<ReportApproval> = [
    {
      title: '版本',
      dataIndex: 'version',
      key: 'version',
      width: 80,
      render: (v: number) => <strong>v{v}</strong>
    },
    {
      title: '审批类型',
      dataIndex: 'approvalType',
      key: 'approvalType',
      width: 100,
      render: (type: number) => type === 1 ? '发布审批' : type === 2 ? '下线审批' : '修改审批'
    },
    {
      title: '状态',
      dataIndex: 'approvalStatus',
      key: 'approvalStatus',
      width: 100,
      render: (status: number) => getApprovalStatusTag(status)
    },
    {
      title: '提交人',
      dataIndex: 'submitByName',
      key: 'submitByName',
      width: 100
    },
    {
      title: '提交时间',
      dataIndex: 'submitTime',
      key: 'submitTime',
      width: 180
    },
    {
      title: '提交备注',
      dataIndex: 'submitRemark',
      key: 'submitRemark'
    },
    {
      title: '审批人',
      dataIndex: 'approveByName',
      key: 'approveByName',
      width: 100
    },
    {
      title: '审批时间',
      dataIndex: 'approveTime',
      key: 'approveTime',
      width: 180
    },
    {
      title: '审批备注',
      dataIndex: 'approveRemark',
      key: 'approveRemark'
    }
  ]

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card size="small">
        <Form form={searchForm} layout="inline">
          <Row gutter={16}>
            <Col>
              <Form.Item name="keyword" label="关键字">
                <Input placeholder="报表名称/编码" allowClear style={{ width: 200 }} />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="status" label="状态">
                <Select
                  placeholder="全部状态"
                  allowClear
                  style={{ width: 130 }}
                  options={[
                    { label: '草稿', value: 0 },
                    { label: '待审批', value: 1 },
                    { label: '已发布', value: 2 },
                    { label: '已下线', value: 3 },
                    { label: '已驳回', value: 4 }
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
        title={
          <Space>
            <span>报表列表</span>
            <Tag color={isConnected ? 'green' : 'default'}>
              {isConnected ? '● 实时同步' : '○ 离线'}
            </Tag>
            {shouldRefresh && (
              <Tag color="orange">
                <SyncOutlined spin /> 有新数据
              </Tag>
            )}
          </Space>
        }
        extra={
          <Space>
            <Button
              icon={<ReloadOutlined spin={shouldRefresh} />}
              onClick={() => {
                acknowledgeRefresh()
                fetchData()
              }}
              type={shouldRefresh ? 'primary' : 'default'}
            >
              {shouldRefresh ? '立即刷新' : '刷新'}
            </Button>
            <Upload
              showUploadList={false}
              beforeUpload={() => {
                message.success('导入成功')
                fetchData()
                return false
              }}
            >
              <Button icon={<CloudUploadOutlined />}>
                导入
              </Button>
            </Upload>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增报表
            </Button>
            <Button danger icon={<DeleteOutlined />} disabled={selectedRowKeys.length === 0} onClick={handleBatchDelete}>
              批量删除
            </Button>
          </Space>
        }
      >
        {shouldRefresh && (
          <Alert
            message="检测到报表数据变更"
            description={
              lastMessage
                ? `变更类型: ${lastMessage.type}${
                    lastMessage.payload?.changeType ? ' (' + lastMessage.payload.changeType + ')' : ''
                  }${
                    lastMessage.payload?.templateName ? ' - ' + lastMessage.payload.templateName : ''
                  }`
                : ''
            }
            type="info"
            showIcon
            closable
            onClose={acknowledgeRefresh}
            style={{ marginBottom: 16 }}
            action={
              <Button size="small" type="primary" onClick={() => { acknowledgeRefresh(); fetchData() }}>
                立即刷新
              </Button>
            }
          />
        )}
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          pagination={false}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys
          }}
          scroll={{ x: 1600 }}
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

      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
        width={600}
        okText="保存"
      >
        <Form form={modalForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="templateName"
                label="报表名称"
                rules={[{ required: true, message: '请输入报表名称' }]}
              >
                <Input placeholder="请输入报表名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="templateCode"
                label="报表编码"
                rules={[{ required: true, message: '请输入报表编码' }]}
              >
                <Input placeholder="请输入报表编码" disabled={!!editingId} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="templateType" label="报表类型" initialValue={1}>
                <Select
                  options={[
                    { label: '表格报表', value: 1 },
                    { label: '图表报表', value: 2 },
                    { label: '混合报表', value: 3 }
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="status" label="状态" initialValue={0}>
                <Select
                  options={[
                    { label: '草稿', value: 0 },
                    { label: '待审批', value: 1 },
                    { label: '已发布', value: 2 }
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入描述" rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="提交审批"
        open={approvalModalVisible}
        onOk={handleApprovalOk}
        onCancel={() => setApprovalModalVisible(false)}
        destroyOnClose
        width={500}
        okText="提交"
      >
        <Descriptions size="small" bordered column={1} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="报表名称">{currentReport?.templateName}</Descriptions.Item>
          <Descriptions.Item label="报表编码">{currentReport?.templateCode}</Descriptions.Item>
          <Descriptions.Item label="当前状态">{getStatusTag(currentReport?.status)}</Descriptions.Item>
        </Descriptions>
        <Form form={approvalForm} layout="vertical">
          <Form.Item name="remark" label="审批说明">
            <Input.TextArea placeholder="请输入审批说明（可选）" rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`审批历史 - ${currentReport?.templateName || ''}`}
        open={approvalHistory.length > 0}
        onCancel={() => {
          setApprovalHistory([])
          setCurrentReport(null)
        }}
        width={1200}
        footer={[
          <Button key="close" onClick={() => {
            setApprovalHistory([])
            setCurrentReport(null)
          }}>
            关闭
          </Button>
        ]}
      >
        <Table
          rowKey="id"
          columns={approvalHistoryColumns}
          dataSource={approvalHistory}
          pagination={false}
          scroll={{ x: 1200 }}
        />
      </Modal>

      <Modal
        title={`预览 - ${previewReport?.templateName || ''}`}
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        width={1100}
        footer={[
          <Button key="back" onClick={() => setPreviewVisible(false)}>
            关闭
          </Button>,
          <Button
            key="excel"
            icon={<CloudDownloadOutlined />}
            onClick={() => previewReport && handleExportExcel(previewReport)}
          >
            导出 Excel
          </Button>,
          <Button
            key="pdf"
            icon={<CloudDownloadOutlined />}
            onClick={() => previewReport && handleExportPdf(previewReport)}
          >
            导出 PDF
          </Button>,
          <Button
            key="design"
            type="primary"
            icon={<DesignOutlined />}
            onClick={() => {
              setPreviewVisible(false)
              previewReport && handleDesign(previewReport)
            }}
          >
            打开设计器
          </Button>
        ]}
      >
        {previewReport && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Descriptions size="small" bordered column={3}>
              <Descriptions.Item label="报表名称">{previewReport.templateName}</Descriptions.Item>
              <Descriptions.Item label="编码">{previewReport.templateCode}</Descriptions.Item>
              <Descriptions.Item label="类型">{getTypeTag(previewReport.templateType)}</Descriptions.Item>
              <Descriptions.Item label="状态">{getStatusTag(previewReport.status)}</Descriptions.Item>
              <Descriptions.Item label="创建人">{previewReport.createByName}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{previewReport.updateTime}</Descriptions.Item>
              <Descriptions.Item label="描述" span={3}>{previewReport.description || '-'}</Descriptions.Item>
            </Descriptions>
            <Card size="small" title="报表内容预览">
              <div style={{ minHeight: 400, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#888' }}>
                <div style={{ textAlign: 'center' }}>
                  <FileTextOutlined style={{ fontSize: 64, marginBottom: 16 }} />
                  <p>报表内容预览区域</p>
                  <p style={{ fontSize: 12 }}>点击「打开设计器」进入报表设计页面</p>
                </div>
              </div>
            </Card>
          </Space>
        )}
      </Modal>

      {currentReport && (
        <VersionManagement
          templateId={currentReport.id!}
          templateName={currentReport.templateName || ''}
          open={versionModalVisible}
          onClose={() => setVersionModalVisible(false)}
          onRollback={fetchData}
        />
      )}
    </Space>
  )
}

export default ReportManagement
