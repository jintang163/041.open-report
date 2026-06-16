import React, { useEffect, useState, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { Spin, Typography, Divider, Result, Button, Form, Input, Select, DatePicker, Radio, Checkbox, InputNumber, Space, Row, Col, Table, Empty, Card, List, Tag, Pagination } from 'antd'
import { ReloadOutlined, ExportOutlined, PrinterOutlined, FileExcelOutlined, FilePdfOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import {
  getPublicReportInfo,
  executePublicReport,
  exportPublicReportExcel,
  exportPublicReportPdf
} from '@/api/report'
import {
  ReportParam,
  ReportRenderResult,
  initParamValues,
  formatParamValue,
  parseHtmlTable,
  TableData,
  ChartConfig,
  isMobileDevice,
  handlePrint,
  downloadBlob
} from '../preview/utils/report'
import dayjs from 'dayjs'

const { Title } = Typography
const { RangePicker } = DatePicker
const { TextArea } = Input
const { Option } = Select
const { TextArea: InputTextArea } = Input

interface ViewerState {
  reportInfo: { id: number; name: string; params?: ReportParam[] } | null
  paramValues: Record<string, any>
  reportData: ReportRenderResult | null
  loading: boolean
  exporting: boolean
  isMobile: boolean
  error: string | null
}

const ViewerPage: React.FC = () => {
  const { token } = useParams<{ token: string }>()
  const [form] = Form.useForm()
  const [state, setState] = useState<ViewerState>({
    reportInfo: null,
    paramValues: {},
    reportData: null,
    loading: false,
    exporting: false,
    isMobile: false,
    error: null
  })
  const [searchText, setSearchText] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)

  useEffect(() => {
    setState((prev) => ({ ...prev, isMobile: isMobileDevice() }))
  }, [])

  useEffect(() => {
    const init = async () => {
      if (!token) {
        setState((prev) => ({ ...prev, error: '缺少访问凭证' }))
        return
      }
      try {
        setState((prev) => ({ ...prev, loading: true }))
        const info = await getPublicReportInfo(token)
        const params = (info.params || []) as ReportParam[]
        const paramValues = initParamValues(params)
        setState((prev) => ({
          ...prev,
          reportInfo: { ...info, params },
          paramValues
        }))

        const formInitialValues: Record<string, any> = {}
        params.forEach((param) => {
          if (param.type === 'date' && paramValues[param.name]) {
            formInitialValues[param.name] = dayjs(paramValues[param.name])
          } else if (param.type === 'dateRange' && Array.isArray(paramValues[param.name])) {
            formInitialValues[param.name] = paramValues[param.name].map((v: any) => dayjs(v))
          } else {
            formInitialValues[param.name] = paramValues[param.name]
          }
        })
        form.setFieldsValue(formInitialValues)

        const formattedParams = formatParamValue(params, paramValues)
        const result = await executePublicReport(token, formattedParams)
        setState((prev) => ({
          ...prev,
          reportData: result as ReportRenderResult,
          loading: false
        }))
      } catch (err: any) {
        console.error('加载公开报表失败:', err)
        setState((prev) => ({
          ...prev,
          error: err.message || '加载报表失败，请检查链接是否有效',
          loading: false
        }))
      }
    }

    init()
  }, [token, form])

  const handleValuesChange = (_: any, allValues: Record<string, any>) => {
    const params = state.reportInfo?.params || []
    const newValues: Record<string, any> = { ...state.paramValues }
    params.forEach((param) => {
      const value = allValues[param.name]
      if (param.type === 'date') {
        newValues[param.name] = value || null
      } else if (param.type === 'dateRange') {
        newValues[param.name] = value || null
      } else {
        newValues[param.name] = value
      }
    })
    setState((prev) => ({ ...prev, paramValues: newValues }))
  }

  const handleSubmit = async () => {
    try {
      await form.validateFields()
      const params = state.reportInfo?.params || []
      const formattedParams = formatParamValue(params, state.paramValues)
      setState((prev) => ({ ...prev, loading: true }))
      const result = await executePublicReport(token!, formattedParams)
      setState((prev) => ({
        ...prev,
        reportData: result as ReportRenderResult,
        loading: false
      }))
    } catch (error) {
      console.log('表单校验失败:', error)
    }
  }

  const handleReset = () => {
    const params = state.reportInfo?.params || []
    const paramValues = initParamValues(params)
    const initialValues: Record<string, any> = {}
    params.forEach((param) => {
      if (param.type === 'date') {
        initialValues[param.name] = param.defaultValue ? dayjs(param.defaultValue) : null
      } else if (param.type === 'dateRange' && param.defaultValue) {
        initialValues[param.name] = [dayjs(param.defaultValue[0]), dayjs(param.defaultValue[1])]
      } else {
        initialValues[param.name] = param.defaultValue
      }
    })
    form.setFieldsValue(initialValues)
    setState((prev) => ({ ...prev, paramValues }))
  }

  const handleExport = async (type: 'excel' | 'pdf') => {
    if (!token) return
    try {
      setState((prev) => ({ ...prev, exporting: true }))
      const params = state.reportInfo?.params || []
      const formattedParams = formatParamValue(params, state.paramValues)
      let blob: Blob
      let filename: string
      const name = state.reportInfo?.name || '报表'
      const timestamp = dayjs().format('YYYYMMDDHHmmss')
      if (type === 'excel') {
        blob = await exportPublicReportExcel(token, formattedParams)
        filename = `${name}_${timestamp}.xlsx`
      } else {
        blob = await exportPublicReportPdf(token, formattedParams)
        filename = `${name}_${timestamp}.pdf`
      }
      downloadBlob(blob, filename)
    } catch (error) {
      console.error(`导出${type.toUpperCase()}失败:`, error)
    } finally {
      setState((prev) => ({ ...prev, exporting: false }))
    }
  }

  const renderParamItem = (param: ReportParam) => {
    const { type, options = [], placeholder, disabled } = param
    const commonProps = { disabled, style: { width: '100%' } }

    switch (type) {
      case 'input':
        return <Input placeholder={placeholder || `请输入${param.label}`} {...commonProps} allowClear />
      case 'textarea':
        return <TextArea placeholder={placeholder || `请输入${param.label}`} rows={3} {...commonProps} allowClear />
      case 'number':
        return <InputNumber placeholder={placeholder || `请输入${param.label}`} {...commonProps} />
      case 'select':
        return (
          <Select placeholder={placeholder || `请选择${param.label}`} allowClear {...commonProps}>
            {options.map((opt) => (
              <Option key={String(opt.value)} value={opt.value}>
                {opt.label}
              </Option>
            ))}
          </Select>
        )
      case 'date':
        return <DatePicker placeholder={placeholder || `请选择${param.label}`} {...commonProps} />
      case 'dateRange':
        return <RangePicker style={{ width: '100%' }} disabled={disabled} />
      case 'radio':
        return (
          <Radio.Group {...commonProps}>
            {options.map((opt) => (
              <Radio key={String(opt.value)} value={opt.value}>
                {opt.label}
              </Radio>
            ))}
          </Radio.Group>
        )
      case 'checkbox':
        return (
          <Checkbox.Group {...commonProps}>
            {options.map((opt) => (
              <Checkbox key={String(opt.value)} value={opt.value}>
                {opt.label}
              </Checkbox>
            ))}
          </Checkbox.Group>
        )
      default:
        return <Input placeholder={placeholder || `请输入${param.label}`} {...commonProps} allowClear />
    }
  }

  const tableData: TableData | undefined = useMemo(() => {
    if (state.reportData?.table) return state.reportData.table
    if (state.reportData?.html) {
      const parsed = parseHtmlTable(state.reportData.html)
      if (parsed) return parsed
    }
    return undefined
  }, [state.reportData])

  const filteredTableData = useMemo(() => {
    if (!tableData?.dataSource || !searchText) return tableData?.dataSource || []
    const lowerSearch = searchText.toLowerCase()
    return tableData.dataSource.filter((record) =>
      Object.values(record).some((value) =>
        String(value).toLowerCase().includes(lowerSearch)
      )
    )
  }, [tableData, searchText])

  const paginatedData = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize
    return filteredTableData.slice(startIndex, startIndex + pageSize)
  }, [filteredTableData, currentPage, pageSize])

  if (state.error) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', padding: 24 }}>
        <Result status="warning" title="无法访问" subTitle={state.error} />
      </div>
    )
  }

  const renderChart = (chart: ChartConfig) => {
    return (
      <Card
        key={chart.id}
        title={chart.title}
        style={{ borderRadius: 8, marginBottom: 16 }}
        bodyStyle={{ padding: 12 }}
      >
        <ReactECharts
          option={chart.option || {}}
          style={{ height: chart.height || 300, width: chart.width || '100%' }}
          notMerge
          lazyUpdate
          opts={{ renderer: 'canvas' }}
        />
      </Card>
    )
  }

  const renderMobileCard = (record: any, index: number) => {
    if (!tableData?.columns) return null
    const columns = tableData.columns

    return (
      <Card
        key={index}
        size="small"
        style={{ marginBottom: 12, borderRadius: 8 }}
        bodyStyle={{ padding: 12 }}
      >
        <List
          size="small"
          dataSource={columns}
          renderItem={(col, colIndex) => {
            const value = record[col.dataIndex]
            return (
              <List.Item
                key={col.dataIndex}
                style={{
                  padding: '6px 0',
                  borderBottom: colIndex < columns.length - 1 ? '1px solid #f0f0f0' : 'none'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', width: '100%', gap: 8 }}>
                  <span style={{ color: '#666', flexShrink: 0, fontWeight: 500 }}>
                    {col.title}
                  </span>
                  <span style={{ color: '#333', textAlign: 'right', wordBreak: 'break-all' }}>
                    {value !== undefined && value !== null && value !== '' ? (
                      String(value)
                    ) : (
                      <Tag color="default">--</Tag>
                    )}
                  </span>
                </div>
              </List.Item>
            )
          }}
        />
      </Card>
    )
  }

  return (
    <div style={{ background: '#f5f5f5', minHeight: '100vh', padding: state.isMobile ? 8 : 16 }}>
      <div style={{ maxWidth: state.isMobile ? '100%' : 1400, margin: '0 auto' }}>
        <Spin spinning={state.loading} tip="加载中...">
          <div style={{ background: '#fff', borderRadius: 8, padding: state.isMobile ? 12 : '20px 24px', marginBottom: 16 }}>
            <Title level={state.isMobile ? 5 : 4} style={{ margin: 0, textAlign: 'center' }}>
              {state.reportInfo?.name || '公开报表'}
            </Title>
          </div>

          {state.reportInfo?.params && state.reportInfo.params.length > 0 && (
            <div style={{ background: '#fff', borderRadius: 8, padding: 16, marginBottom: 16 }}>
              <div style={{ fontWeight: 600, marginBottom: 12 }}>查询参数</div>
              <Form
                form={form}
                layout="vertical"
                onValuesChange={handleValuesChange}
              >
                <Row gutter={16}>
                  {state.reportInfo.params.map((param) => (
                    <Col xs={24} sm={12} md={8} lg={6} key={param.name}>
                      <Form.Item
                        label={param.label}
                        name={param.name}
                        rules={param.required ? [{ required: true, message: `请${param.type === 'select' || param.type === 'radio' ? '选择' : '输入'}${param.label}` }] : []}
                      >
                        {renderParamItem(param)}
                      </Form.Item>
                    </Col>
                  ))}
                </Row>
                <Row justify="end">
                  <Space>
                    <Button icon={<ReloadOutlined />} onClick={handleReset}>
                      重置
                    </Button>
                    <Button type="primary" onClick={handleSubmit} loading={state.loading}>
                      查询
                    </Button>
                  </Space>
                </Row>
              </Form>
            </div>
          )}

          <div style={{ background: '#fff', borderRadius: 8, padding: '12px 16px', marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
            <Space>
              <Button
                icon={<FileExcelOutlined />}
                onClick={() => handleExport('excel')}
                loading={state.exporting}
              >
                导出 Excel
              </Button>
              <Button
                icon={<FilePdfOutlined />}
                onClick={() => handleExport('pdf')}
                loading={state.exporting}
              >
                导出 PDF
              </Button>
              <Button icon={<PrinterOutlined />} onClick={() => handlePrint('report-viewer-content')}>
                打印
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleSubmit} loading={state.loading}>
                刷新
              </Button>
            </Space>
          </div>

          <div id="report-viewer-content">
            {state.reportData?.title && (
              <>
                <div style={{ background: '#fff', borderRadius: 8, padding: '16px 24px', marginBottom: 16 }}>
                  <Title level={3} style={{ margin: 0, textAlign: 'center' }}>
                    {state.reportData.title}
                  </Title>
                  {state.reportData.summary && (
                    <div style={{ textAlign: 'center', color: '#666', marginTop: 8 }}>
                      {state.reportData.summary}
                    </div>
                  )}
                </div>
                <Divider style={{ margin: '0 0 16px 0' }} />
              </>
            )}

            {state.reportData?.charts && state.reportData.charts.length > 0 && (
              <Row gutter={[16, 16]}>
                {state.reportData.charts.map((chart: ChartConfig) => (
                  <Col
                    xs={24}
                    sm={24}
                    md={state.reportData!.charts!.length > 1 ? 12 : 24}
                    lg={state.reportData!.charts!.length > 2 ? 8 : state.reportData!.charts!.length > 1 ? 12 : 24}
                    key={chart.id}
                  >
                    {renderChart(chart)}
                  </Col>
                ))}
              </Row>
            )}

            {state.isMobile ? (
              <>
                {tableData && tableData.columns && tableData.columns.length > 0 && (
                  <Card style={{ borderRadius: 8 }} bodyStyle={{ padding: 12 }}>
                    <Input.Search
                      placeholder="搜索内容..."
                      allowClear
                      onChange={(e) => {
                        setSearchText(e.target.value)
                        setCurrentPage(1)
                      }}
                      style={{ marginBottom: 12 }}
                    />
                    {paginatedData.length > 0 ? (
                      <>
                        {paginatedData.map((record, index) => renderMobileCard(record, (currentPage - 1) * pageSize + index))}
                        <div style={{ display: 'flex', justifyContent: 'center', marginTop: 16 }}>
                          <Pagination
                            current={currentPage}
                            pageSize={pageSize}
                            total={filteredTableData.length}
                            showSizeChanger={false}
                            showTotal={(total) => `共 ${total} 条`}
                            onChange={(page, size) => {
                              setCurrentPage(page)
                              setPageSize(size)
                            }}
                          />
                        </div>
                      </>
                    ) : (
                      <Empty description="暂无数据" style={{ padding: 20 }} />
                    )}
                  </Card>
                )}
              </>
            ) : (
              <>
                {state.reportData?.html ? (
                  <div
                    style={{
                      background: '#fff',
                      borderRadius: 8,
                      padding: 16,
                      marginBottom: 16,
                      overflow: 'auto'
                    }}
                    dangerouslySetInnerHTML={{ __html: state.reportData.html }}
                  />
                ) : tableData && tableData.columns && tableData.columns.length > 0 ? (
                  <div style={{ background: '#fff', borderRadius: 8, padding: 16 }}>
                    <Input.Search
                      placeholder="搜索内容..."
                      allowClear
                      onChange={(e) => {
                        setSearchText(e.target.value)
                        setCurrentPage(1)
                      }}
                      style={{ marginBottom: 16, maxWidth: 300 }}
                    />
                    <Table
                      columns={tableData.columns.map((col) => ({
                        title: <span style={{ whiteSpace: 'nowrap' }}>{col.title}</span>,
                        dataIndex: col.dataIndex,
                        key: col.key || col.dataIndex,
                        width: col.width,
                        align: col.align,
                        sorter: col.sorter !== false ? true : false
                      }))}
                      dataSource={filteredTableData}
                      rowKey={(_, index) => String(index)}
                      pagination={{
                        current: currentPage,
                        pageSize,
                        total: filteredTableData.length,
                        showSizeChanger: true,
                        showQuickJumper: true,
                        showTotal: (total) => `共 ${total} 条记录`,
                        onChange: (page, size) => {
                          setCurrentPage(page)
                          setPageSize(size)
                        }
                      }}
                      scroll={{ x: 'max-content' }}
                      size="middle"
                      bordered
                    />
                  </div>
                ) : (
                  state.reportData && (
                    <div
                      style={{
                        background: '#fff',
                        borderRadius: 8,
                        padding: 40,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}
                    >
                      <Empty description="暂无数据，请先执行查询" />
                    </div>
                  )
                )}
              </>
            )}
          </div>
        </Spin>
      </div>
    </div>
  )
}

export default ViewerPage
