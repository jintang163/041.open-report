import { useState, useEffect } from 'react'
import {
  Table, Button, Space, Form, Input, Modal, message,
  Popconfirm, Card, Tag, Pagination, Select
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { SysTenant } from '@/types'
import { getTenantPage, createTenant, updateTenant, deleteTenant } from '@/api/tenant'

const TenantManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<SysTenant[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [searchName, setSearchName] = useState('')

  const [modalVisible, setModalVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [modalTitle, setModalTitle] = useState('新增租户')
  const [form] = Form.useForm()

  const fetchData = async () => {
    setLoading(true)
    try {
      const res = await getTenantPage({ pageNum, pageSize, tenantName: searchName || undefined } as any)
      setDataSource(res.list || [])
      setTotal(res.total || 0)
    } catch {
      setDataSource([])
      setTotal(0)
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

  const handleAdd = () => {
    setEditingId(null)
    setModalTitle('新增租户')
    form.resetFields()
    form.setFieldsValue({ status: 1 })
    setModalVisible(true)
  }

  const handleEdit = (record: SysTenant) => {
    setEditingId(record.id!)
    setModalTitle('编辑租户')
    form.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      if (editingId) {
        await updateTenant({ ...values, id: editingId })
        message.success('更新成功')
      } else {
        await createTenant(values)
        message.success('创建成功')
      }
      setModalVisible(false)
      fetchData()
    } catch (e: any) {
      if (e.errorFields) return
      message.error('操作失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteTenant(id)
      message.success('删除成功')
      fetchData()
    } catch {
      message.error('删除失败')
    }
  }

  const columns: ColumnsType<SysTenant> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '租户名称', dataIndex: 'tenantName', key: 'tenantName' },
    { title: '租户编码', dataIndex: 'tenantCode', key: 'tenantCode' },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 100,
      render: (v: number) => v === 1 ? <Tag color="success">启用</Tag> : <Tag color="default">禁用</Tag>
    },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
    {
      title: '操作', key: 'action', width: 160,
      render: (_: any, record: SysTenant) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除该租户？" onConfirm={() => handleDelete(record.id!)} okText="确定" cancelText="取消">
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <Input
              placeholder="搜索租户名称"
              value={searchName}
              onChange={e => setSearchName(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 240 }}
              prefix={<SearchOutlined />}
            />
            <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>搜索</Button>
          </Space>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={fetchData}>刷新</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增租户</Button>
          </Space>
        </div>

        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          pagination={false}
          size="middle"
        />

        <div style={{ marginTop: 16, textAlign: 'right' }}>
          <Pagination
            current={pageNum}
            pageSize={pageSize}
            total={total}
            showSizeChanger
            showTotal={t => `共 ${t} 条`}
            onChange={(p, ps) => { setPageNum(p); setPageSize(ps) }}
          />
        </div>
      </Card>

      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleSave}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="tenantName" label="租户名称" rules={[{ required: true, message: '请输入租户名称' }]}>
            <Input placeholder="请输入租户名称" />
          </Form.Item>
          <Form.Item name="tenantCode" label="租户编码" rules={[{ required: true, message: '请输入租户编码' }]}>
            <Input placeholder="请输入租户编码，如 TENANT_A" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入描述" rows={3} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select>
              <Select.Option value={1}>启用</Select.Option>
              <Select.Option value={0}>禁用</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default TenantManagement
