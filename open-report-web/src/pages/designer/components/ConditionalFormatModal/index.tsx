import { useState, useEffect } from 'react'
import {
  Modal,
  Form,
  Select,
  Input,
  InputNumber,
  ColorPicker,
  Button,
  List,
  Space,
  Divider,
  Radio,
  message
} from 'antd'
import {
  DeleteOutlined,
  PlusOutlined,
  EditOutlined
} from '@ant-design/icons'
import { useDesignerStore, type ConditionalFormatRule, type CellStyle } from '../../store/designer'
import { cellPositionToRef } from '../../utils/luckysheet'

const ConditionalFormatModal: React.FC = () => {
  const {
    conditionalFormatVisible,
    conditionalFormats,
    selectedCell,
    selectedRange,
    setConditionalFormats,
    setConditionalFormatVisible
  } = useDesignerStore()

  const [editingRule, setEditingRule] = useState<ConditionalFormatRule | null>(null)
  const [isEditing, setIsEditing] = useState(false)
  const [form] = Form.useForm()

  useEffect(() => {
    if (conditionalFormatVisible) {
      setEditingRule(null)
      setIsEditing(false)
      form.resetFields()
    }
  }, [conditionalFormatVisible, form])

  const getCurrentRange = (): string => {
    if (selectedRange) {
      const startRef = cellPositionToRef(selectedRange.start.row, selectedRange.start.col)
      const endRef = cellPositionToRef(selectedRange.end.row, selectedRange.end.col)
      return `${startRef}:${endRef}`
    }
    if (selectedCell) {
      return cellPositionToRef(selectedCell.row, selectedCell.col)
    }
    return ''
  }

  const handleAddNew = () => {
    setEditingRule({
      id: Date.now().toString(),
      type: 'cellValue',
      operator: 'greaterThan',
      value1: '',
      value2: '',
      style: {
        color: '#000000',
        backgroundColor: '#ffffff'
      },
      range: getCurrentRange()
    })
    setIsEditing(true)
    form.setFieldsValue({
      type: 'cellValue',
      operator: 'greaterThan',
      value1: '',
      value2: '',
      range: getCurrentRange(),
      textColor: '#000000',
      bgColor: '#ffffff',
      bold: false,
      italic: false
    })
  }

  const handleEdit = (rule: ConditionalFormatRule) => {
    setEditingRule(rule)
    setIsEditing(true)
    form.setFieldsValue({
      type: rule.type,
      operator: rule.operator,
      value1: rule.value1,
      value2: rule.value2,
      expression: rule.expression,
      range: rule.range,
      textColor: rule.style?.color || '#000000',
      bgColor: rule.style?.backgroundColor || '#ffffff',
      bold: rule.style?.fontWeight === 'bold',
      italic: rule.style?.fontStyle === 'italic'
    })
  }

  const handleDelete = (ruleId: string) => {
    setConditionalFormats(conditionalFormats.filter((r) => r.id !== ruleId))
    message.success('删除成功')
  }

  const handleSaveRule = async () => {
    try {
      const values = await form.validateFields()

      const style: Partial<CellStyle> = {
        color: values.textColor,
        backgroundColor: values.bgColor,
        fontWeight: values.bold ? 'bold' : 'normal',
        fontStyle: values.italic ? 'italic' : 'normal'
      }

      const rule: ConditionalFormatRule = {
        id: editingRule?.id || Date.now().toString(),
        type: values.type,
        operator: values.operator,
        value1: values.value1,
        value2: values.value2,
        expression: values.expression,
        style,
        range: values.range
      }

      if (editingRule) {
        setConditionalFormats(
          conditionalFormats.map((r) => (r.id === editingRule.id ? rule : r))
        )
      } else {
        setConditionalFormats([...conditionalFormats, rule])
      }

      setIsEditing(false)
      setEditingRule(null)
      form.resetFields()
      message.success('保存成功')
    } catch {
    }
  }

  const handleCancelEdit = () => {
    setIsEditing(false)
    setEditingRule(null)
    form.resetFields()
  }

  const handleClose = () => {
    setConditionalFormatVisible(false)
  }

  const getRuleDisplayText = (rule: ConditionalFormatRule): string => {
    const opMap: Record<string, string> = {
      equal: '等于',
      notEqual: '不等于',
      greaterThan: '大于',
      lessThan: '小于',
      between: '介于',
      contains: '包含'
    }

    switch (rule.type) {
      case 'cellValue':
        if (rule.operator === 'between') {
          return `单元格值 ${opMap[rule.operator!]} ${rule.value1} 和 ${rule.value2}`
        }
        return `单元格值 ${opMap[rule.operator!]} ${rule.value1}`
      case 'expression':
        return `表达式: ${rule.expression}`
      case 'dataBar':
        return '数据条'
      case 'colorScale':
        return '色阶'
      case 'iconSet':
        return '图标集'
      default:
        return '条件格式'
    }
  }

  const getValueFields = (type: string, operator: string) => {
    if (type === 'expression') {
      return (
        <Form.Item label="表达式" name="expression" rules={[{ required: true, message: '请输入表达式' }]}>
          <Input placeholder="例如: A1 > 100" />
        </Form.Item>
      )
    }

    if (type === 'cellValue') {
      return (
        <>
          <Form.Item label="比较方式" name="operator" rules={[{ required: true, message: '请选择比较方式' }]}>
            <Select
              options={[
                { value: 'equal', label: '等于' },
                { value: 'notEqual', label: '不等于' },
                { value: 'greaterThan', label: '大于' },
                { value: 'lessThan', label: '小于' },
                { value: 'between', label: '介于' },
                { value: 'contains', label: '包含' }
              ]}
            />
          </Form.Item>
          <Form.Item label="值1" name="value1" rules={[{ required: true, message: '请输入值' }]}>
            <Input />
          </Form.Item>
          {operator === 'between' && (
            <Form.Item label="值2" name="value2" rules={[{ required: true, message: '请输入值' }]}>
              <Input />
            </Form.Item>
          )}
        </>
      )
    }

    return null
  }

  return (
    <Modal
      title="条件格式"
      open={conditionalFormatVisible}
      onCancel={handleClose}
      footer={[
        <Button key="close" onClick={handleClose}>
          关闭
        </Button>
      ]}
      width={700}
    >
      {!isEditing ? (
        <div>
          <div style={{ marginBottom: 12 }}>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAddNew}>
              新建规则
            </Button>
          </div>

          <List
            bordered
            dataSource={conditionalFormats}
            locale={{ emptyText: '暂无条件格式规则' }}
            renderItem={(item) => (
              <List.Item
                actions={[
                  <Button
                    key="edit"
                    type="text"
                    icon={<EditOutlined />}
                    onClick={() => handleEdit(item)}
                  >
                    编辑
                  </Button>,
                  <Button
                    key="delete"
                    type="text"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={() => handleDelete(item.id)}
                  >
                    删除
                  </Button>
                ]}
              >
                <List.Item.Meta
                  title={getRuleDisplayText(item)}
                  description={
                    <Space>
                      <span>应用范围: {item.range}</span>
                      {item.style?.color && (
                        <span>
                          文字颜色:
                          <span
                            style={{
                              display: 'inline-block',
                              width: 14,
                              height: 14,
                              background: item.style.color,
                              border: '1px solid #d9d9d9',
                              verticalAlign: 'middle',
                              marginLeft: 4
                            }}
                          />
                        </span>
                      )}
                      {item.style?.backgroundColor && (
                        <span>
                          背景色:
                          <span
                            style={{
                              display: 'inline-block',
                              width: 14,
                              height: 14,
                              background: item.style.backgroundColor,
                              border: '1px solid #d9d9d9',
                              verticalAlign: 'middle',
                              marginLeft: 4
                            }}
                          />
                        </span>
                      )}
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        </div>
      ) : (
        <Form form={form} layout="vertical">
          <Form.Item label="规则类型" name="type" rules={[{ required: true, message: '请选择规则类型' }]}>
            <Radio.Group>
              <Radio value="cellValue">单元格值</Radio>
              <Radio value="expression">公式</Radio>
              <Radio value="dataBar">数据条</Radio>
              <Radio value="colorScale">色阶</Radio>
              <Radio value="iconSet">图标集</Radio>
            </Radio.Group>
          </Form.Item>

          <Form.Item shouldUpdate={(prev, cur) => prev.type !== cur.type || prev.operator !== cur.operator}>
            {({ getFieldValue }) => {
              const type = getFieldValue('type')
              const operator = getFieldValue('operator')
              return getValueFields(type, operator)
            }}
          </Form.Item>

          <Form.Item label="应用范围" name="range" rules={[{ required: true, message: '请输入应用范围' }]}>
            <Input placeholder="例如: A1:B10" />
          </Form.Item>

          <Divider>样式设置</Divider>

          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item label="文字颜色" name="textColor">
              <ColorPicker showText />
            </Form.Item>
            <Form.Item label="背景颜色" name="bgColor">
              <ColorPicker showText />
            </Form.Item>
          </div>

          <Space>
            <Form.Item name="bold" valuePropName="checked" noStyle>
              <Button type="text" style={{ fontWeight: 'bold' }}>B</Button>
            </Form.Item>
            <Form.Item name="italic" valuePropName="checked" noStyle>
              <Button type="text" style={{ fontStyle: 'italic' }}>I</Button>
            </Form.Item>
          </Space>

          <Divider />

          <Space>
            <Button type="primary" onClick={handleSaveRule}>
              保存
            </Button>
            <Button onClick={handleCancelEdit}>取消</Button>
          </Space>
        </Form>
      )}
    </Modal>
  )
}

export default ConditionalFormatModal
