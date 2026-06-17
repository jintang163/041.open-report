import { useState, useEffect } from 'react'
import {
  Modal,
  Form,
  Select,
  Input,
  InputNumber,
  Button,
  Tabs,
  Card,
  Row,
  Col,
  Space,
  message,
  Spin,
  Divider
} from 'antd'
import ReactECharts from 'echarts-for-react'
import { useDesignerStore, type ChartConfig, type DataSetWithFields } from '../../store/designer'
import { getDatasetPreview } from '@/api/dataset'
import type { EChartsOption } from 'echarts'

const CHART_TYPES = [
  { value: 'bar', label: '柱状图', icon: '📊' },
  { value: 'line', label: '折线图', icon: '📈' },
  { value: 'pie', label: '饼图', icon: '🥧' },
  { value: 'area', label: '面积图', icon: '📉' },
  { value: 'scatter', label: '散点图', icon: '⚪' },
  { value: 'radar', label: '雷达图', icon: '🕸️' }
]

const buildPreviewOption = (
  type: string,
  title: string,
  data: Record<string, any>[],
  xField: string,
  yFields: string[]
): EChartsOption => {
  if (type === 'pie') {
    return {
      title: { text: title || '饼图', left: 'center' },
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: '60%',
        itemStyle: { borderRadius: 4 },
        data: data.map((d: any) => ({
          name: d[xField],
          value: d[yFields?.[0] || '']
        }))
      }]
    }
  }

  if (type === 'radar') {
    const indicators = data.map((d: any) => ({
      name: d[xField],
      max: Math.max(...data.map((r: any) => Number(r[yFields?.[0] || '']) || 0), 10) * 1.2
    }))
    return {
      title: { text: title || '雷达图', left: 'center' },
      tooltip: {},
      radar: { indicator: indicators },
      series: [{
        type: 'radar',
        data: (yFields || []).map((yf: string) => ({
          value: data.map((d: any) => d[yf]),
          name: yf
        }))
      }]
    }
  }

  const xAxisData = data.map((d: any) => d[xField])
  const series = (yFields || []).map((yf: string) => ({
    name: yf,
    type: type === 'area' ? 'line' : type,
    areaStyle: type === 'area' ? {} : undefined,
    smooth: type === 'area' || type === 'line',
    data: data.map((d: any) => d[yf])
  }))

  return {
    title: { text: title || '图表', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { top: 50, bottom: 40, left: 50, right: 30 },
    xAxis: { type: 'category', data: xAxisData },
    yAxis: { type: 'value' },
    series
  }
}

