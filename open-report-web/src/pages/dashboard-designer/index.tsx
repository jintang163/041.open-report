import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Button, Space, InputNumber, Modal, Form, Select, Input, message,
  Dropdown, ColorPicker, Tooltip, Spin
} from 'antd'
import {
  SaveOutlined, EyeOutlined, UndoOutlined, RedoOutlined,
  FullscreenOutlined, DeleteOutlined, SettingOutlined,
  ZoomInOutlined, ZoomOutOutlined, ArrowLeftOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import { useDashboardDesignerStore, CHART_PALETTE } from './store/designer'
import { buildChartOption } from './utils/chartOptions'
import { getDashboardDetail, createDashboard, updateDashboard, saveDashboardItems, getChartData } from '@/api/dashboard'
import { getDatasetAll } from '@/api/dataset'
import type { ChartDashboardItem, ChartType, DataSet } from '@/types'

const ChartPalette: React.FC = () => {
  const setDraggingType = useDashboardDesignerStore(s => s.setDraggingType)

  return (
    <div style={{ padding: 12 }}>
      <div style={{ color: '#e0e0e0', fontSize: 13, fontWeight: 600, marginBottom: 12 }}>图表组件</div>
      {CHART_PALETTE.map(item => (
        <div
          key={item.type}
          draggable
          onDragStart={() => setDraggingType(item.type)}
          onDragEnd={() => setDraggingType(null)}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '10px 12px',
            marginBottom: 6,
            background: 'rgba(255,255,255,0.06)',
            borderRadius: 6,
            cursor: 'grab',
            border: '1px solid rgba(255,255,255,0.08)',
            transition: 'all 0.2s',
            userSelect: 'none'
          }}
          onMouseEnter={e => {
            (e.currentTarget as HTMLDivElement).style.background = 'rgba(255,255,255,0.12)'
          }}
          onMouseLeave={e => {
            (e.currentTarget as HTMLDivElement).style.background = 'rgba(255,255,255,0.06)'
          }}
        >
          <span style={{ fontSize: 18 }}>{item.icon}</span>
          <span style={{ color: '#ccc', fontSize: 13 }}>{item.label}</span>
        </div>
      ))}
    </div>
  )
}

