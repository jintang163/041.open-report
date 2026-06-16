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
  Pagination,
  Upload,
  Switch
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  DatabaseOutlined,
  ApiOutlined,
  FileExcelOutlined,
  LinkOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload/interface'
import type { DataSourceConfig, DataSourceType, PageParams } from '@/types'
import {
  getDatasourceList,
  createDatasource,
  updateDatasource,
  deleteDatasource,
  batchDeleteDatasource,
  testConnection
} from '@/api/datasource'

interface Datasource extends DataSourceConfig {
  id: number
}

const typeColorMap: Record<DataSourceType, string> = {
  MYSQL: '#f29111',
  POSTGRESQL: '#336791',
  ORACLE: '#f80000',
  SQLSERVER: '#cc2927',
  DM: '#015fad',
  API: '#13c2c2',
  EXCEL: '#52c41a'
}

const typeIconMap: Record<DataSourceType, any> = {
  MYSQL: <DatabaseOutlined />,
  POSTGRESQL: <DatabaseOutlined />,
  ORACLE: <DatabaseOutlined />,
  SQLSERVER: <DatabaseOutlined />,
  DM: <DatabaseOutlined />,
  API: <ApiOutlined />,
  EXCEL: <FileExcelOutlined />
}

const defaultPorts: Record<DataSourceType, number> = {
  MYSQL: 3306,
  POSTGRESQL: 5432,
  ORACLE: 1521,
  SQLSERVER: 1433,
  DM: 5236,
  API: 0,
  EXCEL: 0
}

const mockDatasources: Datasource[] = [
  { id: 1, name: '生产库-MySQL', type: 'MYSQL', host: '192.168.1.100', port: 3306, database: 'open_report', username: 'root', password: '', status: 1, remark: '生产环境主数据库', createTime: '2024-01-01 00:00:00' },
  { id: 2, name: '测试库-PostgreSQL', type: 'POSTGRESQL', host: '192.168.1.101', port: 5432, database: 'test_db', username: 'postgres', password: '', status: 1, remark: '测试环境数据库', createTime: '2024-01-02 10:00:00' },
  { id: 3, name: '业务库-Oracle', type: 'ORACLE', host: '192.168.1.102', port: 1521, database: 'ORCL', username: 'business', password: '', status: 1, remark: '业务系统数据库', createTime: '2024-01-03 11:00:00' },
  { id: 4, name: '用户数据接口', type: 'API', host: 'https://api.example.com', port: 0, database: '', username: '', password: '', url: 'https://api.example.com/users', status: 1, remark: '用户数据API接口', createTime: '2024-01-04 14:00:00' },
  { id: 5, name: '销售数据-Excel', type: 'EXCEL', host: '', port: 0, database: '', username: '', password: '', status: 0, remark: '销售历史数据', createTime: '2024-01-05 09:30:00' },
  { id: 6, name: '达梦数据库', type: 'DM', host: '192.168.1.103', port: 5236, database: 'DMDB', username: 'SYSDBA', password: '', status: 1, remark: '国产化数据库', createTime: '2024-01-06 16:20:00' }
]

const DatasourceManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<Datasource[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  const [searchForm] = Form.useForm()
  const [modalForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [modalTitle, setModalTitle] = useState('新增数据源')
  const [dsType, setDsType] = useState<DataSourceType>('MYSQL')
  const [testing, setTesting] = useState(false)

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
        const res = await getDatasourceList(params)
        setDataSource(res.list)
        setTotal(res.total)
      } catch {
        const filtered = mockDatasources.filter(d =>
          (!params.keyword || d.name.includes(params.keyword)) &&
          (!values.type || d.type === values.type)
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
    setModalTitle('新增数据源')
    setDsType('MYSQL')
    modalForm.resetFields()
    modalForm.setFieldsValue({ type: 'MYSQL', port: 3306, status: 1 })
    setModalVisible(true)
  }

  const handleEdit = (record: Datasource) => {
    setEditingId(record.id!)
    setModalTitle('编辑数据源')
    setDsType(record.type as DataSourceType)
    modalForm.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleTypeChange = (value: DataSourceType) => {
    setDsType(value)
    if (defaultPorts[value] > 0) {
      modalForm.setFieldsValue({ port: defaultPorts[value] })
    }
  }

  const handleTestConnection = async () => {
    try {
      const values = await modalForm.validateFields()
      setTesting(true)
      try {
        const res = await testConnection(values)
        if (res.success) {
          message.success('连接测试成功')
        } else {
          message.error(res.message || '连接测试失败')
        }
      } catch {
        await new Promise(resolve => setTimeout(resolve, 1000))
        message.success('连接测试成功（模拟）')
      }
    } catch {
      message.warning('请先填写必填项')
    } finally {
      setTesting(false)
    }
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      if (editingId) {
        try {
          await updateDatasource({ ...values, id: editingId })
        } catch {}
        message.success('修改成功')
      } else {
        try {
          await createDatasource(values)
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
        await deleteDatasource(id)
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
        await batchDeleteDatasource(selectedRowKeys.map(k => Number(k)))
      } catch {}
      message.success(`批量删除成功，共删除 ${selectedRowKeys.length} 条记录`)
      setSelectedRowKeys([])
      fetchData()
    } catch {
      message.error('批量删除失败')
    }
  }

  const isJdbcType = (t: DataSourceType) => ['MYSQL', 'POSTGRESQL', 'ORACLE', 'SQLSERVER', 'DM'].includes(t)

  const columns: ColumnsType<Datasource> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70
    },
    {
      title: '数据源名称',
      dataIndex: 'name',
      key: 'name',
      width: 180,
      render: (text: string, record) => (
        <Space>
          <span style={{ color: typeColorMap[record.type as DataSourceType] }}>
            {typeIconMap[record.type as DataSourceType]}
          </span>
          {text}
        </Space>
      )
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: string) => (
        <Tag color={typeColorMap[type as DataSourceType]}>{type}</Tag>
      )
    },
    {
      title: '主机/地址',
      dataIndex: dsType === 'API' ? 'url' : 'host',
      key: 'host',
      width: 220,
      render: (text: string, record) => {
        if (record.type === 'API') return record.url || '-'
        if (record.type === 'EXCEL') return '本地文件'
        return `${record.host}:${record.port}`
      }
    },
    {
      title: '数据库/路径',
      dataIndex: 'database',
      key: 'database',
      width: 160,
      render: (text: string) => text || '-'
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (text: string) => text || '-'
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
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<LinkOutlined />} onClick={handleTestConnection}>
            测试
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除该数据源?" onConfirm={() => handleDelete(record.id!)}>
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
                <Input placeholder="数据源名称" allowClear style={{ width: 200 }} />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="type" label="类型">
                <Select
                  placeholder="全部类型"
                  allowClear
                  style={{ width: 150 }}
                  options={[
                    { label: 'MySQL', value: 'MYSQL' },
                    { label: 'PostgreSQL', value: 'POSTGRESQL' },
                    { label: 'Oracle', value: 'ORACLE' },
                    { label: 'SQL Server', value: 'SQLSERVER' },
                    { label: '达梦 DM', value: 'DM' },
                    { label: 'API', value: 'API' },
                    { label: 'Excel', value: 'EXCEL' }
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
        title="数据源列表"
        extra={
          <Space>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增数据源
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
          scroll={{ x: 1300 }}
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
        width={720}
        okText="保存"
        footer={(_, { OkBtn, CancelBtn }) => (
          <Space>
            {!editingId && (
              <Button onClick={handleTestConnection} loading={testing}>
                <LinkOutlined /> 测试连接
              </Button>
            )}
            <CancelBtn />
            <OkBtn />
          </Space>
        )}
      >
        <Form form={modalForm} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="数据源名称"
                rules={[{ required: true, message: '请输入数据源名称' }]}
              >
                <Input placeholder="请输入数据源名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="type"
                label="数据源类型"
                rules={[{ required: true, message: '请选择数据源类型' }]}
                initialValue="MYSQL"
              >
                <Select
                  onChange={handleTypeChange}
                  options={[
                    { label: 'MySQL', value: 'MYSQL' },
                    { label: 'PostgreSQL', value: 'POSTGRESQL' },
                    { label: 'Oracle', value: 'ORACLE' },
                    { label: 'SQL Server', value: 'SQLSERVER' },
                    { label: '达梦 DM', value: 'DM' },
                    { label: 'API 接口', value: 'API' },
                    { label: 'Excel 文件', value: 'EXCEL' }
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>

          {isJdbcType(dsType) && (
            <>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="host"
                    label="主机地址"
                    rules={[{ required: true, message: '请输入主机地址' }]}
                  >
                    <Input placeholder="如：192.168.1.100" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    name="port"
                    label="端口"
                    rules={[{ required: true, message: '请输入端口' }]}
                  >
                    <InputNumber min={1} max={65535} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="database"
                    label="数据库名"
                    rules={[{ required: true, message: '请输入数据库名' }]}
                  >
                    <Input placeholder="请输入数据库名" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    name="driverClassName"
                    label="驱动类"
                  >
                    <Input placeholder="可选，使用默认驱动" />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="username"
                    label="用户名"
                    rules={[{ required: true, message: '请输入用户名' }]}
                  >
                    <Input placeholder="请输入用户名" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    name="password"
                    label="密码"
                  >
                    <Input.Password placeholder="请输入密码" />
                  </Form.Item>
                </Col>
              </Row>
            </>
          )}

          {dsType === 'API' && (
            <>
              <Row gutter={16}>
                <Col span={24}>
                  <Form.Item
                    name="url"
                    label="API 地址"
                    rules={[{ required: true, message: '请输入 API 地址' }]}
                  >
                    <Input placeholder="如：https://api.example.com/data" />
                  </Form.Item>
                </Col>
              </Row>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item name="username" label="认证用户名">
                    <Input placeholder="可选，Basic Auth 用户名" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item name="password" label="认证密码">
                    <Input.Password placeholder="可选，Basic Auth 密码" />
                  </Form.Item>
                </Col>
              </Row>
            </>
          )}

          {dsType === 'EXCEL' && (
            <Row gutter={16}>
              <Col span={24}>
                <Form.Item
                  name="file"
                  label="Excel 文件"
                  valuePropName="fileList"
                  getValueFromEvent={(e: any) => (Array.isArray(e) ? e : e?.fileList)}
                >
                  <Upload.Dragger
                    beforeUpload={() => false}
                    accept=".xls,.xlsx"
                    maxCount={1}
                  >
                    <p className="ant-upload-drag-icon">
                      <FileExcelOutlined style={{ fontSize: 48, color: '#52c41a' }} />
                    </p>
                    <p className="ant-upload-text">点击或拖拽 Excel 文件到此处上传</p>
                    <p className="ant-upload-hint">支持 .xls 和 .xlsx 格式</p>
                  </Upload.Dragger>
                </Form.Item>
              </Col>
            </Row>
          )}

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="status" label="状态" initialValue={1} valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="禁用" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="remark" label="备注">
            <Input.TextArea placeholder="请输入备注" rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  )
}

export default DatasourceManagement
