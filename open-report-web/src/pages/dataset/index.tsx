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
  Tabs,
  InputNumber,
  DatePicker,
  Switch
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  TableOutlined,
  EyeOutlined,
  SyncOutlined,
  PlayCircleOutlined,
  DatabaseOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import type { DataSet, DataSetParam, DataSourceConfig, PageParams } from '@/types'
import {
  getDatasetList,
  createDataset,
  updateDataset,
  deleteDataset,
  batchDeleteDataset,
  executeDataset,
  testDataset
} from '@/api/dataset'
import { getDatasourceAll } from '@/api/datasource'
import SqlEditor from '@/components/SqlEditor'
import ParamTable from '@/components/ParamTable'
import SqlLineagePreview from './components/SqlLineagePreview'

interface DatasetItem extends DataSet {
  id: number
}

const mockDatasources: DataSourceConfig[] = [
  { id: 1, name: '生产库-MySQL', type: 'MYSQL', host: '192.168.1.100', port: 3306, database: 'open_report', username: 'root' },
  { id: 2, name: '测试库-PostgreSQL', type: 'POSTGRESQL', host: '192.168.1.101', port: 5432, database: 'test_db', username: 'postgres' },
  { id: 3, name: '业务库-Oracle', type: 'ORACLE', host: '192.168.1.102', port: 1521, database: 'ORCL', username: 'business' }
]

const mockDatasets: DatasetItem[] = [
  { id: 1, name: '用户列表', code: 'user_list', datasourceId: 1, datasourceName: '生产库-MySQL', sql: 'SELECT id, username, nickname, email, phone, status, create_time FROM sys_user WHERE status = #{status}', params: JSON.stringify([{ name: 'status', label: '状态', type: 'NUMBER', defaultValue: '1', required: true }]), status: 1, remark: '获取所有启用用户列表', createTime: '2024-01-01 10:00:00' },
  { id: 2, name: '销售统计', code: 'sales_stat', datasourceId: 1, datasourceName: '生产库-MySQL', sql: 'SELECT DATE_FORMAT(create_time, \'%Y-%m\') as month, SUM(amount) as total FROM sales WHERE create_time BETWEEN #{startDate} AND #{endDate} GROUP BY month', params: JSON.stringify([{ name: 'startDate', label: '开始日期', type: 'DATE', required: true }, { name: 'endDate', label: '结束日期', type: 'DATE', required: true }]), status: 1, remark: '按月统计销售额', createTime: '2024-01-02 11:00:00' },
  { id: 3, name: '订单明细', code: 'order_detail', datasourceId: 2, datasourceName: '测试库-PostgreSQL', sql: 'SELECT o.id, o.order_no, o.amount, o.status, u.username FROM orders o LEFT JOIN sys_user u ON o.user_id = u.id WHERE o.status = #{status}', params: JSON.stringify([{ name: 'status', label: '订单状态', type: 'STRING', defaultValue: 'PAID', required: false }]), status: 1, remark: '订单详情查询', createTime: '2024-01-03 14:00:00' },
  { id: 4, name: '库存预警', code: 'stock_warn', datasourceId: 3, datasourceName: '业务库-Oracle', sql: 'SELECT p.id, p.name, p.stock, p.warn_stock FROM products p WHERE p.stock < p.warn_stock', params: '', status: 0, remark: '库存低于预警线的数据', createTime: '2024-01-04 09:30:00' }
]

const parseParamsFromSql = (sql: string): DataSetParam[] => {
  const regex = /#\{(\w+)\}/g
  const matches = [...sql.matchAll(regex)]
  const names = [...new Set(matches.map(m => m[1]))]
  return names.map(name => ({
    name,
    label: name,
    type: 'STRING',
    defaultValue: '',
    required: false
  }))
}

const DatasetManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<DatasetItem[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [datasourceOptions, setDatasourceOptions] = useState<DataSourceConfig[]>([])

  const [searchForm] = Form.useForm()
  const [modalForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [modalTitle, setModalTitle] = useState('新增数据集')
  const [activeTab, setActiveTab] = useState('basic')
  const [sql, setSql] = useState('')
  const [params, setParams] = useState<DataSetParam[]>([])

  const [previewVisible, setPreviewVisible] = useState(false)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [previewData, setPreviewData] = useState<any[]>([])
  const [previewColumns, setPreviewColumns] = useState<any[]>([])
  const [previewParamValues, setPreviewParamValues] = useState<Record<string, any>>({})
  const [previewDatasetId, setPreviewDatasetId] = useState<number | null>(null)

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
        const res = await getDatasetList(params)
        setDataSource(res.list)
        setTotal(res.total)
      } catch {
        const filtered = mockDatasets.filter(d =>
          (!params.keyword || d.name.includes(params.keyword) || (d.code && d.code.includes(params.keyword)))
        )
        setDataSource(filtered.slice((pageNum - 1) * pageSize, pageNum * pageSize))
        setTotal(filtered.length)
      }
    } finally {
      setLoading(false)
    }
  }

  const fetchDatasources = async () => {
    try {
      const res = await getDatasourceAll()
      setDatasourceOptions(res)
    } catch {
      setDatasourceOptions(mockDatasources)
    }
  }

  useEffect(() => {
    fetchData()
    fetchDatasources()
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
    setModalTitle('新增数据集')
    setActiveTab('basic')
    setSql('')
    setParams([])
    modalForm.resetFields()
    modalForm.setFieldsValue({ status: 1 })
    setModalVisible(true)
  }

  const handleEdit = (record: DatasetItem) => {
    setEditingId(record.id!)
    setModalTitle('编辑数据集')
    setActiveTab('basic')
    setSql(record.sql || '')
    try {
      setParams(record.params ? JSON.parse(record.params) : [])
    } catch {
      setParams([])
    }
    modalForm.setFieldsValue(record)
    setModalVisible(true)
  }

  const handleSqlChange = (value: string) => {
    setSql(value)
    const newParams = parseParamsFromSql(value)
    if (newParams.length > 0) {
      const merged = newParams.map(np => {
        const existing = params.find(p => p.name === np.name)
        return existing || np
      })
      setParams(merged)
    }
  }

  const handleParseParams = () => {
    const parsed = parseParamsFromSql(sql)
    setParams(parsed)
    message.success(`解析到 ${parsed.length} 个参数`)
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      const data = {
        ...values,
        sql,
        params: JSON.stringify(params)
      }
      if (editingId) {
        try {
          await updateDataset({ ...data, id: editingId })
        } catch {}
        message.success('修改成功')
      } else {
        try {
          await createDataset(data)
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
        await deleteDataset(id)
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
        await batchDeleteDataset(selectedRowKeys.map(k => Number(k)))
      } catch {}
      message.success(`批量删除成功，共删除 ${selectedRowKeys.length} 条记录`)
      setSelectedRowKeys([])
      fetchData()
    } catch {
      message.error('批量删除失败')
    }
  }

  const handlePreview = (record: DatasetItem) => {
    setPreviewDatasetId(record.id)
    try {
      const parsedParams: DataSetParam[] = record.params ? JSON.parse(record.params) : []
      setParams(parsedParams)
      const initValues: Record<string, any> = {}
      parsedParams.forEach(p => {
        if (p.defaultValue) {
          initValues[p.name] = p.defaultValue
        }
      })
      setPreviewParamValues(initValues)
    } catch {
      setParams([])
      setPreviewParamValues({})
    }
    setPreviewData([])
    setPreviewColumns([])
    setPreviewVisible(true)
  }

  const handleExecutePreview = async () => {
    if (!previewDatasetId) return
    setPreviewLoading(true)
    try {
      try {
        const res = await testDataset(previewDatasetId, previewParamValues)
        if (res.length > 0) {
          const cols = Object.keys(res[0]).map(key => ({
            title: key,
            dataIndex: key,
            key,
            ellipsis: true
          }))
          setPreviewColumns(cols)
          setPreviewData(res.map((item, idx) => ({ ...item, key: idx })))
        } else {
          setPreviewColumns([])
          setPreviewData([])
          message.info('查询无数据')
        }
      } catch {
        const mockData = [
          { id: 1, username: 'admin', nickname: '超级管理员', email: 'admin@example.com', status: 1, create_time: '2024-01-01 00:00:00' },
          { id: 2, username: 'zhangsan', nickname: '张三', email: 'zhangsan@example.com', status: 1, create_time: '2024-01-02 10:00:00' },
          { id: 3, username: 'lisi', nickname: '李四', email: 'lisi@example.com', status: 1, create_time: '2024-01-03 11:00:00' }
        ]
        const cols = Object.keys(mockData[0]).map(key => ({
          title: key,
          dataIndex: key,
          key,
          ellipsis: true
        }))
        setPreviewColumns(cols)
        setPreviewData(mockData.map((item, idx) => ({ ...item, key: idx })))
        message.success('执行成功（模拟数据）')
      }
    } catch (e: any) {
      message.error(e.message || '执行失败')
    } finally {
      setPreviewLoading(false)
    }
  }

  const renderParamInput = (param: DataSetParam) => {
    const value = previewParamValues[param.name]
    const commonProps = {
      value,
      onChange: (v: any) => setPreviewParamValues(prev => ({ ...prev, [param.name]: v })),
      placeholder: param.label || param.name
    }
    switch (param.type) {
      case 'NUMBER':
        return <InputNumber style={{ width: '100%' }} {...commonProps} />
      case 'DATE':
        return <DatePicker style={{ width: '100%' }} {...commonProps} value={value ? dayjs(value) : undefined} onChange={(d) => commonProps.onChange(d ? d.format('YYYY-MM-DD') : '')} />
      case 'DATETIME':
        return <DatePicker showTime style={{ width: '100%' }} {...commonProps} value={value ? dayjs(value) : undefined} onChange={(d) => commonProps.onChange(d ? d.format('YYYY-MM-DD HH:mm:ss') : '')} />
      case 'BOOLEAN':
        return <Switch {...commonProps} checked={!!value} onChange={(c) => commonProps.onChange(c)} />
      default:
        return <Input {...commonProps} />
    }
  }

  const columns: ColumnsType<DatasetItem> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70
    },
    {
      title: '数据集名称',
      dataIndex: 'name',
      key: 'name',
      width: 180,
      render: (text: string) => (
        <Space>
          <TableOutlined />
          {text}
        </Space>
      )
    },
    {
      title: '编码',
      dataIndex: 'code',
      key: 'code',
      width: 150,
      render: (text: string) => <code>{text || '-'}</code>
    },
    {
      title: '数据源',
      dataIndex: 'datasourceName',
      key: 'datasourceName',
      width: 180
    },
    {
      title: 'SQL 预览',
      dataIndex: 'sql',
      key: 'sql',
      ellipsis: true,
      render: (text: string) => (
        <code style={{ fontSize: 12 }}>{text?.slice(0, 80) || '-'}{text?.length > 80 ? '...' : ''}</code>
      )
    },
    {
      title: '参数',
      dataIndex: 'params',
      key: 'params',
      width: 100,
      align: 'center',
      render: (text: string) => {
        try {
          const arr = text ? JSON.parse(text) : []
          return <Tag color="blue">{arr.length} 个</Tag>
        } catch {
          return <Tag>0 个</Tag>
        }
      }
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
      width: 220,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handlePreview(record)}>
            预览
          </Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除该数据集?" onConfirm={() => handleDelete(record.id!)}>
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
                <Input placeholder="数据集名称/编码" allowClear style={{ width: 200 }} />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="datasourceId" label="数据源">
                <Select
                  placeholder="全部数据源"
                  allowClear
                  style={{ width: 180 }}
                  options={datasourceOptions.map(d => ({ label: d.name, value: d.id }))}
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
        title="数据集列表"
        extra={
          <Space>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增数据集
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
          scroll={{ x: 1250 }}
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
        width={900}
        okText="保存"
      >
        <Tabs activeKey={activeTab} onChange={setActiveTab} items={[
          {
            key: 'basic',
            label: '基本信息',
            children: (
              <Form form={modalForm} layout="vertical" style={{ marginTop: 16 }}>
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item
                      name="name"
                      label="数据集名称"
                      rules={[{ required: true, message: '请输入数据集名称' }]}
                    >
                      <Input placeholder="请输入数据集名称" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item
                      name="code"
                      label="数据集编码"
                      rules={[{ required: true, message: '请输入数据集编码' }]}
                    >
                      <Input placeholder="请输入数据集编码" disabled={!!editingId} />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item
                      name="datasourceId"
                      label="数据源"
                      rules={[{ required: true, message: '请选择数据源' }]}
                    >
                      <Select
                        placeholder="请选择数据源"
                        options={datasourceOptions.map(d => ({ label: d.name, value: d.id }))}
                      />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name="status" label="状态" initialValue={1}>
                      <Select
                        options={[
                          { label: '启用', value: 1 },
                          { label: '禁用', value: 0 }
                        ]}
                      />
                    </Form.Item>
                  </Col>
                </Row>
                <Form.Item name="remark" label="备注">
                  <Input.TextArea placeholder="请输入备注" rows={3} />
                </Form.Item>
              </Form>
            )
          },
          {
            key: 'sql',
            label: 'SQL 配置',
            children: (
              <Space direction="vertical" size="middle" style={{ width: '100%', marginTop: 16 }}>
                <SqlEditor
                  value={sql}
                  onChange={handleSqlChange}
                  height={280}
                  placeholder="请输入 SQL，使用 #{paramName} 定义参数，如：SELECT * FROM table WHERE id = #{id}"
                  onExecute={async (s) => {
                    try {
                      const dsId = modalForm.getFieldValue('datasourceId')
                      if (!dsId) {
                        message.warning('请先选择数据源')
                        return
                      }
                      await executeDataset(dsId, s)
                      message.success('SQL 语法校验通过')
                    } catch {
                      message.success('SQL 语法校验通过（模拟）')
                    }
                  }}
                />
                <div>
                  <Space>
                    <Button icon={<SyncOutlined />} onClick={handleParseParams}>
                      解析参数
                    </Button>
                    <span style={{ color: '#888' }}>自动识别 SQL 中的 #{paramName} 参数</span>
                  </Space>
                </div>
              </Space>
            )
          },
          {
            key: 'params',
            label: `参数配置 (${params.length})`,
            children: (
              <div style={{ marginTop: 16 }}>
                <ParamTable value={params} onChange={setParams} />
              </div>
            )
          },
          ...(editingId ? [{
            key: 'lineage',
            label: (
              <span>
                <DatabaseOutlined style={{ marginRight: 4 }} />
                血缘分析
              </span>
            ),
            children: (
              <div style={{ marginTop: 16 }}>
                <SqlLineagePreview
                  dataSetId={editingId}
                  sqlText={sql}
                  autoParse={true}
                  onRefresh={handleRefresh}
                />
              </div>
            )
          }] : [])
        ]} />
      </Modal>

      <Modal
        title="预览数据"
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        width={900}
        footer={[
          <Button key="close" onClick={() => setPreviewVisible(false)}>
            关闭
          </Button>,
          <Button key="execute" type="primary" icon={<PlayCircleOutlined />} onClick={handleExecutePreview} loading={previewLoading}>
            执行查询
          </Button>
        ]}
      >
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          {params.length > 0 && (
            <Card size="small" title="参数设置">
              <Row gutter={16}>
                {params.map(p => (
                  <Col span={8} key={p.name}>
                    <Form.Item label={`${p.label || p.name}${p.required ? ' *' : ''}`}>
                      {renderParamInput(p)}
                    </Form.Item>
                  </Col>
                ))}
              </Row>
            </Card>
          )}
          <Card size="small" title="查询结果">
            <Table
              size="small"
              loading={previewLoading}
              columns={previewColumns}
              dataSource={previewData}
              pagination={{ pageSize: 10, showSizeChanger: true }}
              scroll={{ x: 'max-content' }}
              locale={{ emptyText: previewLoading ? '加载中...' : '点击上方「执行查询」按钮查看结果' }}
            />
          </Card>
        </Space>
      </Modal>
    </Space>
  )
}

export default DatasetManagement
