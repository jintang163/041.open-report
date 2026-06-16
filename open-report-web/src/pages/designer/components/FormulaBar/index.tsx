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

const COMMON_FUNCTIONS = [
  { key: 'SUM', label: 'SUM - 求和' },
  { key: 'AVG', label: 'AVG - 平均值' },
  { key: 'COUNT', label: 'COUNT - 计数' },
  { key: 'MAX', label: 'MAX - 最大值' },
  { key: 'MIN', label: 'MIN - 最小值' },
  { key: 'IF', label: 'IF - 条件判断' },
  { key: 'CONCAT', label: 'CONCAT - 拼接' },
  { key: 'ROUND', label: 'ROUND - 四舍五入' },
  { key: 'TODAY', label: 'TODAY - 今天' },
  { key: 'NOW', label: 'NOW - 当前时间' }
]

const FormulaBar: React.FC = () => {
  const {
    selectedCell,
    cellValue,
    setCellValue,
    setExpressionEditorVisible
  } = useDesignerStore()

  const [editing, setEditing] = useState(false)
  const inputRef = useRef<any>(null)

  const cellRef = selectedCell
    ? cellPositionToRef(selectedCell.row, selectedCell.col)
    : ''

  const handleFunctionSelect: MenuProps['onClick'] = ({ key }) => {
    const formula = `${key}()`
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
    items: COMMON_FUNCTIONS.map((f) => ({
      key: f.key,
      label: f.label
    }))
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

      <Dropdown menu={functionMenu} trigger={['click']}>
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
