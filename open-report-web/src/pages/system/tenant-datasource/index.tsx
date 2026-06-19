import { useState, useEffect } from 'react'
import {
  Table, Button, Space, Form, Select, Modal, message,
  Popconfirm, Card, Tag, Empty, Spin, Alert
} from 'antd'
import { PlusOutlined, DeleteOutlined, ReloadOutlined, ApiOutlined, SwapOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { SysTenant, TenantDatasourceMappingVO, DataSourceConfig } from '@/types'
import {
  getTenantList,
  getTenantDatasourceMappings,
  saveTenantDatasourceMapping,
  deleteTenantDatasourceMapping,
  testTenantDatasourceConnection
} from '@/api/tenant'
import { getDatasourceAll } from '@/api/datasource'

const TenantDatasourceMappingPage = () => {
  const [loading, setLoading] = useState(false)
  const [tenants, setTenants] = useState<SysTenant[]>([])
  const [selectedTenantId, setSelectedTenantId] = useState<number | null>(null)
  const [mappings, setMappings] = useState<TenantDatasourceMappingVO[]>([])
  const [allDatasources, setAllDatasources] = useState<DataSourceConfig[]>([])
  const [modalVisible, setModalVisible] = useState(false)
  const [testLoading, setTestLoading] = useState<number | null>(null)
  const [form] = Form.useForm()

  const fetchTenants = async () => {
    try {
      const res = await getTenantList()
      setTenants(res || [])
    } catch {
      setTenants([])
    }
  }

  const fetchDatasources = async () => {
    try {
      const res = await getDatasourceAll()
      setAllDatasources(res || [])
    } catch {
      setAllDatasources([])
    }
  }

  const fetchMappings = async (tenantId: number) => {
    setLoading(true)
    try {
      const res = await getTenantDatasourceMappings(tenantId)
      setMappings(res || [])
    } catch {
      setMappings([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchTenants()
    fetchDatasources()
  }, [])

  useEffect(() => {
    if (selectedTenantId) {
      fetchMappings(selectedTenantId)
    } else {
      setMappings([])
    }
  }, [selectedTenantId])

  const handleAdd = () => {
    if (!selectedTenantId) {
      message.warning('请先选择租户')
      return
    }
    form.resetFields()
    form.setFieldsValue({ tenantId: selectedTenantId })
    setModalVisible(true)
  }

  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      await saveTenantDatasourceMapping(values)
      message.success('保存成功')
      setModalVisible(false)
      if (selectedTenantId) fetchMappings(selectedTenantId)
    } catch (e: any) {
      if (e.errorFields) return
      message.error('保存失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteTenantDatasourceMapping(id)
      message.success('删除成功')
      if (selectedTenantId) fetchMappings(selectedTenantId)
    } catch {
      message.error('删除失败')
    }
  }

  const handleTest = async (originalDsId: number) => {
    if (!selectedTenantId) return
    setTestLoading(originalDsId)
    try {
      const res = await testTenantDatasourceConnection(selectedTenantId, originalDsId)
      if (res.success) {
        message.success(`连接成功 - 数据源: ${res.dsName || '未知'}`)
      } else {
        message.error(`连接失败: ${res.message}`)
      }
    } catch {
      message.error('测试连接失败')
    } finally {
      setTestLoading(null)
    }
  }

  const columns: ColumnsType<TenantDatasourceMappingVO> = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    {
      title: '原始数据源', key: 'originalDs', width: 200,
      render: (_: any, record: TenantDatasourceMappingVO) => (
        <div>
          <div style={{ fontWeight: 500 }}>{record.originalDsName || `DS#${record.originalDsId}`}</div>
          <div style={{ fontSize: 12, color: '#999' }}>{record.originalDsCode}</div>
        </div>
      )
    },
    {
      title: '', key: 'arrow', width: 50, align: 'center',
      render: () => <SwapOutlined style={{ color: '#1890ff' }} />
    },
    {
      title: '目标数据源（租户专用）', key: 'targetDs', width: 200,
      render: (_: any, record: TenantDatasourceMappingVO) => (
        <div>
          <div style={{ fontWeight: 500, color: '#1890ff' }}>{record.targetDsName || `DS#${record.targetDsId}`}</div>
          <div style={{ fontSize: 12, color: '#999' }}>{record.targetDsCode}</div>
        </div>
      )
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (v: number) => v === 1 ? <Tag color="success">有效</Tag> : <Tag color="default">禁用</Tag>
    },
    { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
    {
      title: '操作', key: 'action', width: 180,
      render: (_: any, record: TenantDatasourceMappingVO) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<ApiOutlined />}
            loading={testLoading === record.originalDsId}
            onClick={() => handleTest(record.originalDsId)}
          >
            测试连接
          </Button>
          <Popconfirm title="确定删除该映射？" onConfirm={() => handleDelete(record.id!)} okText="确定" cancelText="取消">
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const unmappedDatasources = allDatasources.filter(
    ds => !mappings.some(m => m.originalDsId === ds.id)
  )

  return (
    <div style={{ padding: 24 }}>
      <Card>
        <Alert
          message="多租户数据源映射"
          description="为每个租户配置数据源映射后，该租户的用户执行报表时将自动使用映射后的数据源，实现一套模板多租户复用。未配置映射的数据源将使用原始默认连接。"
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />

        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <span style={{ lineHeight: '32px' }}>选择租户：</span>
            <Select
              style={{ width: 280 }}
              placeholder="请选择租户"
              value={selectedTenantId}
              onChange={setSelectedTenantId}
              showSearch
              optionFilterProp="children"
            >
              {tenants.map(t => (
                <Select.Option key={t.id} value={t.id!}>
                  {t.tenantName} ({t.tenantCode})
                </Select.Option>
              ))}
            </Select>
          </Space>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => selectedTenantId && fetchMappings(selectedTenantId)} disabled={!selectedTenantId}>刷新</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} disabled={!selectedTenantId}>添加映射</Button>
          </Space>
        </div>

        {selectedTenantId ? (
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={mappings}
            pagination={false}
            size="middle"
          />
        ) : (
          <Empty description="请选择租户以查看数据源映射" />
        )}
      </Card>

      <Modal
        title="添加数据源映射"
        open={modalVisible}
        onOk={handleSave}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
        width={520}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="tenantId" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="originalDsId" label="原始数据源" rules={[{ required: true, message: '请选择原始数据源' }]}>
            <Select placeholder="选择需要映射的原始数据源" showSearch optionFilterProp="children">
              {unmappedDatasources.map(ds => (
                <Select.Option key={ds.id} value={ds.id!}>
                  {ds.name} ({ds.type})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="targetDsId" label="目标数据源（租户专用）" rules={[{ required: true, message: '请选择目标数据源' }]}>
            <Select placeholder="选择该租户实际使用的数据源" showSearch optionFilterProp="children">
              {allDatasources.map(ds => (
                <Select.Option key={ds.id} value={ds.id!}>
                  {ds.name} ({ds.type})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default TenantDatasourceMappingPage
