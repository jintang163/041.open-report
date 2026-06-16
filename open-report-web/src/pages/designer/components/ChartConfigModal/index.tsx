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
  message
} from 'antd'
import ReactECharts from 'echarts-for-react'
import { useDesignerStore, type ChartConfig, type DataSetWithFields } from '../../store/designer'
import type { EChartsOption } from 'echarts'

const CHART_TYPES = [
  { value: 'bar', label: '柱状图', icon: '📊' },
  { value: 'line', label: '折线图', icon: '📈' },
  { value: 'pie', label: '饼图', icon: '🥧' },
  { value: 'area', label: '面积图', icon: '📉' },
  { value: 'scatter', label: '散点图', icon: '⚪' },
  { value: 'radar', label: '雷达图', icon: '🕸️' }
]

const generateSampleData = (dataset: DataSetWithFields | undefined, xField: string | undefined, yFields: string[] | undefined) => {
  const sampleData: Record<string, any>[] = []
  const categories = ['一月', '二月', '三月', '四月', '五月', '六月']

  if (xField && yFields && yFields.length > 0) {
    categories.forEach((cat, idx) => {
      const item: Record<string, any> = {}
      item[xField] = cat
      yFields.forEach((yf) => {
        item[yf] = Math.floor(Math.random() * 100) + 20
      })
      sampleData.push(item)
    })
  }

  return sampleData
}

const ChartConfigModal: React.FC = () => {
  const {
    chartConfigVisible,
    editingChart,
    dataSources,
    selectedRange,
    addChart,
    updateChart,
    setChartConfigVisible
  } = useDesignerStore()

  const [form] = Form.useForm()
  const [formValues, setFormValues] = useState<any>({})

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
          height: editingChart.height || 300
        })
      } else {
        form.setFieldsValue({
          type: 'bar',
          title: '',
          width: 400,
          height: 300
        })
      }
    }
  }, [chartConfigVisible, editingChart, form])

  useEffect(() => {
    const values = form.getFieldsValue()
    setFormValues(values)
  }, [form])

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
    const { type, title, datasetId, xAxisField, yAxisFields, seriesName } = formValues
    const dataset = getDatasetById(datasetId)
    const fields = dataset?.fields || []

    const sampleData = generateSampleData(dataset, xAxisField, yAxisFields)

    if (type === 'pie') {
      const xFieldName = xAxisField || (fields[0]?.name ?? '')
      const yFieldName = yAxisFields?.[0] || (fields[1]?.name ?? '')

      return {
        title: { text: title || '饼图', left: 'center' },
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        series: [
          {
            type: 'pie',
            radius: '60%',
            data: sampleData.map((d) => ({
              name: d[xFieldName],
              value: d[yFieldName]
            }))
          }
        ]
      }
    }

    if (type === 'radar') {
      const indicators = (yAxisFields || []).map((f: string) => ({ name: f, max: 150 }))
      return {
        title: { text: title || '雷达图', left: 'center' },
        tooltip: {},
        radar: { indicator: indicators },
        series: [
          {
            type: 'radar',
            data: [
              {
                value: (yAxisFields || []).map(() => Math.floor(Math.random() * 100) + 30),
                name: seriesName || '数据'
              }
            ]
          }
        ]
      }
    }

    const xAxisData = sampleData.map((d) => d[xAxisField || ''])
    const series = (yAxisFields || []).map((yf: string) => ({
      name: yf,
      type: type === 'area' ? 'line' : type,
      areaStyle: type === 'area' ? {} : undefined,
      data: sampleData.map((d) => d[yf])
    }))

    return {
      title: { text: title || (CHART_TYPES.find((c) => c.value === type)?.label || '图表'), left: 'center' },
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0 },
      grid: { top: 50, bottom: 40, left: 50, right: 30 },
      xAxis: { type: 'category', data: xAxisData },
      yAxis: { type: 'value' },
      series
    }
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
                  </Col>
                </Row>
              )
            },
            {
              key: 'preview',
              label: '预览',
              children: (
                <Card style={{ height: 360 }}>
                  <ReactECharts
                    option={getChartOption()}
                    style={{ height: 320, width: '100%' }}
                    notMerge
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
