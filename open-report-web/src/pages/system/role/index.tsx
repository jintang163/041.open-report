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
  Tree
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { SysRole, SysMenu, PageParams } from '@/types'

interface Role extends SysRole {
  id: number
}

const mockRoles: Role[] = [
  { id: 1, name: '超级管理员', code: 'admin', description: '拥有所有权限', status: 1, createTime: '2024-01-01 00:00:00' },
  { id: 2, name: '系统管理员', code: 'system', description: '系统管理权限', status: 1, createTime: '2024-01-02 10:00:00' },
  { id: 3, name: '报表设计师', code: 'designer', description: '设计和编辑报表', status: 1, createTime: '2024-01-03 11:00:00' },
  { id: 4, name: '普通用户', code: 'user', description: '查看报表', status: 1, createTime: '2024-01-04 14:00:00' },
  { id: 5, name: '访客', code: 'guest', description: '仅查看公开报表', status: 0, createTime: '2024-01-05 09:30:00' }
]

const mockMenus: SysMenu[] = [
  {
    id: 1, name: '仪表盘', path: '/dashboard', icon: 'DashboardOutlined', type: 1,
    children: [
      { id: 11, name: '查看仪表盘', path: '/dashboard', perms: 'dashboard:view', type: 2 }
    ]
  },
  {
    id: 2, name: '数据源管理', path: '/datasource', icon: 'DatabaseOutlined', type: 1,
    children: [
      { id: 21, name: '查看', path: '', perms: 'datasource:view', type: 2 },
      { id: 22, name: '新增', path: '', perms: 'datasource:add', type: 2 },
      { id: 23, name: '编辑', path: '', perms: 'datasource:edit', type: 2 },
      { id: 24, name: '删除', path: '', perms: 'datasource:delete', type: 2 }
    ]
  },
  {
    id: 3, name: '数据集管理', path: '/dataset', icon: 'TableOutlined', type: 1,
    children: [
      { id: 31, name: '查看', path: '', perms: 'dataset:view', type: 2 },
      { id: 32, name: '新增', path: '', perms: 'dataset:add', type: 2 },
      { id: 33, name: '编辑', path: '', perms: 'dataset:edit', type: 2 },
      { id: 34, name: '删除', path: '', perms: 'dataset:delete', type: 2 }
    ]
  },
  {
    id: 4, name: '报表管理', path: '/report', icon: 'FileTextOutlined', type: 1,
    children: [
      { id: 41, name: '查看', path: '', perms: 'report:view', type: 2 },
      { id: 42, name: '新增', path: '', perms: 'report:add', type: 2 },
      { id: 43, name: '编辑', path: '', perms: 'report:edit', type: 2 },
      { id: 44, name: '删除', path: '', perms: 'report:delete', type: 2 },
      { id: 45, name: '发布', path: '', perms: 'report:publish', type: 2 }
    ]
  },
  {
    id: 6, name: '函数仓库', path: '/function', icon: 'FunctionOutlined', type: 1,
    children: [
      { id: 61, name: '查询', path: '', perms: 'function:query', type: 2 },
      { id: 62, name: '新增', path: '', perms: 'function:add', type: 2 },
      { id: 63, name: '编辑', path: '', perms: 'function:edit', type: 2 },
      { id: 64, name: '删除', path: '', perms: 'function:remove', type: 2 },
      { id: 65, name: '测试', path: '', perms: 'function:test', type: 2 }
    ]
  },
  {
    id: 5, name: '系统管理', path: '/system', icon: 'SettingOutlined', type: 1,
    children: [
      {
        id: 51, name: '用户管理', path: '/system/user', type: 1,
        children: [
          { id: 511, name: '查看', path: '', perms: 'system:user:view', type: 2 },
          { id: 512, name: '新增', path: '', perms: 'system:user:add', type: 2 },
          { id: 513, name: '编辑', path: '', perms: 'system:user:edit', type: 2 },
          { id: 514, name: '删除', path: '', perms: 'system:user:delete', type: 2 }
        ]
      },
      {
        id: 52, name: '角色管理', path: '/system/role', type: 1,
        children: [
          { id: 521, name: '查看', path: '', perms: 'system:role:view', type: 2 },
          { id: 522, name: '新增', path: '', perms: 'system:role:add', type: 2 },
          { id: 523, name: '编辑', path: '', perms: 'system:role:edit', type: 2 },
          { id: 524, name: '删除', path: '', perms: 'system:role:delete', type: 2 },
          { id: 525, name: '分配权限', path: '', perms: 'system:role:perm', type: 2 }
        ]
      },
      {
        id: 53, name: '菜单管理', path: '/system/menu', type: 1,
        children: [
          { id: 531, name: '查看', path: '', perms: 'system:menu:view', type: 2 },
          { id: 532, name: '新增', path: '', perms: 'system:menu:add', type: 2 },
          { id: 533, name: '编辑', path: '', perms: 'system:menu:edit', type: 2 },
          { id: 534, name: '删除', path: '', perms: 'system:menu:delete', type: 2 }
        ]
      }
    ]
  }
]

