import { useState, useEffect, useRef } from 'react'
import { Input, Button, Dropdown, Space, Tooltip } from 'antd'
import type { MenuProps } from 'antd'
import {
  FunctionOutlined,
  CheckOutlined,
  CloseOutlined
} from '@ant-design/icons'
import { useDesignerStore } from '../../store/designer'
import {
  cellPositionToRef,
  setCellFormula,
  getCellValue,
  refreshLuckysheet
} from '../../utils/luckysheet'
import { getFunctionDocs } from '@/api/function'
import type { FunctionDoc, FunctionCategory } from '@/types'

const CATEGORY_LABELS: Record<FunctionCategory, string> = {
  MATH: '数学函数',
  DATE: '日期函数',
  STRING: '字符串函数',
  LOGIC: '逻辑函数',
  CUSTOM: '自定义函数'
}

const FormulaBar: React.FC = () => {
  const {
    selectedCell,
    cellValue,
    setCellValue,
    setExpressionEditorVisible
  } = useDesignerStore()

  const [editing, setEditing] = useState(false)
  const [functionDocs, setFunctionDocs] = useState<FunctionDoc[]>([])
  const [loading, setLoading] = useState(false)
  const inputRef = useRef<any>(null)

  useEffect(() => {
    loadFunctionDocs()
  }, [])

  const loadFunctionDocs = async () => {
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

  const cellRef = selectedCell
    ? cellPositionToRef(selectedCell.row, selectedCell.col)
    : ''

  const buildFunctionMenu = (): MenuProps['items'] => {
    if (functionDocs.length === 0) {
      return [{ key: 'empty', label: '暂无可用函数', disabled: true }]
    }

    const categories: FunctionCategory[] = ['MATH', 'DATE', 'STRING', 'LOGIC', 'CUSTOM']
    const items: MenuProps['items'] = []

    categories.forEach((category, idx) => {
      const funcs = functionDocs.filter((f) => f.category === category && f.status === 1)
      if (funcs.length === 0) return

      if (idx > 0) {
        items.push({ type: 'divider' as const })
      }

      items.push({
        key: `group-${category}`,
        label: (
          <span style={{ fontSize: 12, color: '#999', fontWeight: 500 }}>
            {CATEGORY_LABELS[category]}
          </span>
        ),
        disabled: true
      })

      funcs.forEach((func) => {
        items.push({
          key: func.name,
          label: (
            <div style={{ padding: '2px 0' }}>
              <div style={{ fontWeight: 500 }}>
                {func.name} - {func.label}
              </div>
              {func.description && (
                <div style={{ fontSize: 12, color: '#999', marginTop: 2 }}>
                  {func.description}
                </div>
              )}
              {func.example && (
                <div style={{ fontSize: 11, color: '#bbb', fontFamily: 'monospace', marginTop: 2 }}>
                  示例: {func.example}
                </div>
              )}
            </div>
          )
        })
      })
    })

    return items
  }

  const handleFunctionSelect: MenuProps['onClick'] = ({ key }) => {
    if (key.startsWith('group-') || key === 'empty') return
    const func = functionDocs.find((f) => f.name === key)
    let paramStr = ''
    if (func?.params && func.params.length > 0) {
      paramStr = func.params.map((p) => p.name).join(', ')
    }
    const formula = `${key}(${paramStr})`
    setCellValue(formula)
    setEditing(true)
    setTimeout(() => {
      if (inputRef.current?.input) {
        const textarea = inputRef.current.input
        const pos = formula.length - 1
        textarea.setSelectionRange(pos, pos)
        textarea.focus()
      }
    }, 50)
  }

  const handleConfirm = () => {
    if (!selectedCell) return
    setCellFormula(selectedCell.row, selectedCell.col, cellValue)
    refreshLuckysheet()
    setEditing(false)
  }

  const handleCancel = () => {
    if (selectedCell) {
      const val = getCellValue(selectedCell.row, selectedCell.col)
      setCellValue(val ? String(val) : '')
    }
    setEditing(false)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      handleConfirm()
    } else if (e.key === 'Escape') {
      handleCancel()
    }
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCellValue(e.target.value)
    setEditing(true)
  }

  const handleOpenExpressionEditor = () => {
    setExpressionEditorVisible(true, cellValue)
  }

  const functionMenu: MenuProps = {
    onClick: handleFunctionSelect,
    items: buildFunctionMenu()
  }

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        padding: '4px 12px',
        borderBottom: '1px solid #f0f0f0',
        background: '#fafafa',
        gap: 8
      }}
    >
      <Input
        value={cellRef}
        readOnly
        style={{ width: 80, textAlign: 'center', fontFamily: 'monospace' }}
        size="small"
        prefix={<span style={{ color: '#999', fontSize: 11 }}>单元格</span>}
      />

      <Space size={2}>
        {editing ? (
          <>
            <Tooltip title="确认">
              <Button
                size="small"
                type="text"
                icon={<CheckOutlined style={{ color: '#52c41a' }} />}
                onClick={handleConfirm}
              />
            </Tooltip>
            <Tooltip title="取消">
              <Button
                size="small"
                type="text"
                icon={<CloseOutlined style={{ color: '#ff4d4f' }} />}
                onClick={handleCancel}
              />
            </Tooltip>
          </>
        ) : (
          <Tooltip title="插入函数">
            <Button
              size="small"
              type="text"
              icon={<FunctionOutlined />}
              onClick={handleOpenExpressionEditor}
            >
              fx
            </Button>
          </Tooltip>
        )}
      </Space>

      <Dropdown menu={functionMenu} trigger={['click']} loading={loading}>
        <Button size="small" type="text">
          函数 ▼
        </Button>
      </Dropdown>

      <Input
        ref={inputRef}
        value={cellValue}
        onChange={handleInputChange}
        onKeyDown={handleKeyDown}
        onFocus={() => setEditing(true)}
        placeholder="输入值或公式，例如：=SUM(A1:A10) 或 ${dataset.field}"
        style={{ flex: 1, fontFamily: 'monospace' }}
        size="small"
        allowClear
      />
    </div>
  )
}

export default FormulaBar
