import { useState, useEffect, useRef } from 'react'
import { Modal, Tabs, Input, Tree, Button, message, Typography, Space } from 'antd'
import {
  PlusOutlined,
  MinusOutlined,
  MultiplyOutlined,
  DividerOutlined
} from '@ant-design/icons'
import { useDesignerStore, type DataSetWithFields } from '../../store/designer'

const { Text } = Typography
const { TextArea } = Input

export interface FunctionItem {
  name: string
  label: string
  category: string
  description: string
  syntax: string
  example: string
}

const FUNCTION_LIST: FunctionItem[] = [
  { name: 'SUM', label: '求和', category: '数学函数', description: '计算一组数值的总和', syntax: 'SUM(number1, [number2], ...)', example: 'SUM(A1:A10)' },
  { name: 'AVG', label: '平均值', category: '数学函数', description: '计算一组数值的平均值', syntax: 'AVG(number1, [number2], ...)', example: 'AVG(B1:B5)' },
  { name: 'COUNT', label: '计数', category: '数学函数', description: '计算包含数字的单元格数量', syntax: 'COUNT(value1, [value2], ...)', example: 'COUNT(C1:C20)' },
  { name: 'MAX', label: '最大值', category: '数学函数', description: '返回一组数值中的最大值', syntax: 'MAX(number1, [number2], ...)', example: 'MAX(D1:D10)' },
  { name: 'MIN', label: '最小值', category: '数学函数', description: '返回一组数值中的最小值', syntax: 'MIN(number1, [number2], ...)', example: 'MIN(E1:E10)' },
  { name: 'ROUND', label: '四舍五入', category: '数学函数', description: '按指定小数位数四舍五入', syntax: 'ROUND(number, decimals)', example: 'ROUND(3.1415, 2)' },
  { name: 'ABS', label: '绝对值', category: '数学函数', description: '返回数字的绝对值', syntax: 'ABS(number)', example: 'ABS(-5)' },
  { name: 'IF', label: '条件判断', category: '逻辑函数', description: '根据条件返回不同的值', syntax: 'IF(condition, trueValue, falseValue)', example: 'IF(A1>10, "大于10", "小于等于10")' },
  { name: 'AND', label: '与', category: '逻辑函数', description: '所有条件为真时返回真', syntax: 'AND(logical1, [logical2], ...)', example: 'AND(A1>0, B1>0)' },
  { name: 'OR', label: '或', category: '逻辑函数', description: '任一条件为真时返回真', syntax: 'OR(logical1, [logical2], ...)', example: 'OR(A1>10, B1>10)' },
  { name: 'NOT', label: '非', category: '逻辑函数', description: '对条件取反', syntax: 'NOT(logical)', example: 'NOT(A1="")' },
  { name: 'CONCAT', label: '拼接', category: '文本函数', description: '将多个文本字符串拼接在一起', syntax: 'CONCAT(text1, [text2], ...)', example: 'CONCAT(A1, "-", B1)' },
  { name: 'SUBSTRING', label: '截取', category: '文本函数', description: '从文本中截取子串', syntax: 'SUBSTRING(text, start, length)', example: 'SUBSTRING(A1, 1, 3)' },
  { name: 'LENGTH', label: '长度', category: '文本函数', description: '返回文本的长度', syntax: 'LENGTH(text)', example: 'LENGTH(A1)' },
  { name: 'UPPER', label: '转大写', category: '文本函数', description: '将文本转换为大写', syntax: 'UPPER(text)', example: 'UPPER(A1)' },
  { name: 'LOWER', label: '转小写', category: '文本函数', description: '将文本转换为小写', syntax: 'LOWER(text)', example: 'LOWER(A1)' },
  { name: 'TRIM', label: '去空格', category: '文本函数', description: '去除文本首尾空格', syntax: 'TRIM(text)', example: 'TRIM(A1)' },
  { name: 'TODAY', label: '今天', category: '日期函数', description: '返回当前日期', syntax: 'TODAY()', example: 'TODAY()' },
  { name: 'NOW', label: '当前时间', category: '日期函数', description: '返回当前日期和时间', syntax: 'NOW()', example: 'NOW()' },
  { name: 'YEAR', label: '年', category: '日期函数', description: '返回日期的年份', syntax: 'YEAR(date)', example: 'YEAR(A1)' },
  { name: 'MONTH', label: '月', category: '日期函数', description: '返回日期的月份', syntax: 'MONTH(date)', example: 'MONTH(A1)' },
  { name: 'DAY', label: '日', category: '日期函数', description: '返回日期的天数', syntax: 'DAY(date)', example: 'DAY(A1)' },
  { name: 'FORMAT_DATE', label: '格式化日期', category: '日期函数', description: '按指定格式格式化日期', syntax: 'FORMAT_DATE(date, pattern)', example: 'FORMAT_DATE(A1, "yyyy-MM-dd")' }
]

