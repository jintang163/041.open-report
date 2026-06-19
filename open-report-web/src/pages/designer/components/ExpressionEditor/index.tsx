import { useState, useEffect, useRef } from 'react'
import { Modal, Tabs, Input, Tree, Button, message, Typography, Space } from 'antd'
import {
  PlusOutlined,
  MinusOutlined,
  MultiplyOutlined,
  DividerOutlined
} from '@ant-design/icons'
import { useDesignerStore, type DataSetWithFields } from '../../store/designer'
import { getFunctionDocs } from '@/api/function'
import type { FunctionDoc, FunctionParam, FunctionCategory } from '@/types'

const { Text } = Typography
const { TextArea } = Input

const CATEGORY_LABELS: Record<FunctionCategory, string> = {
  MATH: '数学函数',
  DATE: '日期函数',
  STRING: '字符串函数',
  LOGIC: '逻辑函数',
  CUSTOM: '自定义函数'
}

const buildSyntax = (name: string, params?: FunctionParam[]): string => {
  if (!params || params.length === 0) {
    return `${name}()`
  }
  const paramStr = params
    .map((p) => (p.required ? p.name : `[${p.name}]`))
    .join(', ')
  return `${name}(${paramStr})`
}

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
  const [selectedFunction, setSelectedFunction] = useState<FunctionDoc | null>(null)
  const [functionDocs, setFunctionDocs] = useState<FunctionDoc[]>([])
  const [loading, setLoading] = useState(false)
  const textareaRef = useRef<any>(null)

  useEffect(() => {
    if (expressionEditorVisible) {
      setExpression(expressionEditorInitialValue)
      loadFunctionDocs()
    }
  }, [expressionEditorVisible, expressionEditorInitialValue])

  const loadFunctionDocs = async () => {
    if (functionDocs.length > 0) return
    try {
      setLoading(true)
      const data = await getFunctionDocs()
      setFunctionDocs(data || [])
    } catch (err) {
      console.error('加载函数列表失败:', err)
    } finally {
      setLoading(false)
    }
  }

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
    if (functionDocs.length === 0) {
      return [{ key: 'empty', title: loading ? '加载中...' : '暂无可用函数', isLeaf: true, disabled: true }]
    }

    const categories = new Map<string, FunctionDoc[]>()
    const filteredFunctions = functionDocs.filter(
      (fn) =>
        fn.status === 1 &&
        (!searchKeyword ||
          fn.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
          fn.label.includes(searchKeyword) ||
          (fn.description && fn.description.includes(searchKeyword)))
    )

    filteredFunctions.forEach((fn) => {
      const categoryLabel = CATEGORY_LABELS[fn.category] || fn.category
      if (!categories.has(categoryLabel)) {
        categories.set(categoryLabel, [])
      }
      categories.get(categoryLabel)!.push(fn)
    })

    const categoryOrder: FunctionCategory[] = ['MATH', 'DATE', 'STRING', 'LOGIC', 'CUSTOM']
    const result: any[] = []
    categoryOrder.forEach((cat) => {
      const label = CATEGORY_LABELS[cat]
      const functions = categories.get(label)
      if (functions && functions.length > 0) {
        result.push({
          key: `category-${cat}`,
          title: label,
          children: functions.map((fn) => ({
            key: `fn-${fn.name}`,
            title: `${fn.name} - ${fn.label}`,
            isLeaf: true,
            functionData: fn
          }))
        })
      }
    })

    categories.forEach((functions, label) => {
      if (!categoryOrder.includes(label as FunctionCategory)) {
        result.push({
          key: `category-${label}`,
          title: label,
          children: functions.map((fn) => ({
            key: `fn-${fn.name}`,
            title: `${fn.name} - ${fn.label}`,
            isLeaf: true,
            functionData: fn
          }))
        })
      }
    })

    return result
  }

  const handleFieldSelect = (selectedKeys: any[], info: any) => {
    if (info.node.isLeaf && info.node.datasetName && info.node.fieldName) {
      const fieldExpr = `\${${info.node.datasetName}.${info.node.fieldName}}`
      insertText(fieldExpr)
    }
  }

  const handleFunctionSelect = (selectedKeys: any[], info: any) => {
    if (info.node.isLeaf && info.node.functionData) {
      const fn = info.node.functionData as FunctionDoc
      setSelectedFunction(fn)
      let paramStr = ''
      if (fn.params && fn.params.length > 0) {
        paramStr = fn.params.map((p) => p.name).join(', ')
      }
      insertText(`${fn.name}(${paramStr})`)
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
                {selectedFunction.description && (
                  <div><Text type="secondary">说明：</Text>{selectedFunction.description}</div>
                )}
                <div><Text type="secondary">语法：</Text><Text code>{buildSyntax(selectedFunction.name, selectedFunction.params)}</Text></div>
                {selectedFunction.params && selectedFunction.params.length > 0 && (
                  <div>
                    <Text type="secondary">参数：</Text>
                    {selectedFunction.params.map((p, idx) => (
                      <span key={idx} style={{ marginRight: 8 }}>
                        <Text code>{p.name}</Text>
                        <Text type="secondary" style={{ marginLeft: 4 }}>
                          ({p.type}{p.required ? ', 必填' : ''}{p.description ? ` - ${p.description}` : ''})
                        </Text>
                      </span>
                    ))}
                  </div>
                )}
                {selectedFunction.example && (
                  <div><Text type="secondary">示例：</Text><Text code>{selectedFunction.example}</Text></div>
                )}
                {selectedFunction.returnType && (
                  <div><Text type="secondary">返回值：</Text>{selectedFunction.returnType}</div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </Modal>
  )
}

export default ExpressionEditor
