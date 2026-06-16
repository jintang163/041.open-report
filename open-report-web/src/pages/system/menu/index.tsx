import { useState, useEffect } from 'react'
import {
  Table,
  Button,
  Space,
  Form,
  Input,
  Select,
  InputNumber,
  Modal,
  message,
  Popconfirm,
  Card,
  Row,
  Col,
  Tag,
  TreeSelect
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  MenuOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { SysMenu } from '@/types'

interface Menu extends SysMenu {
  id: number
  children?: Menu[]
}

const mockMenus: Menu[] = [
  { id: 1, name: '仪表盘', path: '/dashboard', icon: 'DashboardOutlined', component: '/dashboard/index', parentId: 0, sort: 1, type: 1, perms: '',
    children: [
      { id: 11, name: '查看仪表盘', path: '', icon: '', component: '', parentId: 1, sort: 1, type: 2, perms: 'dashboard:view' }
    ]
  },
  { id: 2, name: '数据源管理', path: '/datasource', icon: 'DatabaseOutlined', component: '/datasource/index', parentId: 0, sort: 2, type: 1, perms: '',
    children: [
      { id: 21, name: '查看', path: '', icon: '', component: '', parentId: 2, sort: 1, type: 2, perms: 'datasource:view' },
      { id: 22, name: '新增', path: '', icon: '', component: '', parentId: 2, sort: 2, type: 2, perms: 'datasource:add' },
      { id: 23, name: '编辑', path: '', icon: '', component: '', parentId: 2, sort: 3, type: 2, perms: 'datasource:edit' },
      { id: 24, name: '删除', path: '', icon: '', component: '', parentId: 2, sort: 4, type: 2, perms: 'datasource:delete' }
    ]
  },
  { id: 3, name: '数据集管理', path: '/dataset', icon: 'TableOutlined', component: '/dataset/index', parentId: 0, sort: 3, type: 1, perms: '',
    children: [
      { id: 31, name: '查看', path: '', icon: '', component: '', parentId: 3, sort: 1, type: 2, perms: 'dataset:view' },
      { id: 32, name: '新增', path: '', icon: '', component: '', parentId: 3, sort: 2, type: 2, perms: 'dataset:add' },
      { id: 33, name: '编辑', path: '', icon: '', component: '', parentId: 3, sort: 3, type: 2, perms: 'dataset:edit' },
      { id: 34, name: '删除', path: '', icon: '', component: '', parentId: 3, sort: 4, type: 2, perms: 'dataset:delete' }
    ]
  },
  { id: 4, name: '报表管理', path: '/report', icon: 'FileTextOutlined', component: '/report/index', parentId: 0, sort: 4, type: 1, perms: '',
    children: [
      { id: 41, name: '查看', path: '', icon: '', component: '', parentId: 4, sort: 1, type: 2, perms: 'report:view' },
      { id: 42, name: '新增', path: '', icon: '', component: '', parentId: 4, sort: 2, type: 2, perms: 'report:add' },
      { id: 43, name: '编辑', path: '', icon: '', component: '', parentId: 4, sort: 3, type: 2, perms: 'report:edit' },
      { id: 44, name: '删除', path: '', icon: '', component: '', parentId: 4, sort: 4, type: 2, perms: 'report:delete' },
      { id: 45, name: '发布', path: '', icon: '', component: '', parentId: 4, sort: 5, type: 2, perms: 'report:publish' }
    ]
  },
  { id: 5, name: '系统管理', path: '/system', icon: 'SettingOutlined', component: '', parentId: 0, sort: 5, type: 1, perms: '',
    children: [
      { id: 51, name: '用户管理', path: '/system/user', icon: 'UserOutlined', component: '/system/user/index', parentId: 5, sort: 1, type: 1, perms: '',
        children: [
          { id: 511, name: '查看', path: '', icon: '', component: '', parentId: 51, sort: 1, type: 2, perms: 'system:user:view' },
          { id: 512, name: '新增', path: '', icon: '', component: '', parentId: 51, sort: 2, type: 2, perms: 'system:user:add' },
          { id: 513, name: '编辑', path: '', icon: '', component: '', parentId: 51, sort: 3, type: 2, perms: 'system:user:edit' },
          { id: 514, name: '删除', path: '', icon: '', component: '', parentId: 51, sort: 4, type: 2, perms: 'system:user:delete' }
        ]
      },
      { id: 52, name: '角色管理', path: '/system/role', icon: 'TeamOutlined', component: '/system/role/index', parentId: 5, sort: 2, type: 1, perms: '',
        children: [
          { id: 521, name: '查看', path: '', icon: '', component: '', parentId: 52, sort: 1, type: 2, perms: 'system:role:view' },
          { id: 522, name: '新增', path: '', icon: '', component: '', parentId: 52, sort: 2, type: 2, perms: 'system:role:add' },
          { id: 523, name: '编辑', path: '', icon: '', component: '', parentId: 52, sort: 3, type: 2, perms: 'system:role:edit' },
          { id: 524, name: '删除', path: '', icon: '', component: '', parentId: 52, sort: 4, type: 2, perms: 'system:role:delete' },
          { id: 525, name: '分配权限', path: '', icon: '', component: '', parentId: 52, sort: 5, type: 2, perms: 'system:role:perm' }
        ]
      },
      { id: 53, name: '菜单管理', path: '/system/menu', icon: 'MenuOutlined', component: '/system/menu/index', parentId: 5, sort: 3, type: 1, perms: '',
        children: [
          { id: 531, name: '查看', path: '', icon: '', component: '', parentId: 53, sort: 1, type: 2, perms: 'system:menu:view' },
          { id: 532, name: '新增', path: '', icon: '', component: '', parentId: 53, sort: 2, type: 2, perms: 'system:menu:add' },
          { id: 533, name: '编辑', path: '', icon: '', component: '', parentId: 53, sort: 3, type: 2, perms: 'system:menu:edit' },
          { id: 534, name: '删除', path: '', icon: '', component: '', parentId: 53, sort: 4, type: 2, perms: 'system:menu:delete' }
        ]
      }
    ]
  }
]

const flattenMenus = (menus: Menu[]): { id: number; name: string; parentId: number }[] => {
  const result: { id: number; name: string; parentId: number }[] = [{ id: 0, name: '顶级菜单', parentId: -1 }]
  const traverse = (list: Menu[]) => {
    list.forEach(item => {
      result.push({ id: item.id, name: item.name, parentId: item.parentId || 0 })
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      }
    })
  }
  traverse(menus)
  return result
}

const MenuManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<Menu[]>([])
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([])

  const [searchForm] = Form.useForm()
  const [modalForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [modalTitle, setModalTitle] = useState('新增菜单')
  const [menuType, setMenuType] = useState<number>(1)

  const fetchData = async () => {
    setLoading(true)
    try {
      const values = searchForm.getFieldsValue()
      if (values.keyword) {
        const filtered: Menu[] = []
        const filter = (menus: Menu[]): boolean => {
          let hasMatch = false
          menus.forEach(menu => {
            const childrenMatch = menu.children ? filter(menu.children) : false
            const selfMatch = menu.name.includes(values.keyword)
            if (selfMatch || childrenMatch) {
              hasMatch = true
              filtered.push({ ...menu, children: childrenMatch ? menu.children : undefined })
            }
          })
          return hasMatch
        }
        filter(mockMenus)
        setDataSource(filtered)
      } else {
        setDataSource(mockMenus)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
    const allKeys: React.Key[] = []
    const traverse = (menus: Menu[]) => {
      menus.forEach(m => {
        allKeys.push(m.id)
        if (m.children) traverse(m.children)
      })
    }
    traverse(mockMenus)
    setExpandedRowKeys(allKeys)
  }, [])

  const handleSearch = () => {
    fetchData()
  }

  const handleReset = () => {
    searchForm.resetFields()
    fetchData()
  }

  const handleAdd = (parentId?: number) => {
    setEditingId(null)
    setModalTitle('新增菜单')
    setMenuType(1)
    modalForm.resetFields()
    modalForm.setFieldsValue({ parentId: parentId || 0, type: 1, sort: 1 })
    setModalVisible(true)
  }

  const handleAddChild = (record: Menu) => {
    if (record.type === 2) {
      message.warning('按钮类型不能添加子菜单')
      return
    }
    setEditingId(null)
    setModalTitle(`新增「${record.name}」的子菜单`)
    setMenuType(1)
    modalForm.resetFields()
    modalForm.setFieldsValue({ parentId: record.id, type: 1, sort: 1 })
    setModalVisible(true)
  }

  const handleEdit = (record: Menu) => {
    setEditingId(record.id)
    setModalTitle('编辑菜单')
    setMenuType(record.type || 1)
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

  const flatMenuList = flattenMenus(mockMenus)
  const treeData = flatMenuList.map(m => ({ title: m.name, value: m.id, key: m.id }))

  const columns: ColumnsType<Menu> = [
    {
      title: '菜单名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      render: (text: string, record) => (
        <Space>
          <MenuOutlined />
          {text}
          {record.type === 2 && <Tag color="blue">按钮</Tag>}
          {record.type === 1 && <Tag color="green">菜单</Tag>}
        </Space>
      )
    },
    {
      title: '图标',
      dataIndex: 'icon',
      key: 'icon',
      width: 120,
      render: (text: string) => text || '-'
    },
    {
      title: '路径',
      dataIndex: 'path',
      key: 'path',
      width: 200,
      render: (text: string) => <code>{text || '-'}</code>
    },
    {
      title: '组件',
      dataIndex: 'component',
      key: 'component',
      width: 220,
      render: (text: string) => <code>{text || '-'}</code>
    },
    {
      title: '权限标识',
      dataIndex: 'perms',
      key: 'perms',
      width: 180,
      render: (text: string) => <Tag color="purple">{text || '-'}</Tag>
    },
    {
      title: '排序',
      dataIndex: 'sort',
      key: 'sort',
      width: 80,
      align: 'center'
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          {record.type !== 2 && (
            <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => handleAddChild(record)}>
              新增
            </Button>
          )}
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除该菜单?删除后子菜单也会被删除" onConfirm={() => handleDelete(record.id)}>
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
              <Form.Item name="keyword" label="菜单名称">
                <Input placeholder="请输入菜单名称" allowClear style={{ width: 200 }} />
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
        title="菜单列表"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAdd()}>
            新增菜单
          </Button>
        }
      >
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          pagination={false}
          expandable={{
            expandedRowKeys,
            onExpandedRowsChange: (keys) => setExpandedRowKeys(keys)
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
        width={640}
      >
        <Form form={modalForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="parentId" label="上级菜单" initialValue={0}>
                <TreeSelect
                  treeData={treeData}
                  treeDefaultExpandAll
                  placeholder="请选择上级菜单"
                  allowClear
                  disabled={!!editingId}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="type"
                label="菜单类型"
                rules={[{ required: true, message: '请选择菜单类型' }]}
                initialValue={1}
              >
                <Select
                  onChange={(v) => setMenuType(v)}
                  options={[
                    { label: '目录/菜单', value: 1 },
                    { label: '按钮', value: 2 }
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="菜单名称"
                rules={[{ required: true, message: '请输入菜单名称' }]}
              >
                <Input placeholder="请输入菜单名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="sort" label="排序" initialValue={1}>
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          {menuType === 1 && (
            <>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="path" label="路由路径">
                    <Input placeholder="如：/system/user" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="component" label="组件路径">
                    <Input placeholder="如：/system/user/index" />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="icon" label="图标">
                    <Input placeholder="图标名称，如：UserOutlined" />
                  </Form.Item>
                </Col>
              </Row>
            </>
          )}
          <Form.Item name="perms" label="权限标识">
            <Input placeholder="权限标识，如：system:user:add" />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}

export default MenuManagement
