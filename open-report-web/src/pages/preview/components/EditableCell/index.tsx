import React, { useState, useRef, useEffect } from 'react'
import { Input, InputNumber, DatePicker, Select, message } from 'antd'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import type { EditableCellConfig, FieldType } from '@/types'

interface EditableCellProps {
  config: EditableCellConfig
  value: any
  onChange: (value: any) => void
  rowIndex: number
  isEditing: boolean
  onStartEdit: () => void
  onFinishEdit: () => void
}

const EditableCell: React.FC<EditableCellProps> = ({
  config,
  value,
  onChange,
  isEditing,
  onStartEdit,
  onFinishEdit
}) => {
  const inputRef = useRef<any>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (isEditing && inputRef.current) {
      setTimeout(() => {
        inputRef.current?.focus?.()
        inputRef.current?.select?.()
      }, 50)
    }
  }, [isEditing])

  const validate = (val: any): boolean => {
    if (config.required && (val === undefined || val === null || val === '')) {
      setError('此字段为必填项')
      return false
    }

    if (val === undefined || val === null || val === '') {
      setError(null)
      return true
    }

    if (config.validationRule) {
      try {
        const regex = new RegExp(config.validationRule)
        if (!regex.test(String(val))) {
          setError(config.validationMessage || '格式不正确')
          return false
        }
      } catch (e) {
        console.error('正则表达式错误:', e)
      }
    }

    if (config.fieldType === 'NUMBER') {
      if (isNaN(Number(val))) {
        setError('请输入有效的数字')
        return false
      }
    }

    setError(null)
    return true
  }

  const handleChange = (newValue: any) => {
    validate(newValue)
    onChange(newValue)
  }

  const handleBlur = () => {
    if (validate(value)) {
      onFinishEdit()
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      if (validate(value)) {
        onFinishEdit()
      }
    } else if (e.key === 'Escape') {
      setError(null)
      onFinishEdit()
    }
  }

  if (!config.editable) {
    return <span>{value ?? '-'}</span>
  }

  const renderInput = () => {
    const fieldType: FieldType = config.fieldType as FieldType

    switch (fieldType) {
      case 'NUMBER':
        return (
          <InputNumber
            ref={inputRef}
            value={value as number}
            onChange={handleChange}
            onBlur={handleBlur}
            onKeyDown={handleKeyDown}
            style={{ width: '100%' }}
            size="small"
          />
        )

      case 'DATE':
        return (
          <DatePicker
            ref={inputRef}
            value={value ? dayjs(value) : undefined}
            onChange={(date: Dayjs | null) => handleChange(date ? date.format('YYYY-MM-DD') : null)}
            onOpenChange={(open) => {
              if (!open) handleBlur()
            }}
            style={{ width: '100%' }}
            size="small"
          />
        )

      case 'DATETIME':
        return (
          <DatePicker
            ref={inputRef}
            showTime
            value={value ? dayjs(value) : undefined}
            onChange={(date: Dayjs | null) => handleChange(date ? date.format('YYYY-MM-DD HH:mm:ss') : null)}
            onOpenChange={(open) => {
              if (!open) handleBlur()
            }}
            style={{ width: '100%' }}
            size="small"
          />
        )

      case 'SELECT':
        return (
          <Select
            ref={inputRef}
            value={value}
            onChange={handleChange}
            onBlur={handleBlur}
            style={{ width: '100%' }}
            size="small"
            options={config.options?.map(opt => ({ label: opt, value: opt }))}
          />
        )

      default:
        return (
          <Input
            ref={inputRef}
            value={value as string}
            onChange={(e) => handleChange(e.target.value)}
            onBlur={handleBlur}
            onKeyDown={handleKeyDown}
            size="small"
          />
        )
    }
  }

  return (
    <div style={{ position: 'relative', width: '100%' }}>
      {isEditing ? (
        renderInput()
      ) : (
        <div
          onClick={onStartEdit}
          style={{
            cursor: 'pointer',
            padding: '4px 8px',
            minHeight: '32px',
            display: 'flex',
            alignItems: 'center',
            background: config.editable ? '#f0f7ff' : 'transparent',
            border: '1px solid transparent',
            borderRadius: '4px',
            transition: 'all 0.2s'
          }}
          onMouseEnter={(e) => {
            if (config.editable) {
              e.currentTarget.style.border = '1px solid #1890ff'
              e.currentTarget.style.background = '#e6f4ff'
            }
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.border = '1px solid transparent'
            e.currentTarget.style.background = config.editable ? '#f0f7ff' : 'transparent'
          }}
        >
          <span style={{ flex: 1 }}>{value ?? '-'}</span>
          {config.editable && (
            <span
              style={{
                fontSize: '12px',
                color: '#1890ff',
                marginLeft: '4px'
              }}
            >
              ✎
            </span>
          )}
        </div>
      )}
      {error && (
        <div
          style={{
            position: 'absolute',
            top: '100%',
            left: 0,
            right: 0,
            background: '#fff2f0',
            border: '1px solid #ffccc7',
            color: '#ff4d4f',
            padding: '4px 8px',
            fontSize: '12px',
            borderRadius: '4px',
            marginTop: '2px',
            zIndex: 1000,
            whiteSpace: 'nowrap'
          }}
        >
          {error}
        </div>
      )}
    </div>
  )
}

export default EditableCell
