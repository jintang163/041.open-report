import { useState, useEffect } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Select,
  Modal,
  Input,
  Descriptions,
  message,
  Row,
  Col,
  Pagination
} from 'antd'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  EyeOutlined,
  SearchOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ReportApproval } from '@/types'
import {
  getApprovalList,
  approveApproval,
  rejectApproval,
  getReportById,
  getVersionDetail
} from '@/api/report'

const ApprovalManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<ReportApproval[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined)

  const [actionModalVisible, setActionModalVisible] = useState(false)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [currentApproval, setCurrentApproval] = useState<ReportApproval | null>(null)
  const [actionType, setActionType] = useState<'approve' | 'reject'>('approve')
  const [actionRemark, setActionRemark] = useState('')
  const [previewJson, setPreviewJson] = useState<string>('')

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getApprovalList({
        pageNum,
        pageSize,
        status: statusFilter
      })
      setDataSource(res.list || [])
      setTotal(res.total || 0)
    } catch (e) {
      message.error('获取审批列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [pageNum, pageSize, statusFilter])

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

  const getApprovalTypeTag = (type?: number) => {
    const typeMap: Record<number, { color: string; text: string }> = {
      1: { color: 'blue', text: '发布审批' },
      2: { color: 'purple', text: '下线审批' }
    }
    const info = typeMap[type || 1]
    return <Tag color={info.color}>{info.text}</Tag>
  }

  const handleViewDetail = async (record: ReportApproval) => {
    setCurrentApproval(record)
    setPreviewJson('')
    try {
      if (record.templateId) {
        const template = await getReportById(record.templateId)
        if (record.version && template.templateJson) {
          const snapshot = await getVersionDetail(record.templateId, record.version)
          if (snapshot?.templateJson) {
            setPreviewJson(JSON.stringify(JSON.parse(snapshot.templateJson), null, 2))
          }
        }
      }
    } catch {
      setPreviewJson('')
    }
    setDetailModalVisible(true)
  }

  const handleAction = (record: ReportApproval, type: 'approve' | 'reject') => {
    setCurrentApproval(record)
    setActionType(type)
    setActionRemark('')
    setActionModalVisible(true)
  }

  const handleActionConfirm = async () => {
    if (!currentApproval?.id) return
    try {
      if (actionType === 'approve') {
        await approveApproval(currentApproval.id, actionRemark)
        message.success('审批通过')
      } else {
        await rejectApproval(currentApproval.id, actionRemark)
        message.success('审批已驳回')
      }
      setActionModalVisible(false)
      fetchData()
    } catch (e) {
      message.error(actionType === 'approve' ? '审批通过失败' : '审批驳回失败')
    }
  }

  const columns: ColumnsType<ReportApproval> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70
    },
    {
      title: '模板名称',
      dataIndex: 'templateName',
      key: 'templateName',
      width: 180
    },
    {
      title: '版本',
      dataIndex: 'version',
      key: 'version',
      width: 80,
      render: (v: number) => v ? <strong>v{v}</strong> : '-'
    },
    {
      title: '审批类型',
      dataIndex: 'approvalType',
      key: 'approvalType',
      width: 100,
      render: (type: number) => getApprovalTypeTag(type)
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
      key: 'submitRemark',
      width: 200,
      ellipsis: true
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
      title: '操作',
      key: 'action',
      width: 240,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          {record.approvalStatus === 0 && (
            <>
              <Button
                type="link"
                size="small"
                icon={<CheckCircleOutlined />}
                style={{ color: '#52c41a' }}
                onClick={() => handleAction(record, 'approve')}
              >
                通过
              </Button>
              <Button
                type="link"
                size="small"
                danger
                icon={<CloseCircleOutlined />}
                onClick={() => handleAction(record, 'reject')}
              >
                驳回
              </Button>
            </>
          )}
        </Space>
      )
    }
  ]

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card size="small">
        <Row gutter={16} align="middle">
          <Col>
            <span style={{ marginRight: 8 }}>审批状态：</span>
            <Select
              placeholder="全部状态"
              allowClear
              style={{ width: 150 }}
              value={statusFilter}
              onChange={(v) => {
                setStatusFilter(v)
                setPageNum(1)
              }}
              options={[
                { label: '待审批', value: 0 },
                { label: '已通过', value: 1 },
                { label: '已驳回', value: 2 },
                { label: '已撤销', value: 3 }
              ]}
            />
          </Col>
          <Col>
            <Button icon={<ReloadOutlined />} onClick={fetchData}>
              刷新
            </Button>
          </Col>
        </Row>
      </Card>

      <Card size="small" title="审批列表">
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          pagination={false}
          scroll={{ x: 1500 }}
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

      <Modal
        title={actionType === 'approve' ? '审批通过' : '审批驳回'}
        open={actionModalVisible}
        onOk={handleActionConfirm}
        onCancel={() => setActionModalVisible(false)}
        okText={actionType === 'approve' ? '确认通过' : '确认驳回'}
        okButtonProps={{ danger: actionType === 'reject' }}
      >
        <Descriptions size="small" bordered column={1} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="模板名称">{currentApproval?.templateName}</Descriptions.Item>
          <Descriptions.Item label="版本">v{currentApproval?.version}</Descriptions.Item>
          <Descriptions.Item label="审批类型">{getApprovalTypeTag(currentApproval?.approvalType)}</Descriptions.Item>
          <Descriptions.Item label="提交人">{currentApproval?.submitByName}</Descriptions.Item>
          <Descriptions.Item label="提交备注">{currentApproval?.submitRemark || '-'}</Descriptions.Item>
        </Descriptions>
        <div style={{ marginBottom: 8 }}>审批备注：</div>
        <Input.TextArea
          value={actionRemark}
          onChange={(e) => setActionRemark(e.target.value)}
          placeholder="请输入审批备注（可选）"
          rows={3}
        />
      </Modal>

      <Modal
        title={`审批详情 - ${currentApproval?.templateName || ''}`}
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        width={1000}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>
        ]}
      >
        {currentApproval && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Descriptions size="small" bordered column={2}>
              <Descriptions.Item label="模板名称">{currentApproval.templateName}</Descriptions.Item>
              <Descriptions.Item label="版本">v{currentApproval.version}</Descriptions.Item>
              <Descriptions.Item label="审批类型">{getApprovalTypeTag(currentApproval.approvalType)}</Descriptions.Item>
              <Descriptions.Item label="状态">{getApprovalStatusTag(currentApproval.approvalStatus)}</Descriptions.Item>
              <Descriptions.Item label="提交人">{currentApproval.submitByName}</Descriptions.Item>
              <Descriptions.Item label="提交时间">{currentApproval.submitTime}</Descriptions.Item>
              <Descriptions.Item label="提交备注" span={2}>{currentApproval.submitRemark || '-'}</Descriptions.Item>
              <Descriptions.Item label="审批人">{currentApproval.approveByName || '-'}</Descriptions.Item>
              <Descriptions.Item label="审批时间">{currentApproval.approveTime || '-'}</Descriptions.Item>
              <Descriptions.Item label="审批备注" span={2}>{currentApproval.approveRemark || '-'}</Descriptions.Item>
            </Descriptions>
            {previewJson && (
              <Card size="small" title={`版本 v${currentApproval.version} 模板内容`}>
                <pre
                  style={{
                    maxHeight: 400,
                    overflow: 'auto',
                    background: '#f5f5f5',
                    padding: 16,
                    borderRadius: 4,
                    fontSize: 12,
                    fontFamily: 'monospace'
                  }}
                >
                  {previewJson}
                </pre>
              </Card>
            )}
          </Space>
        )}
      </Modal>
    </Space>
  )
}

export default ApprovalManagement
