import React from 'react'
import { Table, Button, Space, Tag, Popconfirm } from 'antd'
import { PlusOutlined, DeleteOutlined, SaveOutlined } from '@ant-design/icons'
import EditableCell from '../EditableCell'
import { useWritebackStore } from '../../store/writeback'
import type { ColumnsType } from 'antd/es/table'
import type { EditableRowData, EditableCellConfig, RowStatus } from '@/types'

interface EditableTableProps {
  reportId: number
  data: Record<string, any>[]
  loading?: boolean
  onSubmit?: () => void
}

const EditableTable: React.FC<EditableTableProps> = ({
  reportId,
  data,
  loading = false,
  onSubmit
}) => {
  const {
    activeConfig,
    editableRows,
    editingCell,
    submitting,
    initEditableRows,
    addEditableRow,
    updateCellValue,
    markRowForDelete,
    setEditingCell,
    submitData,
    getDirtyRows
  } = useWritebackStore()

  React.useEffect(() => {
    if (activeConfig && data && data.length > 0) {
      initEditableRows(data)
    }
  }, [activeConfig, data, initEditableRows])

  const getRowStatusTag = (status: RowStatus, dirty: boolean) => {
    if (status === 'INSERT') {
      return <Tag color="green">新增</Tag>
    }
    if (status === 'DELETE') {
      return <Tag color="red">删除</Tag>
    }
    if (dirty && status === 'UPDATE') {
      return <Tag color="blue">修改</Tag>
    }
    return null
  }

  const handleAddRow = () => {
    const defaultData: Record<string, any> = {}
    activeConfig?.fields?.forEach(field => {
      if (field.defaultValue !== undefined && field.defaultValue !== null) {
        defaultData[field.fieldName] = field.defaultValue
      }
    })
    addEditableRow(defaultData)
  }

  const handleDeleteRow = (rowIndex: number) => {
    markRowForDelete(rowIndex)
  }

  const handleStartEdit = (rowIndex: number, fieldName: string) => {
    setEditingCell({ rowIndex, fieldName })
  }

  const handleFinishEdit = () => {
    setEditingCell(null)
  }

  const handleCellChange = (rowIndex: number, fieldName: string, value: any) => {
    updateCellValue(rowIndex, fieldName, value)
  }

  const handleSubmit = async () => {
    const result = await submitData(reportId)
    if (result && onSubmit) {
      onSubmit()
    }
  }

  const dirtyCount = getDirtyRows().length

  const columns: ColumnsType<EditableRowData> = activeConfig?.fields?.map(field => ({
    title: (
      <div>
        {field.fieldName}
        {field.required === 1 && <span style={{ color: 'red', marginLeft: '4px' }}>*</span>}
      </div>
    ),
    dataIndex: ['currentData', field.fieldName],
    key: field.fieldName,
    width: 150,
    render: (text: any, record: EditableRowData) => {
      const cellConfig: EditableCellConfig = record.cells[field.fieldName] || {
        editable: field.editable === 1,
        fieldName: field.fieldName,
        fieldType: field.fieldType,
        required: field.required === 1,
        validationRule: field.validationRule,
        validationMessage: field.validationMessage,
        cellPosition: field.cellPosition
      }

      const isEditing =
        editingCell?.rowIndex === record.rowIndex &&
        editingCell?.fieldName === field.fieldName

      return (
        <EditableCell
          config={cellConfig}
          value={text}
          onChange={(value) => handleCellChange(record.rowIndex, field.fieldName, value)}
          rowIndex={record.rowIndex}
          isEditing={isEditing}
          onStartEdit={() => handleStartEdit(record.rowIndex, field.fieldName)}
          onFinishEdit={handleFinishEdit}
        />
      )
    }
  })) || []

  if (activeConfig) {
    columns.push({
      title: '状态',
      key: 'status',
      width: 80,
      fixed: 'right',
      render: (_: any, record: EditableRowData) =>
        getRowStatusTag(record.rowStatus, record.dirty)
    })

    columns.push({
      title: '操作',
      key: 'action',
      width: 80,
      fixed: 'right',
      render: (_: any, record: EditableRowData) => (
        <Popconfirm
          title="确定要删除这一行吗？"
          onConfirm={() => handleDeleteRow(record.rowIndex)}
          okText="确定"
          cancelText="取消"
        >
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            size="small"
            disabled={record.rowStatus === 'DELETE'}
          >
            删除
          </Button>
        </Popconfirm>
      )
    })
  }

  const filteredRows = editableRows.filter(row => row.rowStatus !== 'DELETE')

  const getRowClassName = (record: EditableRowData) => {
    if (record.rowStatus === 'INSERT') return 'row-insert'
    if (record.rowStatus === 'DELETE') return 'row-delete'
    if (record.dirty) return 'row-update'
    return ''
  }

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleAddRow}
            disabled={!activeConfig}
          >
            新增行
          </Button>
          <span style={{ color: '#666', fontSize: 13 }}>
            {activeConfig ? `目标表: ${activeConfig.tableName}` : '请先配置回写规则'}
          </span>
        </Space>
        <Space>
          {dirtyCount > 0 && (
            <span style={{ color: '#faad14', fontSize: 13 }}>
              有 {dirtyCount} 条数据待提交
            </span>
          )}
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={handleSubmit}
            loading={submitting}
            disabled={!activeConfig || dirtyCount === 0}
          >
            提交数据
          </Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={filteredRows}
        rowKey="rowIndex"
        loading={loading || submitting}
        pagination={{
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条记录`
        }}
        rowClassName={getRowClassName}
        scroll={{ x: 1200 }}
      />

      <style>{`
        .row-insert {
          background: #f6ffed !important;
        }
        .row-insert:hover > td {
          background: #d9f7be !important;
        }
        .row-update {
          background: #e6f7ff !important;
        }
        .row-update:hover > td {
          background: #bae7ff !important;
        }
        .row-delete {
          background: #fff1f0 !important;
          text-decoration: line-through;
        }
        .row-delete:hover > td {
          background: #ffccc7 !important;
        }
      `}</style>
    </div>
  )
}

export default EditableTable
