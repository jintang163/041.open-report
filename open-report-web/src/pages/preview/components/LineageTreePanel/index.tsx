import React, { useEffect, useState } from 'react'
import { Tree, Card, Tag, Space, Button, Spin, Empty, Tooltip, Typography } from 'antd'
import {
  DatabaseOutlined,
  TableOutlined,
  ColumnHeightOutlined,
  FileTextOutlined,
  ApiOutlined,
  ReloadOutlined,
  InfoCircleOutlined
} from '@ant-design/icons'
import { getLineageTree, refreshLineageForReport } from '@/api/report'
import type { LineageTreeNode } from '@/types'

const { Text } = Typography

interface LineageTreePanelProps {
  reportId: number
  reportName?: string
  onFieldClick?: (fieldName: string) => void
}

const LineageTreePanel: React.FC<LineageTreePanelProps> = ({
  reportId,
  reportName,
  onFieldClick
}) => {
  const [loading, setLoading] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [treeData, setTreeData] = useState<LineageTreeNode | null>(null)
  const [stats, setStats] = useState({ lineageCount: 0, dataSetCount: 0, tableCount: 0 })

  useEffect(() => {
    if (reportId) {
      loadLineageTree()
    }
  }, [reportId])

  const loadLineageTree = async () => {
    if (!reportId) return
    setLoading(true)
    try {
      const result = await getLineageTree(reportId)
      if (result?.success && result.tree) {
        setTreeData(result.tree)
        setStats({
          lineageCount: result.lineageCount || 0,
          dataSetCount: result.dataSetCount || 0,
          tableCount: result.tableCount || 0
        })
      }
    } catch (err: any) {
      console.error('加载血缘树失败:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleRefresh = async () => {
    setRefreshing(true)
    try {
      await refreshLineageForReport(reportId)
      await loadLineageTree()
    } catch (err: any) {
      console.error('刷新血缘失败:', err)
    } finally {
      setRefreshing(false)
    }
  }

  const getNodeIcon = (type: string) => {
    switch (type) {
      case 'report':
        return <FileTextOutlined style={{ color: '#1890ff' }} />
      case 'dataSet':
        return <ApiOutlined style={{ color: '#52c41a' }} />
      case 'table':
        return <TableOutlined style={{ color: '#fa8c16' }} />
      case 'column':
        return <ColumnHeightOutlined style={{ color: '#722ed1' }} />
      case 'reportField':
        return <DatabaseOutlined style={{ color: '#13c2c2' }} />
      default:
        return <InfoCircleOutlined />
    }
  }

  const getNodeColor = (type: string) => {
    switch (type) {
      case 'report':
        return 'blue'
      case 'dataSet':
        return 'green'
      case 'table':
        return 'orange'
      case 'column':
        return 'purple'
      case 'reportField':
        return 'cyan'
      default:
        return 'default'
    }
  }

  const renderTreeNode = (node: LineageTreeNode): React.ReactNode => {
    const isLeaf = !node.children || node.children.length === 0

    const title = (
      <Space size={4}>
        {getNodeIcon(node.type)}
        <Text strong={node.type === 'report'}>{node.name}</Text>
        {node.type === 'reportField' && node.title && node.title !== node.name && (
          <Text type="secondary" style={{ fontSize: 12 }}>
            ({node.title})
          </Text>
        )}
        {node.lineageType && node.lineageType !== 'DIRECT' && (
          <Tag color={node.lineageType === 'AGGREGATION' ? 'red' : 'gold'} size="small">
            {node.lineageType === 'AGGREGATION' ? '聚合' : '表达式'}
          </Tag>
        )}
        {node.expression && node.type === 'reportField' && (
          <Tooltip title={`表达式: ${node.expression}`}>
            <InfoCircleOutlined style={{ color: '#8c8c8c', fontSize: 12 }} />
          </Tooltip>
        )}
      </Space>
    )

    const key = node.id

    return {
      key,
      title,
      isLeaf,
      icon: getNodeIcon(node.type),
      children: node.children?.map(child => renderTreeNode(child as LineageTreeNode)),
      data: node
    }
  }

  const handleSelect = (selectedKeys: React.Key[], info: any) => {
    if (selectedKeys.length > 0 && info.node?.data) {
      const node = info.node.data as LineageTreeNode
      if (node.type === 'reportField' && node.field && onFieldClick) {
        onFieldClick(node.field)
      }
    }
  }

  const treeNodes = treeData ? [renderTreeNode(treeData)] : []

  return (
    <Card
      title={
        <Space>
          <DatabaseOutlined style={{ color: '#1890ff' }} />
          <span>数据血缘关系</span>
          {stats.lineageCount > 0 && (
            <Space size={4}>
              <Tag color="blue">{stats.lineageCount} 个字段</Tag>
              <Tag color="green">{stats.dataSetCount} 个数据集</Tag>
              <Tag color="orange">{stats.tableCount} 个表</Tag>
            </Space>
          )}
        </Space>
      }
      extra={
        <Button
          icon={<ReloadOutlined spin={refreshing} />}
          size="small"
          onClick={handleRefresh}
        >
          刷新血缘
        </Button>
      }
      style={{ height: '100%' }}
      bodyStyle={{ padding: 16, height: 'calc(100% - 57px)', overflow: 'auto' }}
    >
      <Spin spinning={loading}>
        {treeData ? (
          <Tree
            showLine
            showIcon
            defaultExpandAll
            treeData={treeNodes as any}
            onSelect={handleSelect}
            blockNode
          />
        ) : (
          <Empty
            description="暂无血缘数据"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button type="primary" onClick={handleRefresh}>
              生成血缘关系
            </Button>
          </Empty>
        )}
      </Spin>
    </Card>
  )
}

export default LineageTreePanel
