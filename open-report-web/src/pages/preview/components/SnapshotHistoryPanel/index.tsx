import React, { useState, useMemo } from 'react'
import {
  Drawer,
  List,
  Tag,
  Button,
  Space,
  Typography,
  Empty,
  Divider,
  Tooltip,
  Modal,
  message,
  Descriptions,
  Statistic,
  Row,
  Col,
  Spin,
  Badge,
  Input,
  Select,
  DatePicker
} from 'antd'
import {
  DeleteOutlined,
  PlayCircleOutlined,
  ClockCircleOutlined,
  BarChartOutlined,
  DatabaseOutlined,
  EyeOutlined,
  SearchOutlined
} from '@ant-design/icons'
import type { ReportDataSnapshot } from '@/types'
import { deleteSnapshot } from '@/api/report'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)

const { Title, Text } = Typography
const { RangePicker } = DatePicker

interface SnapshotHistoryPanelProps {
  visible: boolean
  snapshotList: ReportDataSnapshot[]
  loading: boolean
  selectedSnapshotId: number | null
  onClose: () => void
  onSelectSnapshot: (snapshotId: number) => void
  onCompare: (snapshotId: number) => void
  onRefresh: () => void
}

const formatBytes = (bytes: number) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getStatusInfo = (status: number) => {
  switch (status) {
    case 1:
      return { color: 'success', text: '成功' }
    case 0:
      return { color: 'processing', text: '生成中' }
    case -1:
      return { color: 'error', text: '失败' }
    default:
      return { color: 'default', text: '未知' }
  }
}

