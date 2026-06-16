import React, { useEffect } from 'react'
import { Form, Input, Select, DatePicker, Radio, Checkbox, InputNumber, Button, Space, Row, Col, Collapse } from 'antd'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { usePreviewStore } from '../../store/preview'
import { ReportParam } from '../../utils/report'
import dayjs from 'dayjs'

const { RangePicker } = DatePicker
const { TextArea } = Input

interface ParamPanelProps {
  collapsible?: boolean
}

const renderParamItem = (param: ReportParam, form: any) => {
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
            <Select.Option key={String(opt.value)} value={opt.value}>
              {opt.label}
            </Select.Option>
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

const ParamPanel: React.FC<ParamPanelProps> = ({ collapsible = true }) => {
  const [form] = Form.useForm()
  const params = usePreviewStore((state) => state.params)
  const paramValues = usePreviewStore((state) => state.paramValues)
  const updateParamValue = usePreviewStore((state) => state.updateParamValue)
  const resetParams = usePreviewStore((state) => state.resetParams)
  const executeReport = usePreviewStore((state) => state.executeReport)
  const loading = usePreviewStore((state) => state.loading)

  useEffect(() => {
    const initialValues: Record<string, any> = {}
    params.forEach((param) => {
      const value = paramValues[param.name]
      if (param.type === 'date' && value) {
        initialValues[param.name] = dayjs(value)
      } else if (param.type === 'dateRange' && Array.isArray(value) && value.length === 2) {
        initialValues[param.name] = [dayjs(value[0]), dayjs(value[1])]
      } else {
        initialValues[param.name] = value
      }
    })
    form.setFieldsValue(initialValues)
  }, [params, form])

  const handleValuesChange = (_: any, allValues: Record<string, any>) => {
    params.forEach((param) => {
      const value = allValues[param.name]
      if (param.type === 'date') {
        updateParamValue(param.name, value || null)
      } else if (param.type === 'dateRange') {
        updateParamValue(param.name, value || null)
      } else {
        updateParamValue(param.name, value)
      }
    })
  }

  const handleSubmit = async () => {
    try {
      await form.validateFields()
      await executeReport()
    } catch (error) {
      console.log('表单校验失败:', error)
    }
  }

  const handleReset = () => {
    resetParams()
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
  }

  const formContent = (
    <Form
      form={form}
      layout="vertical"
      onValuesChange={handleValuesChange}
      initialValues={paramValues}
      style={{ padding: '8px 4px 0' }}
    >
      <Row gutter={16}>
        {params.map((param) => (
          <Col xs={24} sm={12} md={8} lg={6} xl={6} key={param.name}>
            <Form.Item
              label={param.label}
              name={param.name}
              rules={param.required ? [{ required: true, message: `请${param.type === 'select' || param.type === 'radio' ? '选择' : '输入'}${param.label}` }] : []}
            >
              {renderParamItem(param, form)}
            </Form.Item>
          </Col>
        ))}
      </Row>
      <Row justify="end" style={{ marginBottom: 8 }}>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            重置
          </Button>
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSubmit} loading={loading}>
            查询
          </Button>
        </Space>
      </Row>
    </Form>
  )

  if (!collapsible) {
    return <div style={{ background: '#fff', borderRadius: 8, marginBottom: 16 }}>{formContent}</div>
  }

  return (
    <Collapse
      defaultActiveKey={['params']}
      style={{ background: '#fff', borderRadius: 8, marginBottom: 16 }}
      items={[
        {
          key: 'params',
          label: <span style={{ fontWeight: 600 }}>查询参数</span>,
          children: formContent
        }
      ]}
    />
  )
}

export default ParamPanel
