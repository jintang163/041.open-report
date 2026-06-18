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
  Pagination
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  LockOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { FieldPermissionRule, SysRole, PageParams } from '@/types'
import {
  getFieldPermissionList,
  createFieldPermission,
  updateFieldPermission,
  deleteFieldPermission,
  getRoleAll
} from '@/api/security'

interface FieldPermission extends FieldPermissionRule {
  roleName?: string
}

const mockRoles: SysRole[] = [
  { id: 1, name: '超级管理员', code: 'admin', description: '拥有所有权限', status: 1, createTime: '2024-01-01 00:00:00' },
  { id: 2, name: '报表设计师', code: 'designer', description: '设计和编辑报表', status: 1, createTime: '2024-01-02 10:00:00' },
  { id: 3, name: '普通用户', code: 'user', description: '查看报表', status: 1, createTime: '2024-01-03 11:00:00' },
  { id: 4, name: '访客', code: 'guest', description: '仅查看公开报表', status: 0, createTime: '2024-01-04 14:00:00' }
]

const mockData: FieldPermission[] = [
  { id: 1, roleId: 3, roleName: '普通用户', tableName: '*', fieldName: 'cost_price', permissionType: 'HIDDEN', description: '普通用户隐藏成本价', status: 1 },
  { id: 2, roleId: 3, roleName: '普通用户', tableName: '*', fieldName: 'phone', permissionType: 'MASKED', description: '普通用户手机号脱敏', status: 1 },
  { id: 3, roleId: 3, roleName: '普通用户', tableName: '*', fieldName: 'profit', permissionType: 'HIDDEN', description: '普通用户隐藏利润', status: 1 }
]

const FieldPermissionManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<FieldPermission[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [roleList, setRoleList] = useState<SysRole[]>([])

  const [searchForm] = Form.useForm()
  const [modalForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [modalTitle, setModalTitle] = useState('新增字段权限规则')

  const fetchRoles = async () => {
    try {
      const res = await getRoleAll()
      setRoleList(res)
    } catch {
      setRoleList(mockRoles)
    }
  }

  const fetchData = async () => {
    setLoading(true)
    try {
      const values = searchForm.getFieldsValue()
      const params: PageParams & { roleId?: number } = {
        pageNum,
        pageSize,
        keyword: values.keyword,
        roleId: values.roleId
      }
      try {
        const res = await getFieldPermissionList(params)
        setDataSource(res.list || res)
        setTotal(res.total || (res.list || res).length)
      } catch {
        let filtered = [...mockData]
        if (params.roleId) {
          filtered = filtered.filter(r => r.roleId === params.roleId)
        }
        if (params.keyword) {
          filtered = filtered.filter(r =>
            r.tableName.includes(params.keyword!) ||
            r.fieldName.includes(params.keyword!) ||
            (r.roleName && r.roleName.includes(params.keyword!))
          )
        }
        setDataSource(filtered.slice((pageNum - 1) * pageSize, pageNum * pageSize))
        setTotal(filtered.length)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRoles()
  }, [])

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
    setModalTitle('新增字段权限规则')
    modalForm.resetFields()
    modalForm.setFieldsValue({ tableName: '*', status: 1 })
    setModalVisible(true)
  }

  const handleEdit = (record: FieldPermission) => {
    setEditingId(record.id!)
    setModalTitle('编辑字段权限规则')
    modalForm.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      if (editingId) {
        try {
          await updateFieldPermission({ ...values, id: editingId })
        } catch {
          setDataSource(prev => prev.map(item => item.id === editingId ? { ...item, ...values } : item))
        }
        message.success('修改成功')
      } else {
        try {
          await createFieldPermission(values)
        } catch {
          const roleName = roleList.find(r => r.id === values.roleId)?.name || ''
          const newRule: FieldPermission = {
            ...values,
            id: Date.now(),
            roleName,
            status: values.status ?? 1,
            createTime: new Date().toLocaleString()
          }
          setDataSource(prev => [newRule, ...prev])
          setTotal(prev => prev + 1)
        }
        message.success('新增成功')
      }
      setModalVisible(false)
      fetchData()
    } catch {}
  }

  const handleDelete = async (id: number) => {
    try {
      try {
        await deleteFieldPermission(id)
      } catch {
        setDataSource(prev => prev.filter(item => item.id !== id))
        setTotal(prev => prev - 1)
      }
      message.success('删除成功')
      fetchData()
    } catch {
      message.error('删除失败')
    }
  }

  const columns: ColumnsType<FieldPermission> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70
    },
    {
      title: '角色ID',
      dataIndex: 'roleId',
      key: 'roleId',
      width: 80,
      align: 'center'
    },
    {
      title: '角色名称',
      dataIndex: 'roleName',
      key: 'roleName',
      width: 130,
      render: (text: string) => (
        <Space>
          <LockOutlined />
          {text}
        </Space>
      )
    },
    {
      title: '表名',
      dataIndex: 'tableName',
      key: 'tableName',
      width: 120,
      render: (text: string) => <code>{text}</code>
    },
    {
      title: '字段名',
      dataIndex: 'fieldName',
      key: 'fieldName',
      width: 130,
      render: (text: string) => <code style={{ color: '#1890ff' }}>{text}</code>
    },
    {
      title: '权限类型',
      dataIndex: 'permissionType',
      key: 'permissionType',
      width: 100,
      align: 'center',
      render: (type: 'HIDDEN' | 'MASKED') => (
        <Tag color={type === 'HIDDEN' ? 'red' : 'orange'}>
          {type === 'HIDDEN' ? '隐藏' : '脱敏'}
        </Tag>
      )
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
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除该规则?" onConfirm={() => handleDelete(record.id!)}>
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
              <Form.Item name="roleId" label="角色">
                <Select
                  placeholder="全部角色"
                  allowClear
                  style={{ width: 180 }}
                  options={roleList.map(r => ({ label: r.name, value: r.id }))}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="keyword" label="关键字">
                <Input placeholder="表名/字段名" allowClear style={{ width: 200 }} />
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
        title="字段权限规则"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            新增规则
          </Button>
        }
      >
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          pagination={false}
          scroll={{ x: 1100 }}
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
          <Form.Item
            name="roleId"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select
              placeholder="请选择角色"
              showSearch
              optionFilterProp="label"
              options={roleList.map(r => ({ label: r.name, value: r.id }))}
            />
          </Form.Item>
          <Form.Item
            name="tableName"
            label="表名"
            rules={[{ required: true, message: '请输入表名' }]}
          >
            <Input placeholder="请输入表名，* 表示所有表" />
          </Form.Item>
          <Form.Item
            name="fieldName"
            label="字段名"
            rules={[{ required: true, message: '请输入字段名' }]}
          >
            <Input placeholder="请输入字段名" />
          </Form.Item>
          <Form.Item
            name="permissionType"
            label="权限类型"
            rules={[{ required: true, message: '请选择权限类型' }]}
          >
            <Select
              placeholder="请选择权限类型"
              options={[
                { label: '隐藏', value: 'HIDDEN' },
                { label: '脱敏', value: 'MASKED' }
              ]}
            />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input placeholder="请输入描述" />
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
    </Space>
  )
}

export default FieldPermissionManagement
