import React, { useEffect, useMemo, useState } from 'react'
import {
  Modal,
  Tabs,
  Descriptions,
  Statistic,
  Row,
  Col,
  Tag,
  Empty,
  Spin,
  Alert,
  Table,
  Space,
  Typography,
  Card,
  Button,
  message
} from 'antd'
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  MinusOutlined,
  SwapOutlined,
  CloudServerOutlined,
  DatabaseOutlined,
  DownloadOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import type { SnapshotComparisonResult } from '@/types'
import dayjs from 'dayjs'

const { Title, Text } = Typography

interface SnapshotCompareModalProps {
  visible: boolean
  loading: boolean
  result: SnapshotComparisonResult | null
  mode: 'snapshot-snapshot' | 'snapshot-realtime'
  onClose: () => void
  onRefresh?: () => void
}

const formatBytes = (bytes: number) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getDiffTag = (diff: number) => {
  if (diff > 0) {
    return <Tag color="green"><ArrowUpOutlined /> +{diff.toLocaleString()}</Tag>
  } else if (diff < 0) {
    return <Tag color="red"><ArrowDownOutlined /> {diff.toLocaleString()}</Tag>
  }
  return <Tag><MinusOutlined /> 0</Tag>
}

const getDiffPercentTag = (percent: string) => {
  if (!percent || percent === 'N/A') return <Tag>N/A</Tag>
  const num = parseFloat(percent)
  if (num > 0) {
    return <Tag color="green">+{percent}</Tag>
  } else if (num < 0) {
    return <Tag color="red">{percent}</Tag>
  }
  return <Tag>0%</Tag>
}