const ChartItemBox: React.FC<{
  item: ChartDashboardItem
  isSelected: boolean
  scale: number
  onSelect: () => void
  onMove: (dx: number, dy: number) => void
  onResize: (w: number, h: number) => void
  onDoubleClick: () => void
  chartData: Record<string, any>[]
  linkageFilter?: { field: string; value: any }
  onChartClick?: (params: any) => void
}> = ({ item, isSelected, scale, onSelect, onMove, onResize, onDoubleClick, chartData, linkageFilter, onChartClick }) => {
  const moveRef = useRef<{ startX: number; startY: number; originX: number; originY: number } | null>(null)
  const resizeRef = useRef<{ startX: number; startY: number; originW: number; originH: number } | null>(null)

  const option = useMemo(() =>
    buildChartOption(item.chartType, item.title, chartData, item.xField, item.yFields, linkageFilter),
    [item.chartType, item.title, chartData, item.xField, item.yFields, linkageFilter]
  )

  const handleMouseDown = (e: React.MouseEvent) => {
    e.stopPropagation()
    onSelect()
    moveRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      originX: item.positionX,
      originY: item.positionY
    }

    const handleMouseMove = (ev: MouseEvent) => {
      if (!moveRef.current) return
      const dx = (ev.clientX - moveRef.current.startX) / scale
      const dy = (ev.clientY - moveRef.current.startY) / scale
      onMove(moveRef.current.originX + dx, moveRef.current.originY + dy)
    }

    const handleMouseUp = () => {
      moveRef.current = null
      window.removeEventListener('mousemove', handleMouseMove)
      window.removeEventListener('mouseup', handleMouseUp)
    }

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseup', handleMouseUp)
  }

  const handleResizeMouseDown = (e: React.MouseEvent) => {
    e.stopPropagation()
    e.preventDefault()
    resizeRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      originW: item.width,
      originH: item.height
    }

    const handleMouseMove = (ev: MouseEvent) => {
      if (!resizeRef.current) return
      const dw = (ev.clientX - resizeRef.current.startX) / scale
      const dh = (ev.clientY - resizeRef.current.startY) / scale
      onResize(
        Math.max(200, resizeRef.current.originW + dw),
        Math.max(150, resizeRef.current.originH + dh)
      )
    }

    const handleMouseUp = () => {
      resizeRef.current = null
      window.removeEventListener('mousemove', handleMouseMove)
      window.removeEventListener('mouseup', handleMouseUp)
    }

    window.addEventListener('mousemove', handleMouseMove)
    window.addEventListener('mouseup', handleMouseUp)
  }

  return (
    <div
      style={{
        position: 'absolute',
        left: item.positionX,
        top: item.positionY,
        width: item.width,
        height: item.height,
        background: 'rgba(13,27,42,0.85)',
        border: isSelected ? '2px solid #1677ff' : '1px solid rgba(255,255,255,0.1)',
        borderRadius: 8,
        cursor: 'move',
        overflow: 'hidden',
        boxShadow: isSelected ? '0 0 12px rgba(22,119,255,0.3)' : '0 2px 8px rgba(0,0,0,0.3)'
      }}
      onMouseDown={handleMouseDown}
      onDoubleClick={onDoubleClick}
    >
      {item.title && (
        <div style={{
          padding: '6px 12px',
          color: '#e0e0e0',
          fontSize: 13,
          fontWeight: 600,
          borderBottom: '1px solid rgba(255,255,255,0.06)',
          background: 'rgba(255,255,255,0.03)',
          whiteSpace: 'nowrap',
          overflow: 'hidden',
          textOverflow: 'ellipsis'
        }}>
          {item.title}
        </div>
      )}
      <div style={{ width: '100%', height: item.title ? `calc(100% - 32px)` : '100%' }}>
        {chartData.length > 0 ? (
          <ReactECharts
            option={option}
            style={{ width: '100%', height: '100%' }}
            notMerge
            lazyUpdate
            onEvents={{ click: onChartClick }}
          />
        ) : (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100%',
            color: '#666',
            fontSize: 13
          }}>
            请配置数据集
          </div>
        )}
      </div>
      {isSelected && (
        <div
          onMouseDown={handleResizeMouseDown}
          style={{
            position: 'absolute',
            right: 0,
            bottom: 0,
            width: 14,
            height: 14,
            cursor: 'nwse-resize',
            background: 'rgba(22,119,255,0.6)',
            borderRadius: '0 0 8px 0',
            zIndex: 10
          }}
        />
      )}
    </div>
  )
}