const getAllMenuIds = (menus: SysMenu[]): number[] => {
  const ids: number[] = []
  const traverse = (list: SysMenu[]) => {
    list.forEach(item => {
      if (item.id) ids.push(item.id)
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      }
    })
  }
  traverse(menus)
  return ids
}

const RoleManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<Role[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  const [searchForm] = Form.useForm()
  const [modalForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [permModalVisible, setPermModalVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [permRoleId, setPermRoleId] = useState<number | null>(null)
  const [modalTitle, setModalTitle] = useState('新增角色')
  const [checkedKeys, setCheckedKeys] = useState<number[]>([])

  const fetchData = async () => {
    setLoading(true)
    try {
      const values = searchForm.getFieldsValue()
      const filtered = mockRoles.filter(r =>
        !values.keyword ||
        r.name.includes(values.keyword) ||
        (r.code && r.code.includes(values.keyword))
      )
      setDataSource(filtered.slice((pageNum - 1) * pageSize, pageNum * pageSize))
      setTotal(filtered.length)
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
    setModalTitle('新增角色')
    modalForm.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record: Role) => {
    setEditingId(record.id!)
    setModalTitle('编辑角色')
    modalForm.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      if (editingId) {
        setDataSource(prev => prev.map(item => item.id === editingId ? { ...item, ...values } : item))
        message.success('修改成功')
      } else {
        const newRole: Role = { ...values, id: Date.now(), status: values.status ?? 1, createTime: new Date().toLocaleString() }
        setDataSource(prev => [newRole, ...prev])
        setTotal(prev => prev + 1)
        message.success('新增成功')
      }
      setModalVisible(false)
      fetchData()
    } catch {}
  }

  const handleDelete = async (id: number) => {
    try {
      setDataSource(prev => prev.filter(item => item.id !== id))
      setTotal(prev => prev - 1)
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
      setDataSource(prev => prev.filter(item => !selectedRowKeys.includes(item.id as React.Key)))
      setTotal(prev => prev - selectedRowKeys.length)
      message.success(`批量删除成功，共删除 ${selectedRowKeys.length} 条记录`)
      setSelectedRowKeys([])
      fetchData()
    } catch {
      message.error('批量删除失败')
    }
  }

  const handlePerm = (record: Role) => {
    setPermRoleId(record.id!)
    if (record.code === 'admin') {
      setCheckedKeys(getAllMenuIds(mockMenus))
    } else {
      setCheckedKeys([1, 11, 2, 21, 3, 31])
    }
    setPermModalVisible(true)
  }

  const handlePermModalOk = async () => {
    try {
      message.success('权限分配成功')
      setPermModalVisible(false)
    } catch {}
  }

  const columns: ColumnsType<Role> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70
    },
    {
      title: '角色名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
      render: (text: string) => (
        <Space>
          <SafetyCertificateOutlined />
          {text}
        </Space>
      )
    },
    {
      title: '角色编码',
      dataIndex: 'code',
      key: 'code',
      width: 150,
      render: (text: string) => <code>{text}</code>
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      align: 'center',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      )
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
      width: 240,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button type="link" size="small" onClick={() => handlePerm(record)}>
            分配权限
          </Button>
          <Popconfirm title="确定删除该角色?" onConfirm={() => handleDelete(record.id!)} disabled={record.code === 'admin'}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />} disabled={record.code === 'admin'}>
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
                <Input placeholder="角色名称/编码" allowClear style={{ width: 200 }} />
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
        title="角色列表"
        extra={
          <Space>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增角色
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
          scroll={{ x: 950 }}
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
        width={560}
      >
        <Form form={modalForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="角色名称"
                rules={[{ required: true, message: '请输入角色名称' }]}
              >
                <Input placeholder="请输入角色名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="code"
                label="角色编码"
                rules={[{ required: true, message: '请输入角色编码' }]}
              >
                <Input placeholder="请输入角色编码" disabled={!!editingId} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="请输入描述" rows={3} />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select
              options={[
                { label: '启用', value: 1 },
                { label: '禁用', value: 0 }
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="分配权限"
        open={permModalVisible}
        onOk={handlePermModalOk}
        onCancel={() => setPermModalVisible(false)}
        destroyOnClose
        width={600}
        okText="保存"
      >
        <Tree
          checkable
          defaultExpandAll
          treeData={mockMenus.map(m => ({
            title: m.name,
            key: m.id!,
            children: m.children?.map(c => ({
              title: c.name,
              key: c.id!,
              children: c.children?.map(cc => ({
                title: cc.name,
                key: cc.id!
              }))
            }))
          }))}
          checkedKeys={checkedKeys}
          onCheck={(keys) => setCheckedKeys(keys as number[])}
        />
      </Modal>
    </Space>
  )
}

export default RoleManagement
