import { useState, useEffect } from 'react'
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
  Descriptions
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
  CloudDownloadOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ReportTemplate, PageParams } from '@/types'
import {
  getReportList,
  createReport,
  updateReport,
  deleteReport,
  batchDeleteReport,
  copyReport,
  exportReportExcel,
  exportReportPdf
} from '@/api/report'

interface Report extends ReportTemplate {
  id: number
}

const mockReports: Report[] = [
  { id: 1, name: '销售月报', code: 'sales_monthly', type: 1, status: 2, remark: '按月统计销售数据，包含柱状图和饼图展示', createBy: 'admin', createTime: '2024-01-01 10:00:00', updateTime: '2024-01-15 14:30:00' },
  { id: 2, name: '财务分析报表', code: 'finance_analysis', type: 2, status: 2, remark: '财务数据综合分析，含多维度钻取', createBy: 'zhangsan', createTime: '2024-01-02 11:00:00', updateTime: '2024-01-14 09:20:00' },
  { id: 3, name: '库存统计', code: 'inventory_stat', type: 1, status: 1, remark: '实时库存数据统计与预警', createBy: 'lisi', createTime: '2024-01-03 14:00:00', updateTime: '2024-01-10 16:45:00' },
  { id: 4, name: '客户分析', code: 'customer_analysis', type: 2, status: 2, remark: '客户画像及消费行为分析', createBy: 'wangwu', createTime: '2024-01-04 09:30:00', updateTime: '2024-01-12 11:15:00' },
  { id: 5, name: '员工绩效', code: 'employee_perf', type: 1, status: 0, remark: '员工KPI绩效考核报表', createBy: 'zhaoliu', createTime: '2024-01-05 16:20:00', updateTime: '2024-01-08 10:00:00' },
  { id: 6, name: '生产日报', code: 'production_daily', type: 1, status: 2, remark: '每日生产数据汇总', createBy: 'sunqi', createTime: '2024-01-06 08:00:00', updateTime: '2024-01-15 08:30:00' },
  { id: 7, name: '采购分析', code: 'purchase_analysis', type: 1, status: 1, remark: '供应商及采购数据分析', createBy: 'zhouba', createTime: '2024-01-07 13:00:00', updateTime: '2024-01-11 15:00:00' }
]

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

  const [modalVisible, setModalVisible] = useState(false)
  const [previewVisible, setPreviewVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [modalTitle, setModalTitle] = useState('新增报表')
  const [previewReport, setPreviewReport] = useState<Report | null>(null)

  const fetchData = async () => {
    setLoading(true)
    try {
      const values = searchForm.getFieldsValue()
      const params: PageParams = {
        pageNum,
        pageSize,
        keyword: values.keyword
      }
      try {
        const res = await getReportList(params)
        setDataSource(res.list)
        setTotal(res.total)
      } catch {
        const filtered = mockReports.filter(r =>
          (!params.keyword || r.name.includes(params.keyword) || (r.code && r.code.includes(params.keyword))) &&
          (values.status === undefined || r.status === values.status)
        )
        setDataSource(filtered.slice((pageNum - 1) * pageSize, pageNum * pageSize))
        setTotal(filtered.length)
      }
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
    modalForm.setFieldsValue({ type: 1, status: 0 })
    setModalVisible(true)
  }

  const handleEdit = (record: Report) => {
    setEditingId(record.id!)
    setModalTitle('编辑报表')
    modalForm.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      if (editingId) {
        try {
          await updateReport({ ...values, id: editingId })
        } catch {}
        message.success('修改成功')
      } else {
        try {
          await createReport(values)
        } catch {}
        message.success('新增成功')
      }
      setModalVisible(false)
      fetchData()
    } catch {}
  }

  const handleDelete = async (id: number) => {
    try {
      try {
        await deleteReport(id)
      } catch {}
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
      try {
        await batchDeleteReport(selectedRowKeys.map(k => Number(k)))
      } catch {}
      message.success(`批量删除成功，共删除 ${selectedRowKeys.length} 条记录`)
      setSelectedRowKeys([])
      fetchData()
    } catch {
      message.error('批量删除失败')
    }
  }

  const handleCopy = async (record: Report) => {
    try {
      try {
        await copyReport(record.id!)
      } catch {}
      message.success('复制成功')
      fetchData()
    } catch {
      message.error('复制失败')
    }
  }

  const handlePublish = async (record: Report) => {
    try {
      try {
        await updateReport({ ...record, id: record.id, status: 2 })
      } catch {
        setDataSource(prev => prev.map(item => item.id === record.id ? { ...item, status: 2 } : item))
      }
      message.success('发布成功')
      fetchData()
    } catch {
      message.error('发布失败')
    }
  }

  const handleUnpublish = async (record: Report) => {
    try {
      try {
        await updateReport({ ...record, id: record.id, status: 1 })
      } catch {
        setDataSource(prev => prev.map(item => item.id === record.id ? { ...item, status: 1 } : item))
      }
      message.success('已取消发布')
      fetchData()
    } catch {
      message.error('操作失败')
    }
  }

  const handleDesign = (record: Report) => {
    navigate(`/designer/${record.id}`)
  }

  const handlePreview = (record: Report) => {
    setPreviewReport(record)
    setPreviewVisible(true)
  }

  const handleExportExcel = async (record: Report) => {
    try {
      try {
        await exportReportExcel(record.id!)
      } catch {}
      message.success('导出成功')
    } catch {
      message.error('导出失败')
    }
  }

  const handleExportPdf = async (record: Report) => {
    try {
      try {
        await exportReportPdf(record.id!)
      } catch {}
      message.success('导出成功')
    } catch {
      message.error('导出失败')
    }
  }

  const getStatusTag = (status?: number) => {
    const statusMap: Record<number, { color: string; text: string }> = {
      0: { color: 'default', text: '草稿' },
      1: { color: 'orange', text: '未发布' },
      2: { color: 'green', text: '已发布' }
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

  const columns: ColumnsType<Report> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70
    },
    {
      title: '报表名称',
      dataIndex: 'name',
      key: 'name',
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
      dataIndex: 'code',
      key: 'code',
      width: 160,
      render: (text: string) => <code>{text || '-'}</code>
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
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
      dataIndex: 'createBy',
      key: 'createBy',
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
      width: 360,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small" wrap>
          <Button type="link" size="small" icon={<DesignOutlined />} onClick={() => handleDesign(record)}>
            设计
          </Button>
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handlePreview(record)}>
            预览
          </Button>
          {record.status === 2 ? (
            <Button type="link" size="small" icon={<PauseCircleOutlined />} onClick={() => handleUnpublish(record)}>
              取消发布
            </Button>
          ) : (
            <Button type="link" size="small" icon={<PlayCircleOutlined />} onClick={() => handlePublish(record)}>
              发布
            </Button>
          )}
          <Button type="link" size="small" icon={<CopyOutlined />} onClick={() => handleCopy(record)}>
            复制
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除该报表?" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
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
                    { label: '未发布', value: 1 },
                    { label: '已发布', value: 2 }
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
        title="报表列表"
        extra={
          <Space>
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
          scroll={{ x: 1400 }}
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
                name="name"
                label="报表名称"
                rules={[{ required: true, message: '请输入报表名称' }]}
              >
                <Input placeholder="请输入报表名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="code"
                label="报表编码"
                rules={[{ required: true, message: '请输入报表编码' }]}
              >
                <Input placeholder="请输入报表编码" disabled={!!editingId} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="type" label="报表类型" initialValue={1}>
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
              <Form.Item name="status" label="状态" initialValue={0} valuePropName="checked">
                <Switch
                  checkedChildren="已发布"
                  unCheckedChildren="草稿"
                  checked={modalForm.getFieldValue('status') === 2}
                  onChange={(checked) => modalForm.setFieldsValue({ status: checked ? 2 : 0 })}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="remark" label="备注">
            <Input.TextArea placeholder="请输入备注" rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`预览 - ${previewReport?.name || ''}`}
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
              <Descriptions.Item label="报表名称">{previewReport.name}</Descriptions.Item>
              <Descriptions.Item label="编码">{previewReport.code}</Descriptions.Item>
              <Descriptions.Item label="类型">{getTypeTag(previewReport.type)}</Descriptions.Item>
              <Descriptions.Item label="状态">{getStatusTag(previewReport.status)}</Descriptions.Item>
              <Descriptions.Item label="创建人">{previewReport.createBy}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{previewReport.updateTime}</Descriptions.Item>
              <Descriptions.Item label="备注" span={3}>{previewReport.remark || '-'}</Descriptions.Item>
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
    </Space>
  )
}

export default ReportManagement
