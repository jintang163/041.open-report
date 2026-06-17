import React, { useState, useEffect } from 'react'
import { Modal, Table, Tag, Space, Button, Descriptions, Collapse, Badge } from 'antd'
import { HistoryOutlined, EyeOutlined } from '@ant-design/icons'
import { getWritebackHistoryList, getWritebackDetail } from '@/api/writeback'
import type { WritebackHistory, WritebackDetail, SubmitStatus } from '@/types'
import type { ColumnsType } from 'antd/es/table'

interface WritebackHistoryModalProps {
  reportId: number
  visible: boolean
  onClose: () => void
}

const WritebackHistoryModal: React.FC<WritebackHistoryModalProps> = ({
  reportId,
  visible,
  onClose
}) => {
  const [histories, setHistories] = useState<WritebackHistory[]>([])
  const [loading, setLoading] = useState(false)
  const [detailVisible, setDetailVisible] = useState(false)
  const [currentHistory, setCurrentHistory] = useState<WritebackHistory | null>(null)
  const [details, setDetails] = useState<WritebackDetail[]>([])
  const [detailLoading, setDetailLoading] = useState(false)

  useEffect(() => {
    if (visible && reportId) {
      loadHistories()
    }
  }, [visible, reportId])

  const loadHistories = async () => {
    setLoading(true)
    try {
      const data = await getWritebackHistoryList(reportId, 1, 100)
      setHistories(data.list || [])
    } catch (error) {
      console.error('加载历史记录失败:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadDetails = async (history: WritebackHistory) => {
    setCurrentHistory(history)
    setDetailLoading(true)
    try {
      const data = await getWritebackDetail(history.id)
      setDetails(data || [])
      setDetailVisible(true)
    } catch (error) {
      console.error('加载明细失败:', error)
    } finally {
      setDetailLoading(false)
    }
  }

  const getStatusTag = (status: SubmitStatus) => {
    const statusMap: Record<SubmitStatus, { color: string; text: string }> = {
      SUCCESS: { color: 'green', text: '成功' },
      PARTIAL: { color: 'orange', text: '部分成功' },
      FAILURE: { color: 'red', text: '失败' }
    }
    const config = statusMap[status] || { color: 'default', text: status }
    return <Tag color={config.color}>{config.text}</Tag>
  }

  const getRowStatusTag = (status: string) => {
    const statusMap: Record<string, { color: string; text: string }> = {
      INSERT: { color: 'green', text: '新增' },
      UPDATE: { color: 'blue', text: '修改' },
      DELETE: { color: 'red', text: '删除' }
    }
    const config = statusMap[status] || { color: 'default', text: status }
    return <Tag color={config.color}>{config.text}</Tag>
  }

  const historyColumns: ColumnsType<WritebackHistory> = [
    {
      title: '批次号',
      dataIndex: 'batchNo',
      key: 'batchNo',
      width: 180,
      render: (text) => <code>{text}</code>
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: SubmitStatus) => getStatusTag(status)
    },
    {
      title: '提交人',
      dataIndex: 'createBy',
      key: 'createBy',
      width: 100
    },
    {
      title: '提交时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180
    },
    {
      title: '统计',
      key: 'stats',
      width: 200,
      render: (_: any, record) => (
        <Space size="small">
          <Badge count={record.successCount} style={{ background: '#52c41a' }} title="成功" />
          {record.failCount > 0 && (
            <Badge count={record.failCount} style={{ background: '#ff4d4f' }} title="失败" />
          )}
          <span style={{ color: '#666' }}>共 {record.totalCount} 条</span>
        </Space>
      )
    },
    {
      title: '执行时长',
      dataIndex: 'executionTime',
      key: 'executionTime',
      width: 100,
      render: (time) => time ? `${time}ms` : '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => loadDetails(record)}
        >
          查看明细
        </Button>
      )
    }
  ]

  const renderDataDiff = (oldData: Record<string, any> | null, newData: Record<string, any> | null) => {
    if (!oldData && !newData) return <span style={{ color: '#999' }}>无数据</span>

    const keys = new Set([
      ...Object.keys(oldData || {}),
      ...Object.keys(newData || {})
    ])

    return (
      <div style={{ fontSize: 12 }}>
        {[...keys].map(key => {
          const oldVal = oldData?.[key]
          const newVal = newData?.[key]
          const changed = JSON.stringify(oldVal) !== JSON.stringify(newVal)

          return (
            <div
              key={key}
              style={{
                padding: '2px 4px',
                marginBottom: 2,
                background: changed ? '#fff7e6' : 'transparent',
                borderRadius: 2
              }}
            >
              <span style={{ color: '#666', marginRight: 8 }}>{key}:</span>
              {oldVal !== undefined && oldVal !== null && (
                <span style={{ color: '#ff4d4f', textDecoration: 'line-through', marginRight: 8 }}>
                  {String(oldVal)}
                </span>
              )}
              {newVal !== undefined && newVal !== null && (
                <span style={{ color: '#52c41a' }}>
                  {String(newVal)}
                </span>
              )}
            </div>
          )
        })}
      </div>
    )
  }

  const detailColumns: ColumnsType<WritebackDetail> = [
    {
      title: '行号',
      dataIndex: 'rowIndex',
      key: 'rowIndex',
      width: 80
    },
    {
      title: '操作类型',
      dataIndex: 'rowStatus',
      key: 'rowStatus',
      width: 100,
      render: (status: string) => getRowStatusTag(status)
    },
    {
      title: '执行状态',
      dataIndex: 'executeStatus',
      key: 'executeStatus',
      width: 100,
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '成功' : '失败'}
        </Tag>
      )
    },
    {
      title: '数据变化',
      key: 'dataDiff',
      render: (_: any, record) => renderDataDiff(record.oldData, record.newData)
    },
    {
      title: '错误信息',
      dataIndex: 'errorMessage',
      key: 'errorMessage',
      width: 200,
      ellipsis: true,
      render: (text) => text ? (
        <span style={{ color: '#ff4d4f' }}>{text}</span>
      ) : '-',
    }
  ]

  return (
    <>
      <Modal
        title={
          <Space>
            <HistoryOutlined />
            <span>数据提交历史</span>
          </Space>
        }
        open={visible}
        onCancel={onClose}
        width={900}
        footer={null}
      >
        <Table
          columns={historyColumns}
          dataSource={histories}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showTotal: (total) => `共 ${total} 条记录`
          }}
        />
      </Modal>

      <Modal
        title={
          <Space>
            <span>提交明细</span>
            <code>{currentHistory?.batchNo}</code>
            {currentHistory && getStatusTag(currentHistory.status)}
          </Space>
        }
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        width={1000}
        footer={null}
      >
        {currentHistory && (
          <Collapse
            defaultActiveKey={['basic', 'details']}
            style={{ marginBottom: 16 }}
          >
            <Collapse.Panel header="基本信息" key="basic">
              <Descriptions column={2} size="small">
                <Descriptions.Item label="批次号">{currentHistory.batchNo}</Descriptions.Item>
                <Descriptions.Item label="提交人">{currentHistory.createBy}</Descriptions.Item>
                <Descriptions.Item label="提交时间">{currentHistory.createTime}</Descriptions.Item>
                <Descriptions.Item label="执行时长">{currentHistory.executionTime}ms</Descriptions.Item>
                <Descriptions.Item label="总记录数">{currentHistory.totalCount}</Descriptions.Item>
                <Descriptions.Item label="成功数">{currentHistory.successCount}</Descriptions.Item>
                <Descriptions.Item label="失败数">{currentHistory.failCount}</Descriptions.Item>
                <Descriptions.Item label="表名">{currentHistory.tableName}</Descriptions.Item>
              </Descriptions>
            </Collapse.Panel>
            <Collapse.Panel header="执行明细" key="details">
              <Table
                columns={detailColumns}
                dataSource={details}
                rowKey="id"
                loading={detailLoading}
                pagination={{
                  pageSize: 10,
                  showTotal: (total) => `共 ${total} 条记录`
                }}
                scroll={{ x: 800 }}
              />
            </Collapse.Panel>
          </Collapse>
        )}
      </Modal>
    </>
  )
}

export default WritebackHistoryModal
