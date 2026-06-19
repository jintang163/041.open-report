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
  Tabs,
  Tooltip,
  Divider,
  Empty,
  Collapse,
  Code,
  InputNumber
} from 'antd'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  SearchOutlined,
  HistoryOutlined,
  PlayCircleOutlined,
  CodeOutlined,
  ExperimentOutlined,
  SyncOutlined,
  InfoCircleOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { FunctionParam, FunctionCategory, ReportFunction, ReportFunctionVersion } from '@/types'
import {
  getFunctionPage,
  createFunction,
  updateFunction,
  deleteFunction,
  getFunctionVersions,
  switchFunctionVersion,
  testExecuteScript,
  validateScript,
  reloadFunctions
} from '@/api/function'

const CATEGORY_OPTIONS: { label: string; value: FunctionCategory; color: string }[] = [
  { label: '数学函数', value: 'MATH', color: 'blue' },
  { label: '日期函数', value: 'DATE', color: 'green' },
  { label: '字符串函数', value: 'STRING', color: 'orange' },
  { label: '逻辑函数', value: 'LOGIC', color: 'purple' },
  { label: '自定义函数', value: 'CUSTOM', color: 'magenta' }
]

const CATEGORY_MAP = CATEGORY_OPTIONS.reduce((acc, cur) => {
  acc[cur.value] = cur
  return acc
}, {} as Record<string, typeof CATEGORY_OPTIONS[0]>)

const DEFAULT_SCRIPT = `// 自定义函数脚本示例
// 可用变量:
//   args - 函数参数列表 (List<Object>)
//   dataSets - 数据集数据 (Map<String, List<Map<String, Object>>>)
//   currentRow - 当前行索引 (int)
//   parameters - 报表参数 (Map<String, Object>)

// 示例: 计算两个数的乘积
// if (args.size() < 2) return 0
// def a = args[0] as double
// def b = args[1] as double
// return a * b

// 示例: 字符串处理
// if (args.size() < 1) return ""
// def str = args[0]?.toString() ?: ""
// return str.trim().toUpperCase()

// 返回函数结果
return args[0]
`

const FunctionManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<ReportFunction[]>([])
  const [total, setTotal] = useState(0)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  const [searchForm] = Form.useForm()
  const [modalForm] = Form.useForm()
  const [paramForm] = Form.useForm()

  const [modalVisible, setModalVisible] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [modalTitle, setModalTitle] = useState('新增函数')
  const [activeTab, setActiveTab] = useState('basic')

  const [scriptContent, setScriptContent] = useState('')
  const [params, setParams] = useState<FunctionParam[]>([])
  const [category, setCategory] = useState<FunctionCategory>('CUSTOM')

  const [versionVisible, setVersionVisible] = useState(false)
  const [versionList, setVersionList] = useState<ReportFunctionVersion[]>([])
  const [currentFuncId, setCurrentFuncId] = useState<number | null>(null)

  const [testVisible, setTestVisible] = useState(false)
  const [testLoading, setTestLoading] = useState(false)
  const [testResult, setTestResult] = useState<any>(null)
  const [testArgs, setTestArgs] = useState<string>('[]')

  const fetchData = async () => {
    setLoading(true)
    try {
      const values = searchForm.getFieldsValue()
      const res = await getFunctionPage({
        pageNum,
        pageSize,
        funcName: values.keyword,
        category: values.category,
        status: values.status
      })
      setDataSource(res.records || [])
      setTotal(res.total || 0)
    } catch (e: any) {
      message.error(e.message || '加载函数列表失败')
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
    setModalTitle('新增函数')
    setActiveTab('basic')
    setScriptContent(DEFAULT_SCRIPT)
    setParams([])
    setCategory('CUSTOM')
    modalForm.resetFields()
    modalForm.setFieldsValue({ status: 1, funcCategory: 'CUSTOM' })
    paramForm.resetFields()
    setModalVisible(true)
  }

  const handleEdit = async (record: ReportFunction) => {
    setEditingId(record.id!)
    setModalTitle('编辑函数')
    setActiveTab('basic')
    setCategory(record.funcCategory || 'CUSTOM')

    try {
      const versions = await getFunctionVersions(record.id!)
      const currentVersion = versions.find(v => v.version === record.currentVersion)
      setScriptContent(currentVersion?.scriptContent || DEFAULT_SCRIPT)
    } catch {
      setScriptContent(DEFAULT_SCRIPT)
    }

    try {
      if (record.paramConfig) {
        setParams(JSON.parse(record.paramConfig))
      } else {
        setParams([])
      }
    } catch {
      setParams([])
    }

    modalForm.setFieldsValue({
      ...record,
      status: record.status ?? 1
    })
    setModalVisible(true)
  }

  const handleCategoryChange = (val: FunctionCategory) => {
    setCategory(val)
  }

  const handleAddParam = () => {
    paramForm.validateFields().then(() => {
      const values = paramForm.getFieldsValue()
      setParams(prev => [...prev, {
        name: values.name,
        type: values.type || 'String',
        required: values.required ?? true,
        description: values.description
      }])
      paramForm.resetFields()
    }).catch(() => {})
  }

  const handleRemoveParam = (index: number) => {
    setParams(prev => prev.filter((_, i) => i !== index))
  }

  const handleValidateScript = async () => {
    if (!scriptContent.trim()) {
      message.warning('脚本内容不能为空')
      return
    }
    try {
      await validateScript(scriptContent)
      message.success('脚本语法验证通过')
    } catch (e: any) {
      message.error(e.message || '脚本语法错误')
    }
  }

  const handleModalOk = async () => {
    try {
      const values = await modalForm.validateFields()
      const data: ReportFunction = {
        ...values,
        funcCategory: category,
        paramConfig: JSON.stringify(params),
        scriptContent,
        changeLog: values.changeLog
      }
      if (editingId) {
        data.id = editingId
        await updateFunction(data)
        message.success('修改成功')
      } else {
        await createFunction(data)
        message.success('新增成功')
      }
      setModalVisible(false)
      fetchData()
    } catch (e: any) {
      if (e?.errorFields) return
      message.error(e.message || '保存失败')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteFunction(id)
      message.success('删除成功')
      fetchData()
    } catch (e: any) {
      message.error(e.message || '删除失败')
    }
  }

  const handleReload = async () => {
    try {
      await reloadFunctions()
      message.success('重新加载成功')
      fetchData()
    } catch (e: any) {
      message.error(e.message || '重新加载失败')
    }
  }

  const handleViewVersions = async (record: ReportFunction) => {
    setCurrentFuncId(record.id!)
    try {
      const versions = await getFunctionVersions(record.id!)
      setVersionList(versions)
    } catch {
      setVersionList([])
    }
    setVersionVisible(true)
  }

  const handleSwitchVersion = async (version: number) => {
    if (!currentFuncId) return
    try {
      await switchFunctionVersion(currentFuncId, version)
      message.success(`已切换到版本 v${version}`)
      const versions = await getFunctionVersions(currentFuncId)
      setVersionList(versions)
      fetchData()
    } catch (e: any) {
      message.error(e.message || '切换版本失败')
    }
  }

  const handleOpenTest = (record?: ReportFunction) => {
    setTestResult(null)
    setTestArgs('[]')
    if (record) {
      getFunctionVersions(record.id!).then(versions => {
        const currentVersion = versions.find(v => v.version === record.currentVersion)
        if (currentVersion?.scriptContent) {
          setScriptContent(currentVersion.scriptContent)
        }
      }).catch(() => {})
    }
    setTestVisible(true)
  }

  const handleRunTest = async () => {
    if (!scriptContent.trim()) {
      message.warning('脚本内容不能为空')
      return
    }
    setTestLoading(true)
    try {
      let args: any[] = []
      try {
        args = JSON.parse(testArgs)
      } catch {
        message.error('测试参数格式错误，请输入正确的JSON数组')
        return
      }
      const res = await testExecuteScript(scriptContent, { args })
      setTestResult(res)
      message.success('执行成功')
    } catch (e: any) {
      message.error(e.message || '执行失败')
      setTestResult({ error: e.message })
    } finally {
      setTestLoading(false)
    }
  }

  const columns: ColumnsType<ReportFunction> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70
    },
    {
      title: '函数名称',
      dataIndex: 'funcName',
      key: 'funcName',
      width: 150,
      render: (text: string) => <Code style={{ background: 'transparent', color: '#1677ff' }}>{text}</Code>
    },
    {
      title: '显示名称',
      dataIndex: 'funcLabel',
      key: 'funcLabel',
      width: 200
    },
    {
      title: '分类',
      dataIndex: 'funcCategory',
      key: 'funcCategory',
      width: 100,
      align: 'center',
      render: (val: FunctionCategory) => {
        const cat = CATEGORY_MAP[val]
        return cat ? <Tag color={cat.color}>{cat.label}</Tag> : <Tag>{val}</Tag>
      }
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (text: string) => text || '-'
    },
    {
      title: '返回类型',
      dataIndex: 'returnType',
      key: 'returnType',
      width: 100,
      align: 'center',
      render: (text: string) => text || '-'
    },
    {
      title: '版本',
      dataIndex: 'currentVersion',
      key: 'currentVersion',
      width: 80,
      align: 'center',
      render: (v: number) => <Tag>v{v}</Tag>
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
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<ExperimentOutlined />}
            onClick={() => handleOpenTest(record)}
          >
            测试
          </Button>
          <Button
            type="link"
            size="small"
            icon={<HistoryOutlined />}
            onClick={() => handleViewVersions(record)}
          >
            版本
          </Button>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
            disabled={record.funcCategory !== 'CUSTOM'}
          >
            编辑
          </Button>
          {record.funcCategory === 'CUSTOM' && (
            <Popconfirm title="确定删除该函数?" onConfirm={() => handleDelete(record.id!)}>
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      )
    }
  ]

  const renderParamRow = (p: FunctionParam, index: number) => (
    <Row key={index} gutter={8} align="middle" style={{ marginBottom: 8 }}>
      <Col span={5}>
        <Input value={p.name} readOnly size="small" />
      </Col>
      <Col span={4}>
        <Tag>{p.type}</Tag>
      </Col>
      <Col span={3}>
        <Tag color={p.required ? 'red' : 'default'}>{p.required ? '必填' : '可选'}</Tag>
      </Col>
      <Col span={10}>
        <Input value={p.description} readOnly size="small" placeholder="参数描述" />
      </Col>
      <Col span={2}>
        <Button type="text" size="small" danger onClick={() => handleRemoveParam(index)}>
          删除
        </Button>
      </Col>
    </Row>
  )

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card size="small">
        <Form form={searchForm} layout="inline">
          <Row gutter={16}>
            <Col>
              <Form.Item name="keyword" label="关键字">
                <Input placeholder="函数名称/显示名称" allowClear style={{ width: 200 }} />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="category" label="分类">
                <Select
                  placeholder="全部分类"
                  allowClear
                  style={{ width: 150 }}
                  options={CATEGORY_OPTIONS.map(c => ({ label: c.label, value: c.value }))}
                />
              </Form.Item>
            </Col>
            <Col>
              <Form.Item name="status" label="状态">
                <Select
                  placeholder="全部状态"
                  allowClear
                  style={{ width: 120 }}
                  options={[
                    { label: '启用', value: 1 },
                    { label: '禁用', value: 0 }
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
        title="函数仓库"
        extra={
          <Space>
            <Tooltip title="重新加载运行时函数">
              <Button icon={<SyncOutlined />} onClick={handleReload}>
                刷新缓存
              </Button>
            </Tooltip>
            <Button icon={<ExperimentOutlined />} onClick={() => handleOpenTest()}>
              脚本测试
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              新增函数
            </Button>
          </Space>
        }
      >
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          pagination={{
            current: pageNum,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, size) => {
              setPageNum(page)
              setPageSize(size)
            }
          }}
          scroll={{ x: 1350 }}
        />
      </Card>

      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        destroyOnClose
        width={1000}
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
                      name="funcName"
                      label="函数名称"
                      rules={[{ required: true, message: '请输入函数名称' }]}
                      extra="用于在公式中调用，如 SUM()"
                    >
                      <Input placeholder="请输入函数英文名称，如 MY_FUNCTION" disabled={!!editingId} />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item
                      name="funcLabel"
                      label="显示名称"
                      rules={[{ required: true, message: '请输入显示名称' }]}
                    >
                      <Input placeholder="请输入函数显示名称，如 自定义求和" />
                    </Form.Item>
                  </Col>
                </Row>
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item
                      name="funcCategory"
                      label="函数分类"
                      rules={[{ required: true, message: '请选择分类' }]}
                    >
                      <Select
                        placeholder="请选择分类"
                        onChange={handleCategoryChange}
                        options={CATEGORY_OPTIONS.map(c => ({ label: c.label, value: c.value }))}
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
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item name="returnType" label="返回值类型">
                      <Input placeholder="如 String / Number / Object" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name="changeLog" label="版本变更说明">
                      <Input placeholder="描述本次变更内容" />
                    </Form.Item>
                  </Col>
                </Row>
                <Form.Item name="description" label="函数描述">
                  <Input.TextArea placeholder="请输入函数功能描述" rows={3} />
                </Form.Item>
                <Form.Item name="example" label="使用示例">
                  <Input.TextArea placeholder="如 ${MY_FUNCTION(arg1, arg2)}" rows={2} />
                </Form.Item>
              </Form>
            )
          },
          {
            key: 'params',
            label: `参数配置 (${params.length})`,
            children: (
              <div style={{ marginTop: 16 }}>
                {params.length > 0 ? (
                  <>
                    <Row gutter={8} style={{ marginBottom: 8, color: '#666', fontSize: 12 }}>
                      <Col span={5}>参数名</Col>
                      <Col span={4}>类型</Col>
                      <Col span={3}>必填</Col>
                      <Col span={10}>描述</Col>
                      <Col span={2}>操作</Col>
                    </Row>
                    {params.map((p, i) => renderParamRow(p, i))}
                    <Divider style={{ margin: '12px 0' }} />
                  </>
                ) : (
                  <Empty description="暂无参数" style={{ padding: '20px 0' }} />
                )}
                <Card size="small" title="添加参数">
                  <Form form={paramForm} layout="inline">
                    <Form.Item
                      name="name"
                      label="参数名"
                      rules={[{ required: true, message: '请输入参数名' }]}
                    >
                      <Input placeholder="如 arg1" style={{ width: 150 }} />
                    </Form.Item>
                    <Form.Item name="type" label="类型" initialValue="String">
                      <Select
                        style={{ width: 120 }}
                        options={[
                          { label: 'String', value: 'String' },
                          { label: 'Number', value: 'Number' },
                          { label: 'Boolean', value: 'Boolean' },
                          { label: 'Date', value: 'Date' },
                          { label: 'Object', value: 'Object' },
                          { label: 'Expression', value: 'Expression' }
                        ]}
                      />
                    </Form.Item>
                    <Form.Item name="required" label="必填" valuePropName="checked" initialValue={true}>
                      <Select
                        style={{ width: 80 }}
                        options={[
                          { label: '是', value: true },
                          { label: '否', value: false }
                        ]}
                      />
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                      <Input placeholder="参数描述" style={{ width: 200 }} />
                    </Form.Item>
                    <Form.Item>
                      <Button type="primary" icon={<PlusOutlined />} onClick={handleAddParam}>
                        添加
                      </Button>
                    </Form.Item>
                  </Form>
                </Card>
              </div>
            )
          },
          {
            key: 'script',
            label: 'Groovy 脚本',
            children: (
              <div style={{ marginTop: 16 }}>
                <AlertBanner />
                <Space style={{ marginBottom: 8 }}>
                  <Button icon={<CodeOutlined />} onClick={handleValidateScript}>
                    语法检查
                  </Button>
                </Space>
                <Input.TextArea
                  value={scriptContent}
                  onChange={(e) => setScriptContent(e.target.value)}
                  rows={18}
                  placeholder="请输入 Groovy 脚本..."
                  style={{ fontFamily: 'Consolas, Monaco, monospace', fontSize: 13 }}
                  disabled={category !== 'CUSTOM'}
                />
              </div>
            )
          }
        ]} />
      </Modal>

      <Modal
        title="版本历史"
        open={versionVisible}
        onCancel={() => setVersionVisible(false)}
        footer={[
          <Button key="close" onClick={() => setVersionVisible(false)}>关闭</Button>
        ]}
        width={900}
      >
        {versionList.length === 0 ? (
          <Empty description="暂无版本记录" />
        ) : (
          <Collapse
            items={versionList.map((v) => ({
              key: String(v.version),
              label: (
                <Space>
                  <Tag color="blue">v{v.version}</Tag>
                  <span style={{ color: '#666' }}>{v.createTime}</span>
                  <span>{v.changeLog || '-'}</span>
                </Space>
              ),
              children: (
                <div>
                  <div style={{ marginBottom: 8 }}>
                    <Button
                      type="primary"
                      size="small"
                      onClick={() => handleSwitchVersion(v.version)}
                    >
                      切换到此版本
                    </Button>
                  </div>
                  <Input.TextArea
                    value={v.scriptContent || ''}
                    readOnly
                    rows={10}
                    style={{ fontFamily: 'Consolas, Monaco, monospace', fontSize: 12 }}
                  />
                </div>
              )
            }))}
          />
        )}
      </Modal>

      <Modal
        title="脚本测试工具"
        open={testVisible}
        onCancel={() => setTestVisible(false)}
        width={900}
        footer={[
          <Button key="close" onClick={() => setTestVisible(false)}>关闭</Button>,
          <Button key="validate" icon={<CodeOutlined />} onClick={handleValidateScript}>语法检查</Button>,
          <Button key="run" type="primary" icon={<PlayCircleOutlined />} onClick={handleRunTest} loading={testLoading}>
            运行
          </Button>
        ]}
      >
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <div>
            <div style={{ marginBottom: 8, color: '#666' }}>Groovy 脚本:</div>
            <Input.TextArea
              value={scriptContent}
              onChange={(e) => setScriptContent(e.target.value)}
              rows={12}
              style={{ fontFamily: 'Consolas, Monaco, monospace', fontSize: 13 }}
              placeholder="请输入 Groovy 脚本..."
            />
          </div>
          <div>
            <div style={{ marginBottom: 8, color: '#666' }}>
              测试参数 (JSON数组，对应 args 变量):
              <Tooltip title="例如: [1, 2, 3] 或 [\"hello\", \"world\"]">
                <InfoCircleOutlined style={{ marginLeft: 4 }} />
              </Tooltip>
            </div>
            <Input.TextArea
              value={testArgs}
              onChange={(e) => setTestArgs(e.target.value)}
              rows={3}
              style={{ fontFamily: 'Consolas, Monaco, monospace' }}
              placeholder='例如: [1, 2, 3]'
            />
          </div>
          <div>
            <div style={{ marginBottom: 8, color: '#666' }}>执行结果:</div>
            <Card size="small" style={{ background: '#fafafa', minHeight: 80 }}>
              {testResult === null ? (
                <span style={{ color: '#999' }}>点击「运行」查看执行结果</span>
              ) : (
                <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                  {typeof testResult === 'object' ? JSON.stringify(testResult, null, 2) : String(testResult)}
                </pre>
              )}
            </Card>
          </div>
        </Space>
      </Modal>
    </Space>
  )
}

const AlertBanner = () => (
  <Card size="small" style={{ marginBottom: 12, background: '#e6f4ff', border: '1px solid #91caff' }}>
    <div style={{ color: '#0958d9', fontSize: 13, lineHeight: 1.6 }}>
      <strong>脚本可用变量:</strong><br />
      <Code>args</Code> - 函数参数列表 (List)<br />
      <Code>dataSets</Code> - 数据集数据 (Map&lt;String, List&lt;Map&lt;String, Object>>>)<br />
      <Code>currentRow</Code> - 当前行索引 (int)<br />
      <Code>parameters</Code> - 报表参数 (Map)
    </div>
  </Card>
)

export default FunctionManagement
