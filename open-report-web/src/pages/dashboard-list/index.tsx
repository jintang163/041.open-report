import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Table, Button, Space, Input, Card, Modal, Form, Tag, message, Popconfirm, InputNumber } from 'antd'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined,
  DashboardOutlined, SearchOutlined
} from '@ant-design/icons'
import { getDashboardPage, createDashboard, deleteDashboard } from '@/api/dashboard'
import type { ChartDashboard, PageParams } from '@/types'

const DashboardManagement: React.FC = () => {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<ChartDashboard[]>([])
  const [total, setTotal] = useState(0)
  const [pageParams, setPageParams] = useState<PageParams>({ pageNum: 1, pageSize: 10, keyword: '' })
  const [createModalVisible, setCreateModalVisible] = useState(false)
  const [form] = Form.useForm()

  const loadData = async () => {
    setLoading(true)
    try {
      const res = await getDashboardPage(pageParams)
      setData(res.list || [])
      setTotal(res.total || 0)
    } catch {
      message.error('加载大屏列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [pageParams])

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      const dashboard = await createDashboard({
        name: values.name,
        code: values.code || `DASHBOARD_${Date.now()}`,
        description: values.description,
        canvasWidth: values.canvasWidth || 1920,
        canvasHeight: values.canvasHeight || 1080,
        backgroundColor: values.backgroundColor || '#0d1b2a',
        refreshInterval: values.refreshInterval || 0,
        status: 1
      })
      setCreateModalVisible(false)
      form.resetFields()
      message.success('创建成功')
      navigate(`/dashboard/designer/${dashboard.id}`)
    } catch {}
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteDashboard(id)
      message.success('删除成功')
      loadData()
    } catch {
      message.error('删除失败')
    }
  }

  const columns = [
    {
      title: '大屏名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string, record: ChartDashboard) => (
        <Button type="link" size="small" onClick={() => navigate(`/dashboard/designer/${record.id}`)}>
          {text}
        </Button>
      )
    },
    {
      title: '编码',
      dataIndex: 'code',
      key: 'code',
      render: (text: string) => <Tag>{text}</Tag>
    },
    {
      title: '画布尺寸',
      key: 'canvas',
      render: (_: any, record: ChartDashboard) => `${record.canvasWidth || 1920} x ${record.canvasHeight || 1080}`
    },
    {
      title: '刷新间隔',
      dataIndex: 'refreshInterval',
      key: 'refreshInterval',
      render: (v: number) => v && v > 0 ? `${v}秒` : '-'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>{status === 1 ? '启用' : '禁用'}</Tag>
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime'
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      render: (_: any, record: ChartDashboard) => (
        <Space size={4}>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => navigate(`/dashboard/designer/${record.id}`)}
          >
            设计
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/dashboard/viewer/${record.id}`)}
          >
            预览
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id!)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  return (
    <div>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
          <Space>
            <Input.Search
              placeholder="搜索大屏名称"
              allowClear
              style={{ width: 260 }}
              onSearch={keyword => setPageParams({ ...pageParams, keyword, pageNum: 1 })}
            />
          </Space>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setCreateModalVisible(true)}
          >
            新建大屏
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          pagination={{
            current: pageParams.pageNum,
            pageSize: pageParams.pageSize,
            total,
            showSizeChanger: true,
            showTotal: t => `共 ${t} 条`,
            onChange: (pageNum, pageSize) => setPageParams({ ...pageParams, pageNum, pageSize })
          }}
        />
      </Card>

      <Modal
        title="新建可视化大屏"
        open={createModalVisible}
        onOk={handleCreate}
        onCancel={() => { setCreateModalVisible(false); form.resetFields() }}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item label="大屏名称" name="name" rules={[{ required: true, message: '请输入大屏名称' }]}>
            <Input placeholder="请输入大屏名称" />
          </Form.Item>
          <Form.Item label="大屏编码" name="code">
            <Input placeholder="不填则自动生成" />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={2} placeholder="可选" />
          </Form.Item>
          <Space>
            <Form.Item label="画布宽度" name="canvasWidth" initialValue={1920}>
              <InputNumber min={800} max={3840} step={10} />
            </Form.Item>
            <Form.Item label="画布高度" name="canvasHeight" initialValue={1080}>
              <InputNumber min={600} max={2160} step={10} />
            </Form.Item>
          </Space>
          <Form.Item label="背景色" name="backgroundColor" initialValue="#0d1b2a">
            <Input placeholder="#0d1b2a" />
          </Form.Item>
          <Form.Item label="自动刷新间隔(秒)" name="refreshInterval" initialValue={0} extra="0表示不自动刷新">
            <InputNumber min={0} max={3600} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default DashboardManagement