const ItemConfigModal: React.FC = () => {
  const {
    configModalVisible, editingItem, setConfigModalVisible, updateItem, items
  } = useDashboardDesignerStore()
  const [form] = Form.useForm()
  const [datasets, setDatasets] = useState<DataSet[]>([])

  useEffect(() => {
    if (configModalVisible) {
      getDatasetAll().then(setDatasets).catch(() => {})
    }
  }, [configModalVisible])

  useEffect(() => {
    if (configModalVisible && editingItem) {
      form.setFieldsValue({
        title: editingItem.title,
        chartType: editingItem.chartType,
        datasetId: editingItem.datasetId,
        xField: editingItem.xField,
        yFields: editingItem.yFields,
        linkageField: editingItem.linkageField,
        linkageTargetId: editingItem.linkageTargetId
      })
    }
  }, [configModalVisible, editingItem, form])

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      if (editingItem?.id) {
        updateItem(editingItem.id, {
          title: values.title,
          chartType: values.chartType,
          datasetId: values.datasetId,
          xField: values.xField,
          yFields: values.yFields,
          linkageField: values.linkageField,
          linkageTargetId: values.linkageTargetId
        })
      }
      setConfigModalVisible(false)
      message.success('图表配置已更新')
    } catch {}
  }

  const otherItems = items.filter(i => i.id !== editingItem?.id)

  return (
    <Modal
      title="图表配置"
      open={configModalVisible}
      onOk={handleOk}
      onCancel={() => setConfigModalVisible(false)}
      width={600}
      okText="确定"
      cancelText="取消"
    >
      <Form form={form} layout="vertical">
        <Form.Item label="图表标题" name="title">
          <Input placeholder="请输入图表标题" />
        </Form.Item>
        <Form.Item label="图表类型" name="chartType" rules={[{ required: true }]}>
          <Select>
            {CHART_PALETTE.map(c => (
              <Select.Option key={c.type} value={c.type}>{c.icon} {c.label}</Select.Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item label="数据集" name="datasetId" rules={[{ required: true, message: '请选择数据集' }]}>
          <Select placeholder="请选择数据集" showSearch optionFilterProp="label">
            {datasets.map(ds => (
              <Select.Option key={ds.id} value={ds.id} label={ds.name}>{ds.name}</Select.Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item label="分类轴字段" name="xField" rules={[{ required: true, message: '请输入字段名' }]}>
          <Input placeholder="如：product_name" />
        </Form.Item>
        <Form.Item label="数值轴字段" name="yFields" rules={[{ required: true, message: '请输入字段名' }]}>
          <Select mode="tags" placeholder="输入字段名后回车" />
        </Form.Item>
        <Form.Item label="联动字段" name="linkageField" tooltip="点击此图表时传递给目标图表的筛选字段">
          <Input placeholder="如：category（可选）" />
        </Form.Item>
        <Form.Item label="联动目标组件" name="linkageTargetId" tooltip="点击此图表时，目标图表将按联动字段值筛选">
          <Select placeholder="请选择联动目标" allowClear>
            {otherItems.map(i => (
              <Select.Option key={i.id} value={i.id}>{i.title || `图表-${i.id}`}</Select.Option>
            ))}
          </Select>
        </Form.Item>
      </Form>
    </Modal>
  )
}

const DashboardDesignerPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const canvasRef = useRef<HTMLDivElement>(null)
  const [datasets, setLocalDatasets] = useState<DataSet[]>([])
  const [chartDataMap, setChartDataMap] = useState<Record<number, Record<string, any>[]>>({})
  const [linkageState, setLinkageState] = useState<Record<number, { field: string; value: any }>>({})

  const {
    dashboard, items, selectedItemId, scale, isDirty,
    setDashboard, setItems, addItem, updateItem, removeItem,
    setSelectedItemId, setDraggingType, setIsDirty,
    setConfigModalVisible, setScale, moveItem, resizeItem, reset
  } = useDashboardDesignerStore()

  useEffect(() => {
    getDatasetAll().then(setLocalDatasets).catch(() => {})
  }, [])

  useEffect(() => {
    if (id && id !== 'new') {
      setLoading(true)
      getDashboardDetail(Number(id))
        .then(res => {
          setDashboard(res.dashboard)
          setItems(res.items.map(i => ({
            ...i,
            yFields: typeof (i as any).yFields === 'string'
              ? JSON.parse((i as any).yFields)
              : (i.yFields || []),
            chartConfig: typeof (i as any).chartConfig === 'string'
              ? JSON.parse((i as any).chartConfig)
              : (i.chartConfig || {})
          })))
          setIsDirty(false)
        })
        .catch(() => message.error('加载大屏失败'))
        .finally(() => setLoading(false))
    } else if (id === 'new') {
      setDashboard({
        name: '新建大屏',
        canvasWidth: 1920,
        canvasHeight: 1080,
        backgroundColor: '#0d1b2a',
        refreshInterval: 0,
        status: 1
      })
      setItems([])
      setIsDirty(false)
    }
    return () => { reset() }
  }, [id])

  const loadChartData = useCallback(async (datasetId: number) => {
    if (chartDataMap[datasetId]) return
    try {
      const data = await getChartData(datasetId)
      setChartDataMap(prev => ({ ...prev, [datasetId]: data }))
    } catch {}
  }, [chartDataMap])

  useEffect(() => {
    items.forEach(item => {
      if (item.datasetId) loadChartData(item.datasetId)
    })
  }, [items, loadChartData])

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'copy'
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    const { draggingType } = useDashboardDesignerStore.getState()
    if (!draggingType || !canvasRef.current) return

    const rect = canvasRef.current.getBoundingClientRect()
    const x = (e.clientX - rect.left) / scale
    const y = (e.clientY - rect.top) / scale
    const palette = CHART_PALETTE.find(p => p.type === draggingType)
    const tempId = Date.now()

    addItem({
      id: tempId,
      chartType: draggingType,
      title: palette?.label || '',
      positionX: Math.max(0, x - (palette?.defaultWidth || 400) / 2),
      positionY: Math.max(0, y - (palette?.defaultHeight || 300) / 2),
      width: palette?.defaultWidth || 400,
      height: palette?.defaultHeight || 300,
      yFields: [],
      sortOrder: items.length
    })
  }

  const handleCanvasClick = () => setSelectedItemId(null)

  const handleSave = async () => {
    if (!dashboard) return
    try {
      let savedDashboard = { ...dashboard }
      if (dashboard.id) {
        await updateDashboard(savedDashboard)
      } else {
        savedDashboard = await createDashboard(savedDashboard)
        setDashboard(savedDashboard)
        navigate(`/dashboard/designer/${savedDashboard.id}`, { replace: true })
      }
      const saveItems = items.map((item, idx) => ({
        ...item,
        dashboardId: savedDashboard.id,
        sortOrder: idx,
        yFields: item.yFields || [],
        chartConfig: item.chartConfig || {}
      }))
      await saveDashboardItems(savedDashboard.id!, saveItems)
      setIsDirty(false)
      message.success('保存成功')
    } catch {
      message.error('保存失败')
    }
  }

  const handlePreview = () => {
    if (dashboard?.id) {
      window.open(`/dashboard/viewer/${dashboard.id}`, '_blank')
    } else {
      message.warning('请先保存大屏')
    }
  }

  const handleDeleteSelected = () => {
    if (selectedItemId) {
      removeItem(selectedItemId)
      setSelectedItemId(null)
    }
  }

  const handleChartClick = (itemId: number, item: ChartDashboardItem) => (params: any) => {
    if (!item.linkageField || !item.linkageTargetId) return
    const clickedName = params.name
    if (clickedName === undefined) return
    setLinkageState(prev => ({
      ...prev,
      [item.linkageTargetId!]: { field: item.linkageField, value: clickedName }
    }))
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#0a0e1a' }}>
        <Spin size="large" tip="加载大屏..." />
      </div>
    )
  }

  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column', background: '#0a0e1a', color: '#e0e0e0' }}>
      <div style={{
        height: 48,
        background: 'rgba(13,27,42,0.95)',
        borderBottom: '1px solid rgba(255,255,255,0.08)',
        display: 'flex',
        alignItems: 'center',
        padding: '0 16px',
        gap: 12,
        flexShrink: 0
      }}>
        <Button icon={<ArrowLeftOutlined />} type="text" size="small" onClick={() => navigate(-1)} style={{ color: '#aaa' }} />
        <span style={{ fontSize: 15, fontWeight: 600, color: '#e0e0e0' }}>{dashboard?.name || '新建大屏'}</span>
        {isDirty && <span style={{ color: '#fa8c16', fontSize: 12 }}>未保存</span>}
        <div style={{ flex: 1 }} />
        <Space size={4}>
          <Tooltip title="缩小">
            <Button icon={<ZoomOutOutlined />} type="text" size="small" style={{ color: '#aaa' }}
              onClick={() => setScale(Math.max(0.3, scale - 0.1))} />
          </Tooltip>
          <span style={{ color: '#888', fontSize: 12, minWidth: 40, textAlign: 'center' }}>{Math.round(scale * 100)}%</span>
          <Tooltip title="放大">
            <Button icon={<ZoomInOutlined />} type="text" size="small" style={{ color: '#aaa' }}
              onClick={() => setScale(Math.min(1.5, scale + 0.1))} />
          </Tooltip>
          <Tooltip title="删除选中">
            <Button icon={<DeleteOutlined />} type="text" size="small" style={{ color: '#ff4d4f' }}
              onClick={handleDeleteSelected} disabled={!selectedItemId} />
          </Tooltip>
          <Button icon={<SaveOutlined />} type="primary" size="small" onClick={handleSave}>保存</Button>
          <Button icon={<EyeOutlined />} size="small" onClick={handlePreview}>预览</Button>
        </Space>
      </div>

      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        <div style={{
          width: 200,
          background: 'rgba(13,27,42,0.6)',
          borderRight: '1px solid rgba(255,255,255,0.06)',
          overflow: 'auto',
          flexShrink: 0
        }}>
          <ChartPalette />
        </div>

        <div
          style={{
            flex: 1,
            overflow: 'auto',
            background: '#0a0e1a',
            position: 'relative'
          }}
          onClick={handleCanvasClick}
        >
          <div
            style={{
              position: 'relative',
              margin: 20,
              width: (dashboard?.canvasWidth || 1920) * scale,
              height: (dashboard?.canvasHeight || 1080) * scale,
              background: dashboard?.backgroundColor || '#0d1b2a',
              borderRadius: 4,
              boxShadow: '0 0 40px rgba(0,0,0,0.5)',
              overflow: 'hidden'
            }}
            ref={canvasRef}
            onDragOver={handleDragOver}
            onDrop={handleDrop}
            onClick={e => e.stopPropagation()}
          >
            <div style={{
              transform: `scale(${scale})`,
              transformOrigin: 'top left',
              width: dashboard?.canvasWidth || 1920,
              height: dashboard?.canvasHeight || 1080,
              position: 'relative'
            }}>
              {items.map(item => (
                <ChartItemBox
                  key={item.id}
                  item={item}
                  isSelected={selectedItemId === item.id}
                  scale={scale}
                  onSelect={() => setSelectedItemId(item.id!)}
                  onMove={(x, y) => moveItem(item.id!, x, y)}
                  onResize={(w, h) => resizeItem(item.id!, w, h)}
                  onDoubleClick={() => setConfigModalVisible(true, item)}
                  chartData={item.datasetId ? (chartDataMap[item.datasetId] || []) : []}
                  linkageFilter={linkageState[item.id!]}
                  onChartClick={handleChartClick(item.id!, item)}
                />
              ))}
            </div>
          </div>
        </div>

        <div style={{
          width: 260,
          background: 'rgba(13,27,42,0.6)',
          borderLeft: '1px solid rgba(255,255,255,0.06)',
          padding: 12,
          overflow: 'auto',
          flexShrink: 0
        }}>
          {selectedItemId ? (() => {
            const sel = items.find(i => i.id === selectedItemId)
            if (!sel) return null
            return (
              <div>
                <div style={{ color: '#e0e0e0', fontSize: 13, fontWeight: 600, marginBottom: 12 }}>
                  图表属性
                </div>
                <div style={{ marginBottom: 8 }}>
                  <label style={{ color: '#888', fontSize: 12 }}>标题</label>
                  <Input
                    size="small"
                    value={sel.title || ''}
                    onChange={e => updateItem(sel.id!, { title: e.target.value })}
                    style={{ background: 'rgba(255,255,255,0.06)', borderColor: 'rgba(255,255,255,0.1)', color: '#e0e0e0' }}
                  />
                </div>
                <div style={{ marginBottom: 8 }}>
                  <label style={{ color: '#888', fontSize: 12 }}>X (px)</label>
                  <InputNumber
                    size="small" min={0}
                    value={Math.round(sel.positionX)}
                    onChange={v => v !== null && moveItem(sel.id!, v, sel.positionY)}
                    style={{ width: '100%' }}
                  />
                </div>
                <div style={{ marginBottom: 8 }}>
                  <label style={{ color: '#888', fontSize: 12 }}>Y (px)</label>
                  <InputNumber
                    size="small" min={0}
                    value={Math.round(sel.positionY)}
                    onChange={v => v !== null && moveItem(sel.id!, sel.positionX, v)}
                    style={{ width: '100%' }}
                  />
                </div>
                <div style={{ marginBottom: 8 }}>
                  <label style={{ color: '#888', fontSize: 12 }}>宽度</label>
                  <InputNumber
                    size="small" min={200}
                    value={sel.width}
                    onChange={v => v !== null && resizeItem(sel.id!, v, sel.height)}
                    style={{ width: '100%' }}
                  />
                </div>
                <div style={{ marginBottom: 8 }}>
                  <label style={{ color: '#888', fontSize: 12 }}>高度</label>
                  <InputNumber
                    size="small" min={150}
                    value={sel.height}
                    onChange={v => v !== null && resizeItem(sel.id!, sel.width, v)}
                    style={{ width: '100%' }}
                  />
                </div>
                <div style={{ marginBottom: 8 }}>
                  <label style={{ color: '#888', fontSize: 12 }}>联动字段</label>
                  <Input
                    size="small"
                    value={sel.linkageField || ''}
                    onChange={e => updateItem(sel.id!, { linkageField: e.target.value })}
                    placeholder="如 category"
                    style={{ background: 'rgba(255,255,255,0.06)', borderColor: 'rgba(255,255,255,0.1)', color: '#e0e0e0' }}
                  />
                </div>
                <div style={{ marginBottom: 8 }}>
                  <label style={{ color: '#888', fontSize: 12 }}>联动目标</label>
                  <Select
                    size="small"
                    value={sel.linkageTargetId || undefined}
                    onChange={v => updateItem(sel.id!, { linkageTargetId: v })}
                    placeholder="选择目标图表"
                    allowClear
                    style={{ width: '100%' }}
                  >
                    {items.filter(i => i.id !== sel.id).map(i => (
                      <Select.Option key={i.id} value={i.id}>{i.title || `图表-${i.id}`}</Select.Option>
                    ))}
                  </Select>
                </div>
                <Button icon={<SettingOutlined />} size="small" block onClick={() => setConfigModalVisible(true, sel)}>
                  详细配置
                </Button>
              </div>
            )
          })() : (
            <div>
              <div style={{ color: '#e0e0e0', fontSize: 13, fontWeight: 600, marginBottom: 12 }}>
                大屏设置
              </div>
              <div style={{ marginBottom: 8 }}>
                <label style={{ color: '#888', fontSize: 12 }}>画布宽度</label>
                <InputNumber
                  size="small" min={800} max={3840}
                  value={dashboard?.canvasWidth || 1920}
                  onChange={v => v !== null && dashboard && setDashboard({ ...dashboard, canvasWidth: v })}
                  style={{ width: '100%' }}
                />
              </div>
              <div style={{ marginBottom: 8 }}>
                <label style={{ color: '#888', fontSize: 12 }}>画布高度</label>
                <InputNumber
                  size="small" min={600} max={2160}
                  value={dashboard?.canvasHeight || 1080}
                  onChange={v => v !== null && dashboard && setDashboard({ ...dashboard, canvasHeight: v })}
                  style={{ width: '100%' }}
                />
              </div>
              <div style={{ marginBottom: 8 }}>
                <label style={{ color: '#888', fontSize: 12 }}>自动刷新间隔(秒)</label>
                <InputNumber
                  size="small" min={0} max={3600}
                  value={dashboard?.refreshInterval || 0}
                  onChange={v => v !== null && dashboard && setDashboard({ ...dashboard, refreshInterval: v })}
                  style={{ width: '100%' }}
                />
                <div style={{ color: '#666', fontSize: 11, marginTop: 4 }}>0 表示不自动刷新</div>
              </div>
              <div style={{ marginBottom: 8 }}>
                <label style={{ color: '#888', fontSize: 12 }}>背景色</label>
                <Input
                  size="small"
                  value={dashboard?.backgroundColor || '#0d1b2a'}
                  onChange={e => dashboard && setDashboard({ ...dashboard, backgroundColor: e.target.value })}
                  style={{ background: 'rgba(255,255,255,0.06)', borderColor: 'rgba(255,255,255,0.1)', color: '#e0e0e0' }}
                />
              </div>
              <div style={{ color: '#666', fontSize: 12, marginTop: 16 }}>
                提示：从左侧拖拽图表组件到画布
              </div>
            </div>
          )}
        </div>
      </div>

      <ItemConfigModal />
    </div>
  )
}

export default DashboardDesignerPage
