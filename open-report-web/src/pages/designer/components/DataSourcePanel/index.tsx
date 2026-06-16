import { useState } from 'react'
import { Tree, Input, Empty, Typography, Tag, Tooltip } from 'antd'
import {
  DatabaseOutlined,
  TableOutlined,
  FieldStringOutlined,
  FieldNumberOutlined,
  FieldTimeOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import type { DataNode } from 'antd/es/tree'
import { useDesignerStore, type DataSourceWithDataSets, type FieldItem } from '../../store/designer'

const { Text } = Typography

interface DragFieldData {
  datasetName: string
  fieldName: string
  fieldType: string
  fieldLabel?: string
}

const getFieldIcon = (type: string) => {
  const t = type.toLowerCase()
  if (t.includes('int') || t.includes('number') || t.includes('float') || t.includes('double') || t.includes('decimal')) {
    return <FieldNumberOutlined style={{ color: '#52c41a' }} />
  }
  if (t.includes('date') || t.includes('time')) {
    return <FieldTimeOutlined style={{ color: '#1677ff' }} />
  }
  return <FieldStringOutlined style={{ color: '#fa8c16' }} />
}

const getFieldTypeColor = (type: string) => {
  const t = type.toLowerCase()
  if (t.includes('int') || t.includes('number') || t.includes('float') || t.includes('double') || t.includes('decimal')) {
    return 'green'
  }
  if (t.includes('date') || t.includes('time')) {
    return 'blue'
  }
  if (t.includes('bool')) {
    return 'purple'
  }
  return 'orange'
}

const DataSourcePanel: React.FC = () => {
  const {
    dataSources,
    expandedDataSourceKeys,
    expandedDataSetKeys,
    toggleDataSourceExpand,
    toggleDataSetExpand
  } = useDesignerStore()

  const [searchKeyword, setSearchKeyword] = useState('')

  const filterFields = (fields: FieldItem[]): FieldItem[] => {
    if (!searchKeyword) return fields
    const keyword = searchKeyword.toLowerCase()
    return fields.filter(
      (f) =>
        f.name.toLowerCase().includes(keyword) ||
        (f.label && f.label.toLowerCase().includes(keyword)) ||
        f.type.toLowerCase().includes(keyword)
    )
  }

  const buildTreeData = (): DataNode[] => {
    return dataSources
      .filter((ds) => {
        if (!searchKeyword) return true
        return ds.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
          ds.dataSets?.some((dset) =>
            dset.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
            dset.fields?.some((f) =>
              f.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
              (f.label && f.label.toLowerCase().includes(searchKeyword.toLowerCase()))
            )
          )
      })
      .map((ds: DataSourceWithDataSets) => ({
        key: `ds-${ds.id}`,
        title: (
          <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <DatabaseOutlined style={{ color: '#1677ff' }} />
            <Text strong>{ds.name}</Text>
            <Tag color="blue" style={{ marginLeft: 'auto' }}>{ds.type}</Tag>
          </span>
        ),
        icon: <DatabaseOutlined />,
        children: (ds.dataSets || [])
          .filter((dset) => {
            if (!searchKeyword) return true
            return dset.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
              dset.fields?.some((f) =>
                f.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
                (f.label && f.label.toLowerCase().includes(searchKeyword.toLowerCase()))
              )
          })
          .map((dset) => {
            const filteredFields = filterFields(dset.fields || [])
            return {
              key: `dataset-${dset.id}`,
              title: (
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <TableOutlined style={{ color: '#722ed1' }} />
                  <Text>{dset.name}</Text>
                  <Tag style={{ marginLeft: 'auto' }}>{dset.fields?.length || 0} 字段</Tag>
                </span>
              ),
              icon: <TableOutlined />,
              children: filteredFields.map((field) => ({
                key: `field-${dset.id}-${field.name}`,
                title: (
                  <Tooltip title={`${dset.name}.${field.name} (${field.type})`}>
                    <span
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 6,
                        cursor: 'grab',
                        userSelect: 'none',
                        padding: '2px 0'
                      }}
                      draggable
                      onDragStart={(e) => {
                        const dragData: DragFieldData = {
                          datasetName: dset.name,
                          fieldName: field.name,
                          fieldType: field.type,
                          fieldLabel: field.label
                        }
                        e.dataTransfer.setData('application/json', JSON.stringify(dragData))
                        e.dataTransfer.setData('text/plain', `\${${dset.name}.${field.name}}`)
                        e.dataTransfer.effectAllowed = 'copy'
                      }}
                    >
                      {getFieldIcon(field.type)}
                      <Text>{field.label || field.name}</Text>
                      <Tag
                        color={getFieldTypeColor(field.type)}
                        style={{ marginLeft: 'auto', fontSize: 11 }}
                      >
                        {field.type}
                      </Tag>
                    </span>
                  </Tooltip>
                ),
                isLeaf: true,
                draggable: true
              }))
            }
          })
      }))
  }

  const handleExpand = (expandedKeys: React.Key[]) => {
    expandedKeys.forEach((key) => {
      const keyStr = String(key)
      if (keyStr.startsWith('ds-')) {
        if (!expandedDataSourceKeys.includes(keyStr)) {
          toggleDataSourceExpand(keyStr)
        }
      } else if (keyStr.startsWith('dataset-')) {
        if (!expandedDataSetKeys.includes(keyStr)) {
          toggleDataSetExpand(keyStr)
        }
      }
    })

    ;[...expandedDataSourceKeys, ...expandedDataSetKeys].forEach((key) => {
      if (!expandedKeys.includes(key)) {
        if (key.startsWith('ds-')) {
          toggleDataSourceExpand(key)
        } else if (key.startsWith('dataset-')) {
          toggleDataSetExpand(key)
        }
      }
    })
  }

  const defaultExpandedKeys = [
    ...expandedDataSourceKeys,
    ...expandedDataSetKeys
  ]

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: 12, borderBottom: '1px solid #f0f0f0' }}>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
          <Text strong style={{ fontSize: 14 }}>数据源</Text>
          <Tooltip title="刷新">
            <ReloadOutlined style={{ marginLeft: 'auto', cursor: 'pointer', color: '#999' }} />
          </Tooltip>
        </div>
        <Input.Search
          placeholder="搜索数据源、数据集、字段"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          allowClear
          size="small"
        />
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '4px 8px' }}>
        {dataSources.length === 0 ? (
          <Empty
            description={<Text type="secondary">暂无数据源</Text>}
            style={{ marginTop: 40 }}
          />
        ) : (
          <Tree
            showLine={{ showLeafIcon: false }}
            showIcon
            defaultExpandAll
            expandedKeys={defaultExpandedKeys as any}
            onExpand={handleExpand}
            treeData={buildTreeData()}
            blockNode
            virtual
            height={600}
          />
        )}
      </div>

      <div
        style={{
          padding: '8px 12px',
          borderTop: '1px solid #f0f0f0',
          background: '#fafafa'
        }}
      >
        <Text type="secondary" style={{ fontSize: 12 }}>
          提示：拖拽字段到单元格可自动生成表达式
        </Text>
      </div>
    </div>
  )
}

export default DataSourcePanel