const SnapshotCompareModal: React.FC<SnapshotCompareModalProps> = ({
  visible,
  loading,
  result,
  mode,
  onClose,
  onRefresh
}) => {
  const [activeTableIdx, setActiveTableIdx] = useState(0)

  useEffect(() => {
    setActiveTableIdx(0)
  }, [visible, result])

  const summary = result?.summary
  const tablesComparison = result?.tablesComparison || []
  const currentTable = tablesComparison[activeTableIdx]

  const baseInfo = useMemo(() => {
    if (mode === 'snapshot-realtime') {
      return {
        name: result?.baseSnapshot?.name || '快照数据',
        time: result?.baseSnapshot?.createTime || '',
        rows: result?.summary?.snapshotRowCount || 0,
        dataSize: result?.baseSnapshot?.dataSize || 0,
        version: result?.baseSnapshot?.dataVersion
      }
    }
    return {
      name: result?.baseSnapshot?.name || '基准快照',
      time: result?.baseSnapshot?.createTime || '',
      rows: result?.summary?.baseRowCount || 0,
      dataSize: result?.summary?.baseDataSize || 0,
      version: result?.baseSnapshot?.dataVersion
    }
  }, [result, mode])

  const targetInfo = useMemo(() => {
    if (mode === 'snapshot-realtime') {
      return {
        name: result?.realtimeInfo?.name || '实时数据',
        time: result?.realtimeInfo?.time || '',
        rows: result?.summary?.realtimeRowCount || 0,
        dataSize: 0,
        version: '-'
      }
    }
    return {
      name: result?.targetSnapshot?.name || '对比快照',
      time: result?.targetSnapshot?.createTime || '',
      rows: result?.summary?.targetRowCount || 0,
      dataSize: result?.summary?.targetDataSize || 0,
      version: result?.targetSnapshot?.dataVersion
    }
  }, [result, mode])

  const chartOption = useMemo(() => {
    if (!currentTable?.chartData || currentTable.chartData.length === 0) {
      return null
    }

    const baseData: any[] = []
    const targetData: any[] = []
    const xAxisData: string[] = []

    const baseMap = new Map<string, number>()
    const targetMap = new Map<string, number>()

    for (const item of currentTable.chartData) {
      if (item.category === 'base') {
        baseMap.set(item.x, Number(item.value || 0))
      } else {
        targetMap.set(item.x, Number(item.value || 0))
      }
      if (!xAxisData.includes(String(item.x))) {
        xAxisData.push(String(item.x))
      }
    }

    for (const x of xAxisData) {
      baseData.push(baseMap.get(x) || 0)
      targetData.push(targetMap.get(x) || 0)
    }

    return {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      legend: {
        data: [baseInfo.name, targetInfo.name],
        top: 0
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: 40,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: xAxisData,
        axisLabel: {
          rotate: xAxisData.length > 10 ? 30 : 0,
          interval: 0,
          fontSize: 11
        }
      },
      yAxis: {
        type: 'value',
        name: currentTable.yField || '数值'
      },
      dataZoom: xAxisData.length > 15 ? [
        { type: 'slider', start: 0, end: 100, height: 20, bottom: 5 }
      ] : undefined,
      series: [
        {
          name: baseInfo.name,
          type: 'bar',
          data: baseData,
          itemStyle: { color: '#5470c6' },
          barMaxWidth: 40
        },
        {
          name: targetInfo.name,
          type: 'bar',
          data: targetData,
          itemStyle: { color: '#91cc75' },
          barMaxWidth: 40
        }
      ]
    }
  }, [currentTable, baseInfo.name, targetInfo.name])

  const diffTableData = useMemo(() => {
    return tablesComparison.map((t, idx) => ({
      key: idx,
      bindName: t.bindName,
      baseRows: t.baseRows,
      targetRows: t.targetRows,
      rowDiff: t.rowDiff,
      rowDiffPercent: t.rowDiffPercent,
      baseCols: t.baseCols,
      targetCols: t.targetCols,
      colDiff: t.colDiff
    }))
  }, [tablesComparison])

  const diffTableColumns = [
    {
      title: '数据集',
      dataIndex: 'bindName',
      key: 'bindName',
      width: 160,
      render: (v: string) => <Text strong>{v}</Text>
    },
    {
      title: mode === 'snapshot-realtime' ? '快照行数' : '基准快照行数',
      dataIndex: 'baseRows',
      key: 'baseRows',
      align: 'right' as const,
      render: (v: number) => v.toLocaleString()
    },
    {
      title: mode === 'snapshot-realtime' ? '实时行数' : '对比快照行数',
      dataIndex: 'targetRows',
      key: 'targetRows',
      align: 'right' as const,
      render: (v: number) => v.toLocaleString()
    },
    {
      title: '行数差异',
      dataIndex: 'rowDiff',
      key: 'rowDiff',
      align: 'right' as const,
      render: (v: number) => getDiffTag(v)
    },
    {
      title: '变化率',
      dataIndex: 'rowDiffPercent',
      key: 'rowDiffPercent',
      align: 'right' as const,
      render: (v: string) => getDiffPercentTag(v)
    },
    {
      title: '列数差异',
      dataIndex: 'colDiff',
      key: 'colDiff',
      align: 'right' as const,
      render: (v: number) => getDiffTag(v)
    }
  ]

  const handleExportCSV = () => {
    if (!diffTableData.length) return
    const headers = ['数据集', '基准快照行数', '对比快照行数', '行数差异', '变化率', '列数差异']
    const rows = diffTableData.map(d => [
      d.bindName,
      d.baseRows,
      d.targetRows,
      d.rowDiff,
      d.rowDiffPercent,
      d.colDiff
    ])
    const csv = [headers, ...rows].map(r => r.join(',')).join('\n')
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `snapshot_compare_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  }

  const renderDiffIcon = () => {
    const rowDiff = summary?.rowDiff ?? summary?.totalRowDiff ?? 0
    if (rowDiff > 0) return <ArrowUpOutlined style={{ color: '#52c41a' }} />
    if (rowDiff < 0) return <ArrowDownOutlined style={{ color: '#ff4d4f' }} />
    return <MinusOutlined />
  }

  return (
    <Modal
      title={
        <Space>
          <SwapOutlined />
          <span>{mode === 'snapshot-realtime' ? '快照 vs 实时数据对比' : '快照对比'}</span>
        </Space>
      }
      open={visible}
      onCancel={onClose}
      width={1100}
      footer={[
        <Space key="footer">
          <Button icon={<DownloadOutlined />} onClick={handleExportCSV} disabled={!diffTableData.length}>
            导出对比结果
          </Button>
          {onRefresh && (
            <Button onClick={onRefresh}>
              重新对比
            </Button>
          )}
          <Button type="primary" onClick={onClose}>
            关闭
          </Button>
        </Space>
      ]}
    >
      <Spin spinning={loading}>
        {!result?.success && !loading ? (
          <Alert
            type="error"
            message="对比失败"
            description={result?.message || '未知错误'}
            showIcon
          />
        ) : !result ? (
          <Empty description="暂无对比数据" />
        ) : (
          <>
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={11}>
                <Card
                  size="small"
                  title={
                    <Space>
                      <DatabaseOutlined style={{ color: '#5470c6' }} />
                      <span>{baseInfo.name}</span>
                    </Space>
                  }
                  style={{ borderColor: '#5470c6', borderWidth: 2 }}
                >
                  <Descriptions column={2} size="small">
                    <Descriptions.Item label="数据版本">
                      <Tag color="cyan">{baseInfo.version}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="数据行数">
                      <Text strong>{baseInfo.rows.toLocaleString()}</Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="时间">
                      {baseInfo.time ? dayjs(baseInfo.time).format('YYYY-MM-DD HH:mm:ss') : '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="数据大小">
                      {baseInfo.dataSize ? formatBytes(baseInfo.dataSize) : '-'}
                    </Descriptions.Item>
                  </Descriptions>
                </Card>
              </Col>
              <Col span={2} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 32, marginBottom: 4 }}>{renderDiffIcon()}</div>
                  {summary?.dataHashChanged !== undefined && (
                    <Tag color={summary.dataHashChanged ? 'orange' : 'green'}>
                      {summary.dataHashChanged ? '数据有变化' : '数据一致'}
                    </Tag>
                  )}
                </div>
              </Col>
              <Col span={11}>
                <Card
                  size="small"
                  title={
                    <Space>
                      <CloudServerOutlined style={{ color: '#91cc75' }} />
                      <span>{targetInfo.name}</span>
                    </Space>
                  }
                  style={{ borderColor: '#91cc75', borderWidth: 2 }}
                >
                  <Descriptions column={2} size="small">
                    <Descriptions.Item label="数据版本">
                      <Tag color="green">{targetInfo.version}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="数据行数">
                      <Text strong>{targetInfo.rows.toLocaleString()}</Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="时间">
                      {targetInfo.time ? dayjs(targetInfo.time).format('YYYY-MM-DD HH:mm:ss') : '-'}
                    </Descriptions.Item>
                    <Descriptions.Item label="数据大小">
                      {targetInfo.dataSize ? formatBytes(targetInfo.dataSize) : '-'}
                    </Descriptions.Item>
                  </Descriptions>
                </Card>
              </Col>
            </Row>

            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={8}>
                <Card size="small">
                  <Statistic
                    title="总行数差异"
                    value={summary?.rowDiff ?? summary?.totalRowDiff ?? 0}
                    precision={0}
                    valueStyle={{
                      color: (summary?.rowDiff ?? summary?.totalRowDiff ?? 0) >= 0 ? '#3f8600' : '#cf1322'
                    }}
                    prefix={(summary?.rowDiff ?? summary?.totalRowDiff ?? 0) >= 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
                  />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic
                    title="变化率"
                    value={summary?.rowDiffPercent || 'N/A'}
                    valueStyle={{ fontSize: 22 }}
                  />
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small">
                  <Statistic
                    title="时间间隔"
                    value={summary?.hoursDiff !== undefined && summary.hoursDiff >= 0
                      ? `${summary.hoursDiff} 小时`
                      : (mode === 'snapshot-realtime' ? '实时' : '-')}
                    valueStyle={{ fontSize: 22 }}
                  />
                </Card>
              </Col>
            </Row>

            <Tabs
              defaultActiveKey="chart"
              items={[
                {
                  key: 'chart',
                  label: (
                    <Space>
                      <BarChartOutlined />
                      双柱图对比
                    </Space>
                  ),
                  children: (
                    <div>
                      {tablesComparison.length > 1 && (
                        <div style={{ marginBottom: 12 }}>
                          <Text type="secondary">选择数据集：</Text>
                          <Space wrap>
                            {tablesComparison.map((t, idx) => (
                              <Tag.CheckableTag
                                key={idx}
                                checked={activeTableIdx === idx}
                                onChange={() => setActiveTableIdx(idx)}
                              >
                                {t.bindName}
                              </Tag.CheckableTag>
                            ))}
                          </Space>
                        </div>
                      )}
                      {chartOption ? (
                        <Card size="small">
                          <Title level={5} style={{ margin: '0 0 12px' }}>
                            {currentTable?.bindName} - 维度: {currentTable?.xField || '-'} / 指标: {currentTable?.yField || '-'}
                          </Title>
                          <ReactECharts
                            option={chartOption}
                            style={{ height: 400 }}
                            notMerge
                            lazyUpdate
                          />
                        </Card>
                      ) : (
                        <Empty description="暂无图表数据，数据行数不足或缺少可聚合的数值字段" />
                      )}
                    </div>
                  )
                },
                {
                  key: 'table',
                  label: (
                    <Space>
                      <DatabaseOutlined />
                      数据集对比明细
                    </Space>
                  ),
                  children: (
                    <Table
                      size="small"
                      columns={diffTableColumns}
                      dataSource={diffTableData}
                      pagination={false}
                      scroll={{ x: 800 }}
                    />
                  )
                }
              ]}
            />
          </>
        )}
      </Spin>
    </Modal>
  )
}

export default SnapshotCompareModal