const OPERATORS = [
  { label: '+', icon: <PlusOutlined />, value: ' + ' },
  { label: '-', icon: <MinusOutlined />, value: ' - ' },
  { label: '*', icon: <MultiplyOutlined />, value: ' * ' },
  { label: '/', icon: <DividerOutlined />, value: ' / ' },
  { label: '(', value: '(' },
  { label: ')', value: ')' },
  { label: ',', value: ', ' },
  { label: '=', value: ' = ' },
  { label: '>', value: ' > ' },
  { label: '<', value: ' < ' },
  { label: '>=', value: ' >= ' },
  { label: '<=', value: ' <= ' },
  { label: '!=', value: ' != ' },
  { label: '&&', value: ' && ' },
  { label: '||', value: ' || ' }
]

const ExpressionEditor: React.FC = () => {
  const {
    expressionEditorVisible,
    expressionEditorInitialValue,
    dataSources,
    setExpressionEditorVisible,
    selectedCell
  } = useDesignerStore()

  const [expression, setExpression] = useState('')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [selectedFunction, setSelectedFunction] = useState<FunctionItem | null>(null)
  const textareaRef = useRef<any>(null)

  useEffect(() => {
    if (expressionEditorVisible) {
      setExpression(expressionEditorInitialValue)
    }
  }, [expressionEditorVisible, expressionEditorInitialValue])

  const buildFieldTreeData = (): any[] => {
    return dataSources.map((ds) => ({
      key: `ds-${ds.id}`,
      title: ds.name,
      children: (ds.dataSets || []).map((dataset) => ({
        key: `dataset-${dataset.id}`,
        title: dataset.name,
        children: (dataset.fields || []).map((field) => ({
          key: `field-${dataset.id}-${field.name}`,
          title: `${field.label || field.name} (${field.type})`,
          isLeaf: true,
          datasetName: dataset.name,
          fieldName: field.name,
          fieldType: field.type
        }))
      }))
    }))
  }

  const buildFunctionTreeData = (): any[] => {
    const categories = new Map<string, FunctionItem[]>()
    const filteredFunctions = FUNCTION_LIST.filter(
      (fn) =>
        !searchKeyword ||
        fn.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
        fn.label.includes(searchKeyword) ||
        fn.description.includes(searchKeyword)
    )

    filteredFunctions.forEach((fn) => {
      if (!categories.has(fn.category)) {
        categories.set(fn.category, [])
      }
      categories.get(fn.category)!.push(fn)
    })

    return Array.from(categories.entries()).map(([category, functions]) => ({
      key: `category-${category}`,
      title: category,
      children: functions.map((fn) => ({
        key: `fn-${fn.name}`,
        title: `${fn.name} - ${fn.label}`,
        isLeaf: true,
        functionData: fn
      }))
    }))
  }

  const handleFieldSelect = (selectedKeys: any[], info: any) => {
    if (info.node.isLeaf && info.node.datasetName && info.node.fieldName) {
      const fieldExpr = `\${${info.node.datasetName}.${info.node.fieldName}}`
      insertText(fieldExpr)
    }
  }

  const handleFunctionSelect = (selectedKeys: any[], info: any) => {
    if (info.node.isLeaf && info.node.functionData) {
      const fn = info.node.functionData as FunctionItem
      setSelectedFunction(fn)
      insertText(`${fn.name}()`)
      setTimeout(() => {
        if (textareaRef.current?.resizableTextArea?.textArea) {
          const textarea = textareaRef.current.resizableTextArea.textArea
          const pos = textarea.selectionStart - 1
          textarea.setSelectionRange(pos, pos)
          textarea.focus()
        }
      }, 50)
    }
  }

  const insertText = (text: string) => {
    setExpression((prev) => {
      if (textareaRef.current?.resizableTextArea?.textArea) {
        const textarea = textareaRef.current.resizableTextArea.textArea
        const start = textarea.selectionStart
        const end = textarea.selectionEnd
        const newVal = prev.substring(0, start) + text + prev.substring(end)
        setTimeout(() => {
          const newPos = start + text.length
          textarea.setSelectionRange(newPos, newPos)
          textarea.focus()
        }, 0)
        return newVal
      }
      return prev + text
    })
  }

  const handleOperatorClick = (value: string) => {
    insertText(value)
  }

  const handleOk = () => {
    if (!selectedCell) {
      message.warning('请先选择一个单元格')
      return
    }
    setExpressionEditorVisible(false)
  }

  const handleCancel = () => {
    setExpressionEditorVisible(false)
  }

  const handleInsert = () => {
    if (!selectedCell) {
      message.warning('请先选择一个单元格')
      return
    }
    setExpressionEditorVisible(false)
  }

  const getPreviewResult = (): string => {
    try {
      let result = expression
      dataSources.forEach((ds) => {
        ds.dataSets?.forEach((dataset) => {
          dataset.fields?.forEach((field) => {
            const pattern = new RegExp(`\\$\\{${dataset.name}\\.${field.name}\\}`, 'g')
            result = result.replace(pattern, `[${field.label || field.name}]`)
          })
        })
      })
      return result || '表达式预览...'
    } catch {
      return expression
    }
  }

  return (
    <Modal
      title="表达式编辑器"
      open={expressionEditorVisible}
      onOk={handleOk}
      onCancel={handleCancel}
      width={900}
      footer={[
        <Button key="cancel" onClick={handleCancel}>
          取消
        </Button>,
        <Button key="insert" type="primary" onClick={handleInsert}>
          插入
        </Button>
      ]}
    >
      <div style={{ display: 'flex', height: 500, gap: 12 }}>
        <div style={{ width: 260, display: 'flex', flexDirection: 'column' }}>
          <Tabs
            defaultActiveKey="functions"
            items={[
              {
                key: 'functions',
                label: '函数',
                children: (
                  <div style={{ height: 420, overflow: 'auto' }}>
                    <Input.Search
                      placeholder="搜索函数"
                      value={searchKeyword}
                      onChange={(e) => setSearchKeyword(e.target.value)}
                      style={{ marginBottom: 8 }}
                      allowClear
                    />
                    <Tree
                      showLine
                      defaultExpandAll
                      treeData={buildFunctionTreeData()}
                      onSelect={handleFunctionSelect}
                      blockNode
                    />
                  </div>
                )
              },
              {
                key: 'fields',
                label: '字段',
                children: (
                  <div style={{ height: 420, overflow: 'auto' }}>
                    <Tree
                      showLine
                      defaultExpandAll
                      treeData={buildFieldTreeData()}
                      onSelect={handleFieldSelect}
                      blockNode
                    />
                  </div>
                )
              }
            ]}
          />
        </div>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          <Space wrap size={4} style={{ marginBottom: 8 }}>
            {OPERATORS.map((op) => (
              <Button
                key={op.label}
                size="small"
                onClick={() => handleOperatorClick(op.value)}
                icon={op.icon}
              >
                {!op.icon && op.label}
              </Button>
            ))}
          </Space>

          <TextArea
            ref={textareaRef}
            value={expression}
            onChange={(e) => setExpression(e.target.value)}
            placeholder="请输入表达式，支持函数和字段引用，例如：SUM(${sales.amount})"
            autoSize={{ minRows: 8, maxRows: 8 }}
            style={{ fontFamily: 'monospace', marginBottom: 12 }}
          />

          <div>
            <Text strong style={{ display: 'block', marginBottom: 4 }}>
              预览：
            </Text>
            <div
              style={{
                padding: 12,
                background: '#f5f5f5',
                borderRadius: 4,
                fontFamily: 'monospace',
                minHeight: 60,
                wordBreak: 'break-all'
              }}
            >
              {getPreviewResult()}
            </div>
          </div>

          {selectedFunction && (
            <div style={{ marginTop: 12, padding: 12, background: '#e6f4ff', borderRadius: 4 }}>
              <Text strong>{selectedFunction.name} - {selectedFunction.label}</Text>
              <div style={{ marginTop: 4, fontSize: 13 }}>
                <div><Text type="secondary">说明：</Text>{selectedFunction.description}</div>
                <div><Text type="secondary">语法：</Text><Text code>{selectedFunction.syntax}</Text></div>
                <div><Text type="secondary">示例：</Text><Text code>{selectedFunction.example}</Text></div>
              </div>
            </div>
          )}
        </div>
      </div>
    </Modal>
  )
}

export default ExpressionEditor
