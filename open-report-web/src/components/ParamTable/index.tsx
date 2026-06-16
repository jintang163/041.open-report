import { useState, useEffect } from 'react'
import { Table, Button, Input, Select, Checkbox, Space, Popconfirm, Form, message } from 'antd'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { DataSetParam } from '@/types'

interface ParamTableProps {
  value?: DataSetParam[]
  onChange?: (params: DataSetParam[]) => void
  readOnly?: boolean
}

const ParamTable = ({ value = [], onChange, readOnly = false }: ParamTableProps) => {
  const [dataSource, setDataSource] = useState<DataSetParam[]>([])
  const [editingKey, setEditingKey] = useState<string>('')

  useEffect(() => {
    setDataSource(value)
  }, [value])

  const isEditing = (record: DataSetParam) => record.name === editingKey

  const edit = (record: DataSetParam) => {
    if (readOnly) return
    setEditingKey(record.name)
  }

  const cancel = () => {
    setEditingKey('')
  }

  const save = async (name: string) => {
    try {
      const newData = [...dataSource]
      const index = newData.findIndex((item) => item.name === name)
      if (index > -1) {
        setDataSource(newData)
        onChange?.(newData)
      }
      setEditingKey('')
    } catch {
      message.error('保存失败')
    }
  }

  const handleAdd = () => {
    if (readOnly) return
    const newParam: DataSetParam = {
      name: `param_${Date.now()}`,
      label: '新参数',
      type: 'STRING',
      defaultValue: '',
      required: false
    }
    const newData = [...dataSource, newParam]
    setDataSource(newData)
    onChange?.(newData)
    setEditingKey(newParam.name)
  }

  const handleDelete = (name: string) => {
    if (readOnly) return
    const newData = dataSource.filter((item) => item.name !== name)
    setDataSource(newData)
    onChange?.(newData)
  }

  const handleFieldChange = (index: number, field: keyof DataSetParam, value: any) => {
    const newData = [...dataSource]
    ;(newData[index] as any)[field] = value
    setDataSource(newData)
    onChange?.(newData)
  }

  const columns: ColumnsType<DataSetParam> = [
    {
      title: '参数名',
      dataIndex: 'name',
      key: 'name',
      width: 180,
      editable: true,
      render: (text: string, record, index) => {
        if (isEditing(record)) {
          return (
            <Input
              value={text}
              onChange={(e) => handleFieldChange(index, 'name', e.target.value)}
              placeholder="请输入参数名"
              size="small"
            />
          )
        }
        return <code>{text}</code>
      }
    },
    {
      title: '显示名称',
      dataIndex: 'label',
      key: 'label',
      width: 150,
      editable: true,
      render: (text: string, record, index) => {
        if (isEditing(record)) {
          return (
            <Input
              value={text}
              onChange={(e) => handleFieldChange(index, 'label', e.target.value)}
              placeholder="请输入显示名称"
              size="small"
            />
          )
        }
        return text || '-'
      }
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 130,
      editable: true,
      render: (text: string, record, index) => {
        if (isEditing(record)) {
          return (
            <Select
              value={text}
              onChange={(v) => handleFieldChange(index, 'type', v)}
              size="small"
              style={{ width: '100%' }}
              options={[
                { label: '字符串', value: 'STRING' },
                { label: '数字', value: 'NUMBER' },
                { label: '日期', value: 'DATE' },
                { label: '日期时间', value: 'DATETIME' },
                { label: '布尔', value: 'BOOLEAN' }
              ]}
            />
          )
        }
        const typeMap: Record<string, string> = {
          STRING: '字符串',
          NUMBER: '数字',
          DATE: '日期',
          DATETIME: '日期时间',
          BOOLEAN: '布尔'
        }
        return typeMap[text] || text
      }
    },
    {
      title: '默认值',
      dataIndex: 'defaultValue',
      key: 'defaultValue',
      width: 150,
      editable: true,
      render: (text: string, record, index) => {
        if (isEditing(record)) {
          return (
            <Input
              value={text}
              onChange={(e) => handleFieldChange(index, 'defaultValue', e.target.value)}
              placeholder="默认值"
              size="small"
            />
          )
        }
        return text || '-'
      }
    },
    {
      title: '必填',
      dataIndex: 'required',
      key: 'required',
      width: 80,
      align: 'center',
      render: (checked: boolean, record, index) => {
        if (isEditing(record)) {
          return (
            <Checkbox
              checked={checked}
              onChange={(e) => handleFieldChange(index, 'required', e.target.checked)}
            />
          )
        }
        return checked ? '是' : '否'
      }
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      align: 'center',
      render: (_, record) => {
        if (readOnly) return null
        const editable = isEditing(record)
        return editable ? (
          <Space size="small">
            <Button type="link" size="small" onClick={() => save(record.name)}>
              保存
            </Button>
            <Button type="link" size="small" onClick={cancel}>
              取消
            </Button>
          </Space>
        ) : (
          <Space size="small">
            <Button type="link" size="small" disabled={editingKey !== ''} onClick={() => edit(record)}>
              编辑
            </Button>
            <Popconfirm title="确定删除该参数?" onConfirm={() => handleDelete(record.name)}>
              <Button type="link" size="small" danger>
                删除
              </Button>
            </Popconfirm>
          </Space>
        )
      }
    }
  ]

  return (
    <div>
      <div style={{ marginBottom: 12 }}>
        {!readOnly && (
          <Button type="dashed" onClick={handleAdd} icon={<PlusOutlined />} size="small">
            添加参数
          </Button>
        )}
      </div>
      <Table
        columns={columns}
        dataSource={dataSource}
        rowKey="name"
        size="small"
        pagination={false}
        locale={{ emptyText: readOnly ? '暂无参数' : '暂无参数，点击上方按钮添加' }}
      />
    </div>
  )
}

export default ParamTable
