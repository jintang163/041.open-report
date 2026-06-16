import { useState, useRef, useEffect, useCallback } from 'react'
import { Card, Button, Space, message } from 'antd'
import { PlayCircleOutlined, CopyOutlined, ClearOutlined } from '@ant-design/icons'
import './style.css'

interface SqlEditorProps {
  value?: string
  onChange?: (value: string) => void
  height?: number | string
  readOnly?: boolean
  showToolbar?: boolean
  onExecute?: (sql: string) => Promise<any> | void
  placeholder?: string
}

const SqlEditor = ({
  value = '',
  onChange,
  height = 300,
  readOnly = false,
  showToolbar = true,
  onExecute,
  placeholder = '请输入 SQL 语句...'
}: SqlEditorProps) => {
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const lineNumbersRef = useRef<HTMLDivElement>(null)
  const [lineCount, setLineCount] = useState(1)

  useEffect(() => {
    const lines = value.split('\n').length || 1
    setLineCount(lines)
  }, [value])

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newValue = e.target.value
    onChange?.(newValue)
  }

  const handleScroll = () => {
    if (textareaRef.current && lineNumbersRef.current) {
      lineNumbersRef.current.scrollTop = textareaRef.current.scrollTop
    }
  }

  const handleExecute = useCallback(async () => {
    if (!value.trim()) {
      message.warning('请输入 SQL 语句')
      return
    }
    try {
      await onExecute?.(value)
    } catch (e: any) {
      message.error(e.message || '执行失败')
    }
  }, [value, onExecute])

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(value)
      message.success('已复制到剪贴板')
    } catch {
      message.error('复制失败')
    }
  }

  const handleClear = () => {
    onChange?.('')
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault()
      handleExecute()
    }
    if (e.key === 'Tab') {
      e.preventDefault()
      const start = e.currentTarget.selectionStart
      const end = e.currentTarget.selectionEnd
      const newValue = value.substring(0, start) + '  ' + value.substring(end)
      onChange?.(newValue)
      setTimeout(() => {
        if (textareaRef.current) {
          textareaRef.current.selectionStart = textareaRef.current.selectionEnd = start + 2
        }
      }, 0)
    }
  }

  return (
    <Card
      size="small"
      styles={{ body: { padding: 0 } }}
      extra={
        showToolbar && (
          <Space size="small">
            {!readOnly && (
              <Button
                type="text"
                icon={<ClearOutlined />}
                size="small"
                onClick={handleClear}
              >
                清空
              </Button>
            )}
            <Button
              type="text"
              icon={<CopyOutlined />}
              size="small"
              onClick={handleCopy}
            >
              复制
            </Button>
            {onExecute && (
              <Button
                type="primary"
                icon={<PlayCircleOutlined />}
                size="small"
                onClick={handleExecute}
              >
                执行 (Ctrl+Enter)
              </Button>
            )}
          </Space>
        )
      }
    >
      <div className="sql-editor-container" style={{ height }}>
        <div className="sql-editor-line-numbers" ref={lineNumbersRef}>
          {Array.from({ length: lineCount }, (_, i) => (
            <div key={i} className="sql-editor-line-number">
              {i + 1}
            </div>
          ))}
        </div>
        <textarea
          ref={textareaRef}
          className="sql-editor-textarea"
          value={value}
          onChange={handleChange}
          onScroll={handleScroll}
          onKeyDown={handleKeyDown}
          readOnly={readOnly}
          placeholder={placeholder}
          spellCheck={false}
        />
      </div>
    </Card>
  )
}

export default SqlEditor