const SnapshotHistoryPanel: React.FC<SnapshotHistoryPanelProps> = ({
  visible,
  snapshotList,
  loading,
  selectedSnapshotId,
  onClose,
  onSelectSnapshot,
  onCompare,
  onRefresh
}) => {
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<string | undefined>()
  const [dateRange, setDateRange] = useState<any>()

  const filteredList = useMemo(() => {
    let list = [...snapshotList]
    if (keyword) {
      const kw = keyword.toLowerCase()
      list = list.filter(s =>
        s.snapshotName?.toLowerCase().includes(kw) ||
        s.dataVersion?.toLowerCase().includes(kw)
      )
    }
    if (statusFilter !== undefined) {
      list = list.filter(s => s.status === Number(statusFilter))
    }
    if (dateRange && dateRange.length === 2) {
      const start = dayjs(dateRange[0]).startOf('day')
      const end = dayjs(dateRange[1]).endOf('day')
      list = list.filter(s => {
        const t = dayjs(s.createTime)
        return t.isAfter(start) && t.isBefore(end)
      })
    }
    return list
  }, [snapshotList, keyword, statusFilter, dateRange])

  const handleDelete = (snapshot: ReportDataSnapshot) => {
    Modal.confirm({
      title: '确认删除快照',
      content: `确定要删除快照 "${snapshot.snapshotName}" 吗？`,
      okText: '删除',
      okType: 'danger',
      onOk: async () => {
        try {
          await deleteSnapshot(snapshot.id!)
          message.success('删除成功')
          onRefresh()
        } catch {
          message.error('删除失败')
        }
      }
    })
  }

  const stats = useMemo(() => {
    const total = snapshotList.length
    const success = snapshotList.filter(s => s.status === 1).length
    const totalRows = snapshotList.reduce((sum, s) => sum + (s.rowCount || 0), 0)
    const totalSize = snapshotList.reduce((sum, s) => sum + (s.dataSize || 0), 0)
    return { total, success, totalRows, totalSize }
  }, [snapshotList])

  return (
    <Drawer
      title={
        <Space>
          <DatabaseOutlined />
          <span>快照历史记录</span>
        </Space>
      }
      open={visible}
      onClose={onClose}
      width={520}
      extra={
        <Button size="small" onClick={onRefresh}>
          刷新
        </Button>
      }
    >
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={12}>
          <Statistic
            title="快照总数"
            value={stats.total}
            prefix={<DatabaseOutlined />}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="成功数量"
            value={stats.success}
            valueStyle={{ color: '#3f8600' }}
          />
        </Col>
      </Row>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={12}>
          <Statistic
            title="累计行数"
            value={stats.totalRows}
            precision={0}
          />
        </Col>
        <Col span={12}>
          <Statistic
            title="累计大小"
            value={formatBytes(stats.totalSize)}
            valueStyle={{ fontSize: 16 }}
          />
        </Col>
      </Row>

      <Divider style={{ margin: '8px 0 16px' }} />

      <Space direction="vertical" size={12} style={{ width: '100%', marginBottom: 16 }}>
        <Input
          prefix={<SearchOutlined />}
          placeholder="搜索快照名称/版本号"
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
          allowClear
        />
        <Space style={{ width: '100%' }}>
          <Select
            placeholder="状态筛选"
            allowClear
            style={{ width: 140 }}
            value={statusFilter}
            onChange={setStatusFilter}
            options={[
              { label: '成功', value: '1' },
              { label: '生成中', value: '0' },
              { label: '失败', value: '-1' }
            ]}
          />
          <RangePicker
            style={{ flex: 1 }}
            value={dateRange}
            onChange={setDateRange}
          />
        </Space>
      </Space>

      <Divider style={{ margin: '8px 0 16px' }} />

      <Spin spinning={loading}>
        {filteredList.length === 0 ? (
          <Empty description="暂无快照数据" />
        ) : (
          <List
            dataSource={filteredList}
            style={{ maxHeight: 'calc(100vh - 420px)', overflowY: 'auto' }}
            renderItem={(snapshot) => {
              const statusInfo = getStatusInfo(snapshot.status || 0)
              const isExpired = snapshot.expireTime && dayjs(snapshot.expireTime).isBefore(dayjs())
              const isSelected = selectedSnapshotId === snapshot.id

              return (
                <List.Item
                  key={snapshot.id}
                  style={{
                    padding: '12px 16px',
                    border: isSelected ? '2px solid #1677ff' : '1px solid #f0f0f0',
                    borderRadius: 8,
                    marginBottom: 8,
                    background: isSelected ? '#e6f4ff' : '#fff',
                    cursor: 'pointer'
                  }}
                  onClick={() => snapshot.status === 1 && onSelectSnapshot(snapshot.id!)}
                >
                  <List.Item.Meta
                    avatar={
                      <Badge status={statusInfo.color as any} text={null}>
                        <div
                          style={{
                            width: 40,
                            height: 40,
                            borderRadius: 8,
                            background: snapshot.status === 1 ? '#f6ffed' : '#fff2f0',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                          }}
                        >
                          <ClockCircleOutlined
                            style={{
                              fontSize: 20,
                              color: snapshot.status === 1 ? '#52c41a' : '#ff4d4f'
                            }}
                          />
                        </div>
                      </Badge>
                    }
                    title={
                      <Space>
                        <Text strong>{snapshot.snapshotName}</Text>
                        {isSelected && <Tag color="blue">当前选择</Tag>}
                        {isExpired && <Tag color="orange">已过期</Tag>}
                        <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
                      </Space>
                    }
                    description={
                      <div>
                        <Space direction="vertical" size={4} style={{ width: '100%' }}>
                          <Space size={16} wrap>
                            <Text type="secondary">
                              版本: <Tag color="cyan">{snapshot.dataVersion}</Tag>
                            </Text>
                            <Text type="secondary">
                              行数: <Text strong>{(snapshot.rowCount || 0).toLocaleString()}</Text>
                            </Text>
                            <Text type="secondary">
                              大小: <Text strong>{formatBytes(snapshot.dataSize || 0)}</Text>
                            </Text>
                            <Text type="secondary">
                              耗时: <Text strong>{((snapshot.executeTime || 0) / 1000).toFixed(2)}s</Text>
                            </Text>
                          </Space>
                          <Space size={16} wrap>
                            <Text type="secondary">
                              创建: {dayjs(snapshot.createTime).format('YYYY-MM-DD HH:mm:ss')}
                              <Text type="warning"> ({dayjs(snapshot.createTime).fromNow()})</Text>
                            </Text>
                            {snapshot.expireTime && (
                              <Text type="secondary">
                                过期: {dayjs(snapshot.expireTime).format('YYYY-MM-DD HH:mm')}
                              </Text>
                            )}
                          </Space>
                          <Space size={4}>
                            <Tooltip title={snapshot.createByName && `创建者: ${snapshot.createByName}`}>
                              <Text type="secondary" style={{ fontSize: 12 }}>
                                {snapshot.createByName ? `创建者: ${snapshot.createByName}` : ''}
                              </Text>
                            </Tooltip>
                          </Space>
                        </Space>
                      </div>
                    }
                  />
                  {snapshot.status === 1 && (
                    <Space size={4} onClick={e => e.stopPropagation()}>
                      <Tooltip title="加载此快照">
                        <Button
                          type="primary"
                          size="small"
                          icon={<PlayCircleOutlined />}
                          onClick={() => onSelectSnapshot(snapshot.id!)}
                        >
                          加载
                        </Button>
                      </Tooltip>
                      <Tooltip title="与实时数据对比">
                        <Button
                          size="small"
                          icon={<BarChartOutlined />}
                          onClick={() => onCompare(snapshot.id!)}
                        >
                          对比
                        </Button>
                      </Tooltip>
                      <Tooltip title="查看详情">
                        <Button
                          size="small"
                          icon={<EyeOutlined />}
                          onClick={() => {
                            Modal.info({
                              title: '快照详情',
                              width: 560,
                              content: (
                                <Descriptions column={2} size="small" bordered>
                                  <Descriptions.Item label="快照ID">{snapshot.id}</Descriptions.Item>
                                  <Descriptions.Item label="数据版本">{snapshot.dataVersion}</Descriptions.Item>
                                  <Descriptions.Item label="数据哈希" span={2}>
                                    <Text copyable style={{ fontSize: 12 }}>{snapshot.dataHash}</Text>
                                  </Descriptions.Item>
                                  <Descriptions.Item label="数据集数量">{snapshot.tableCount || 0}</Descriptions.Item>
                                  <Descriptions.Item label="行数">{(snapshot.rowCount?.toLocaleString() || 0}</Descriptions.Item>
                                  <Descriptions.Item label="数据大小">{formatBytes(snapshot.dataSize || 0)}</Descriptions.Item>
                                  <Descriptions.Item label="执行耗时">{((snapshot.executeTime || 0) / 1000).toFixed(2)}s</Descriptions.Item>
                                  <Descriptions.Item label="创建时间" span={2}>
                                    {dayjs(snapshot.createTime).format('YYYY-MM-DD HH:mm:ss')}
                                  </Descriptions.Item>
                                  <Descriptions.Item label="过期时间" span={2}>
                                    {snapshot.expireTime ? dayjs(snapshot.expireTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
                                  </Descriptions.Item>
                                  {snapshot.errorMsg && (
                                    <Descriptions.Item label="错误信息" span={2}>
                                      <Text type="danger">{snapshot.errorMsg}</Text>
                                    </Descriptions.Item>
                                  )}
                                </Descriptions>
                              )
                            })
                          }}
                        />
                      </Tooltip>
                      <Tooltip title="删除快照">
                        <Button
                          size="small"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={() => handleDelete(snapshot)}
                        />
                      </Tooltip>
                    </Space>
                  )}
                </List.Item>
              )
            }}
          />
        )}
      </Spin>
    </Drawer>
  )
}

export default SnapshotHistoryPanel