const ChartConfigModal: React.FC = () => {
  const {
    chartConfigVisible,
    editingChart,
    dataSources,
    selectedRange,
    charts,
    addChart,
    updateChart,
    setChartConfigVisible
  } = useDesignerStore()

  const [form] = Form.useForm()
  const [formValues, setFormValues] = useState<any>({})
  const [previewData, setPreviewData] = useState<Record<string, any>[]>([])
  const [previewLoading, setPreviewLoading] = useState(false)

  useEffect(() => {
    if (chartConfigVisible) {
      if (editingChart) {
        form.setFieldsValue({
          type: editingChart.type,
          title: editingChart.title,
          datasetId: editingChart.datasetId,
          xAxisField: editingChart.xAxisField,
          yAxisFields: editingChart.yAxisFields,
          seriesName: editingChart.seriesName,
          width: editingChart.width || 400,
          height: editingChart.height || 300,
          x: editingChart.x,
          y: editingChart.y,
          linkageField: editingChart.linkageField,
          linkageTargetId: editingChart.linkageTargetId
        })
      } else {
        const offset = charts.length * 20
        form.setFieldsValue({
          type: 'bar',
          title: '',
          width: 400,
          height: 300,
          x: 150 + offset,
          y: 100 + offset,
          linkageField: undefined,
          linkageTargetId: undefined
        })
      }
    }
  }, [chartConfigVisible, editingChart, form, charts.length])

  useEffect(() => {
    const values = form.getFieldsValue()
    setFormValues(values)
  }, [form])

  useEffect(() => {
    if (formValues.datasetId && formValues.xAxisField && formValues.yAxisFields?.length > 0) {
      loadPreviewData()
    } else {
      setPreviewData([])
    }
  }, [formValues.datasetId, formValues.xAxisField, formValues.yAxisFields])

  const loadPreviewData = async () => {
    if (!formValues.datasetId) return
    setPreviewLoading(true)
    try {
      const res = await getDatasetPreview(formValues.datasetId, {}, 100)
      setPreviewData(res.rows || [])
    } catch {
      setPreviewData([])
    } finally {
      setPreviewLoading(false)
    }
  }

  const getAllDatasets = (): DataSetWithFields[] => {
    const datasets: DataSetWithFields[] = []
    dataSources.forEach((ds) => {
      if (ds.dataSets) {
        datasets.push(...ds.dataSets)
      }
    })
    return datasets
  }

  const getDatasetById = (id: number | undefined): DataSetWithFields | undefined => {
    if (!id) return undefined
    return getAllDatasets().find((d) => d.id === id)
  }

  const handleValuesChange = (_: any, allValues: any) => {
    setFormValues(allValues)
  }

  const getChartOption = (): EChartsOption => {
    const { type, title, xAxisField, yAxisFields, seriesName } = formValues
    return buildPreviewOption(type, title, previewData, xAxisField, yAxisFields || [])
  }

  const handleOk = async () => {
    try {
      const values = await form.validateFields()

      const dataset = getDatasetById(values.datasetId)

      const chart: ChartConfig = {
        id: editingChart?.id || Date.now().toString(),
        type: values.type,
        title: values.title,
        datasetId: values.datasetId,
        datasetName: dataset?.name,
        xAxisField: values.xAxisField,
        yAxisFields: values.yAxisFields,
        seriesName: values.seriesName,
        width: values.width,
        height: values.height,
        x: values.x,
        y: values.y,
        linkageField: values.linkageField,
        linkageTargetId: values.linkageTargetId,
        position: selectedRange
          ? {
              startRow: selectedRange.start.row,
              startCol: selectedRange.start.col,
              endRow: selectedRange.end.row,
              endCol: selectedRange.end.col
            }
          : editingChart?.position
      }

      if (editingChart) {
        updateChart(chart)
        message.success('图表更新成功')
      } else {
        addChart(chart)
        message.success('图表添加成功')
      }

      setChartConfigVisible(false)
    } catch {
    }
  }

  const handleCancel = () => {
    setChartConfigVisible(false)
  }

  return (
    <Modal
      title={editingChart ? '编辑图表' : '新建图表'}
      open={chartConfigVisible}
      onOk={handleOk}
      onCancel={handleCancel}
      width={900}
      okText="确定"
      cancelText="取消"
    >
      <Form
        form={form}
        layout="vertical"
        onValuesChange={handleValuesChange}
      >
        <Tabs
          items={[
            {
              key: 'config',
              label: '配置',
              children: (
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item
                      label="图表类型"
                      name="type"
                      rules={[{ required: true, message: '请选择图表类型' }]}
                    >
                      <Select>
                        {CHART_TYPES.map((ct) => (
                          <Select.Option key={ct.value} value={ct.value}>
                            {ct.icon} {ct.label}
                          </Select.Option>
                        ))}
                      </Select>
                    </Form.Item>

                    <Form.Item label="图表标题" name="title">
                      <Input placeholder="请输入图表标题" />
                    </Form.Item>

                    <Form.Item
                      label="数据集"
                      name="datasetId"
                      rules={[{ required: true, message: '请选择数据集' }]}
                    >
                      <Select placeholder="请选择数据集">
                        {getAllDatasets().map((ds) => (
                          <Select.Option key={ds.id} value={ds.id}>
                            {ds.name}
                          </Select.Option>
                        ))}
                      </Select>
                    </Form.Item>

                    <Form.Item noStyle shouldUpdate={(prev, cur) => prev.datasetId !== cur.datasetId}>
                      {({ getFieldValue }) => {
                        const dataset = getDatasetById(getFieldValue('datasetId'))
                        const fields = dataset?.fields || []
                        const isPieOrRadar = ['pie', 'radar'].includes(getFieldValue('type'))

                        return (
                          <>
                            <Form.Item
                              label={isPieOrRadar ? '名称字段' : 'X轴字段'}
                              name="xAxisField"
                              rules={[{ required: true, message: '请选择X轴字段' }]}
                            >
                              <Select placeholder="请选择字段">
                                {fields.map((f) => (
                                  <Select.Option key={f.name} value={f.name}>
                                    {f.label || f.name} ({f.type})
                                  </Select.Option>
                                ))}
                              </Select>
                            </Form.Item>

                            <Form.Item
                              label={isPieOrRadar ? '数值字段' : 'Y轴字段'}
                              name="yAxisFields"
                              rules={[{ required: true, message: '请选择Y轴字段' }]}
                            >
                              <Select
                                mode={isPieOrRadar ? undefined : 'multiple'}
                                placeholder="请选择字段"
                              >
                                {fields.map((f) => (
                                  <Select.Option key={f.name} value={f.name}>
                                    {f.label || f.name} ({f.type})
                                  </Select.Option>
                                ))}
                              </Select>
                            </Form.Item>
                          </>
                        )
                      }}
                    </Form.Item>
                  </Col>

                  <Col span={12}>
                    <Form.Item label="系列名称" name="seriesName">
                      <Input placeholder="请输入系列名称（可选）" />
                    </Form.Item>

                    <Space>
                      <Form.Item label="宽度" name="width" rules={[{ required: true }]}>
                        <InputNumber min={100} max={2000} step={50} />
                      </Form.Item>
                      <Form.Item label="高度" name="height" rules={[{ required: true }]}>
                        <InputNumber min={100} max={1500} step={50} />
                      </Form.Item>
                    </Space>

                    <Space>
                      <Form.Item label="X坐标" name="x">
                        <InputNumber min={0} max={3000} step={10} />
                      </Form.Item>
                      <Form.Item label="Y坐标" name="y">
                        <InputNumber min={0} max={2000} step={10} />
                      </Form.Item>
                    </Space>

                    <Divider style={{ margin: '8px 0' }} />

                    <div style={{ fontWeight: 500, marginBottom: 8 }}>图表联动</div>

                    <Form.Item label="联动字段" name="linkageField" extra="点击此图表时传递的筛选字段">
                      <Input placeholder="如：category（可选）" />
                    </Form.Item>

                    <Form.Item label="联动目标图表" name="linkageTargetId" extra="点击此图表时，目标图表将按联动字段筛选">
                      <Select placeholder="请选择联动目标" allowClear>
                        {charts.filter(c => c.id !== editingChart?.id).map(c => (
                          <Select.Option key={c.id} value={c.id}>
                            {c.title || `图表-${c.id}`} ({c.type})
                          </Select.Option>
                        ))}
                      </Select>
                    </Form.Item>
                  </Col>
                </Row>
              )
            },
            {
              key: 'preview',
              label: '预览',
              children: (
                <Card style={{ height: 360, position: 'relative' }}>
                  {previewLoading && (
                    <div style={{
                      position: 'absolute',
                      top: 0,
                      left: 0,
                      right: 0,
                      bottom: 0,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      background: 'rgba(255,255,255,0.7)',
                      zIndex: 10
                    }}>
                      <Spin tip="加载数据..." />
                    </div>
                  )}
                  <ReactECharts
                    option={getChartOption()}
                    style={{ height: 320, width: '100%' }}
                    notMerge
                    lazyUpdate
                  />
                </Card>
              )
            }
          ]}
        />
      </Form>
    </Modal>
  )
}

export default ChartConfigModal
