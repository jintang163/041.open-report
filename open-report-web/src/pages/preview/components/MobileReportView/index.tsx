import React, { useMemo, useState, useRef, useCallback } from 'react'
import {
  Card,
  Empty,
  Spin,
  List,
  Tag,
  Input,
  Divider,
  Modal,
  message
} from 'antd'
import {
  ShareAltOutlined,
  FileExcelOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  LinkOutlined,
  CloseOutlined,
  ZoomInOutlined,
  ZoomOutOutlined,
  InfoCircleOutlined
} from '@ant-design/icons'
import { usePreviewStore } from '../../store/preview'
import { TableData, TableColumn, parseHtmlTable, ChartConfig } from '../../utils/report'
import ReportChart from '../ReportChart'
import { usePinchZoom } from '@/hooks/usePinchZoom'
import { useSwipe } from '@/hooks/useSwipe'
import { useLongPress } from '@/hooks/useLongPress'

interface MobileReportViewProps {
  tableData?: TableData
  charts?: ChartConfig[]
  onExport?: (type: 'excel' | 'pdf' | 'html') => void
  onShare?: () => void
}

const MobileReportView: React.FC<MobileReportViewProps> = ({ tableData, charts, onExport, onShare }) => {
  const storeData = usePreviewStore((state) => state.reportData)
  const loading = usePreviewStore((state) => state.loading)
  const reportName = usePreviewStore((state) => state.reportName)
  const [searchText, setSearchText] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [actionSheetVisible, setActionSheetVisible] = useState(false)
  const [showHint, setShowHint] = useState(true)
  const containerRef = useRef<HTMLDivElement>(null)
  const pageHintTimerRef = useRef<any>(null)

  const data: TableData | undefined = useMemo(() => {
    if (tableData) return tableData
    if (storeData?.table) return storeData.table
    if (storeData?.html) {
      const parsed = parseHtmlTable(storeData.html)
      if (parsed) return parsed
    }
    return undefined
  }, [tableData, storeData])

  const totalPages = useMemo(() => {
    if (!data?.dataSource) return 1
    return Math.max(1, Math.ceil(data.dataSource.length / pageSize))
  }, [data, pageSize])

  const filteredData = useMemo(() => {
    if (!data?.dataSource || !searchText) return data?.dataSource || []
    const lowerSearch = searchText.toLowerCase()
    return data.dataSource.filter((record) =>
      Object.values(record).some((value) =>
        String(value).toLowerCase().includes(lowerSearch)
      )
    )
  }, [data, searchText])

  const paginatedData = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize
    return filteredData.slice(startIndex, startIndex + pageSize)
  }, [filteredData, currentPage, pageSize])

  const { scale, setScale, bind: pinchBind } = usePinchZoom({
    minScale: 0.5,
    maxScale: 3
  })

  const showPageHint = useCallback((page: number) => {
    setShowHint(true)
    if (pageHintTimerRef.current) {
      clearTimeout(pageHintTimerRef.current)
    }
    pageHintTimerRef.current = setTimeout(() => setShowHint(false), 1500)
  }, [])

  const goToNextPage = useCallback(() => {
    if (currentPage < totalPages) {
      const next = currentPage + 1
      setCurrentPage(next)
      showPageHint(next)
    } else {
      message.info('已经是最后一页')
    }
  }, [currentPage, totalPages, showPageHint])

  const goToPrevPage = useCallback(() => {
    if (currentPage > 1) {
      const prev = currentPage - 1
      setCurrentPage(prev)
      showPageHint(prev)
    } else {
      message.info('已经是第一页')
    }
  }, [currentPage, showPageHint])

  const { bind: swipeBind } = useSwipe({
    onSwipeLeft: goToNextPage,
    onSwipeRight: goToPrevPage,
    threshold: 60
  })

  const handleLongPress = useCallback(() => {
    setActionSheetVisible(true)
  }, [])

  const { bind: longPressBind } = useLongPress({
    onLongPress: handleLongPress,
    duration: 600,
    moveThreshold: 15
  })

  const handleExport = (type: 'excel' | 'pdf' | 'html') => {
    setActionSheetVisible(false)
    if (onExport) {
      onExport(type)
    } else {
      message.loading({ content: `正在导出${type.toUpperCase()}...`, key: 'export', duration: 0 })
      setTimeout(() => {
        message.success({ content: `${type.toUpperCase()}导出成功`, key: 'export' })
      }, 1000)
    }
  }

  const handleShare = () => {
    setActionSheetVisible(false)
    if (onShare) {
      onShare()
    } else {
      const shareData = {
        title: reportName || '报表分享',
        text: '分享一份报表给您查看',
        url: window.location.href
      }
      if (navigator.share) {
        navigator.share(shareData).catch(() => {})
      } else {
        navigator.clipboard?.writeText(window.location.href)
        message.success('分享链接已复制到剪贴板')
      }
    }
  }

  const resetScale = () => {
    setScale(1)
  }

  const mergedTouchHandlers = {
    onTouchStart: (e: React.TouchEvent) => {
      pinchBind.onTouchStart(e)
      swipeBind.onTouchStart(e)
      longPressBind.onTouchStart(e)
    },
    onTouchMove: (e: React.TouchEvent) => {
      pinchBind.onTouchMove(e)
      swipeBind.onTouchMove(e)
      longPressBind.onTouchMove(e)
    },
    onTouchEnd: (e: React.TouchEvent) => {
      pinchBind.onTouchEnd()
      swipeBind.onTouchEnd(e)
      longPressBind.onTouchEnd(e)
    }
  }

  const renderCard = (record: any, index: number) => {
    if (!data?.columns) return null
    const columns = data.columns

    return (
      <Card
        key={index}
        size="small"
        style={{ marginBottom: 12, borderRadius: 8 }}
        bodyStyle={{ padding: 12 }}
      >
        <List
          size="small"
          dataSource={columns}
          renderItem={(col: TableColumn, colIndex) => {
            const value = record[col.dataIndex]
            return (
              <List.Item
                key={col.dataIndex}
                style={{
                  padding: '6px 0',
                  borderBottom: colIndex < columns.length - 1 ? '1px solid #f0f0f0' : 'none'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', width: '100%', gap: 8 }}>
                  <span style={{ color: '#666', flexShrink: 0, fontWeight: 500 }}>
                    {col.title}
                  </span>
                  <span style={{ color: '#333', textAlign: 'right', wordBreak: 'break-all' }}>
                    {value !== undefined && value !== null && value !== '' ? (
                      String(value)
                    ) : (
                      <Tag color="default">--</Tag>
                    )}
                  </span>
                </div>
              </List.Item>
            )
          }}
        />
      </Card>
    )
  }

  const actionSheetItems = [
    {
      key: 'excel',
      icon: <FileExcelOutlined />,
      label: '导出 Excel',
      onClick: () => handleExport('excel')
    },
    {
      key: 'pdf',
      icon: <FilePdfOutlined />,
      label: '导出 PDF',
      onClick: () => handleExport('pdf')
    },
    {
      key: 'html',
      icon: <FileTextOutlined />,
      label: '导出 HTML',
      onClick: () => handleExport('html')
    },
    {
      key: 'share',
      icon: <ShareAltOutlined />,
      label: '分享报表',
      onClick: handleShare
    },
    {
      key: 'copy',
      icon: <LinkOutlined />,
      label: '复制链接',
      onClick: () => {
        navigator.clipboard?.writeText(window.location.href)
        message.success('链接已复制')
        setActionSheetVisible(false)
      }
    }
  ]

  if (!data && (!charts || charts.length === 0) && (!storeData?.charts || storeData.charts.length === 0)) {
    return (
      <Card style={{ borderRadius: 8 }}>
        <div style={{ padding: 40, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Empty description="暂无数据，请先执行查询" />
        </div>
      </Card>
    )
  }

  return (
    <>
      <div
        ref={containerRef}
        style={{
          position: 'relative',
          overflow: 'hidden',
          touchAction: 'manipulation',
          userSelect: 'none'
        }}
        {...mergedTouchHandlers}
        {...longPressBind}
      >
        <div
          style={{
            transform: `scale(${scale})`,
            transformOrigin: 'top center',
            transition: scale === 1 ? 'transform 0.2s ease-out' : 'none',
            minHeight: '100%'
          }}
        >
          <Spin spinning={loading}>
            {(charts || storeData?.charts) && (
              <>
                <ReportChart charts={charts || storeData?.charts} height={280} />
                <Divider style={{ margin: '8px 0 16px' }} />
              </>
            )}

            {data && data.columns && data.columns.length > 0 && (
              <Card style={{ borderRadius: 8 }} bodyStyle={{ padding: 12 }}>
                <Input.Search
                  placeholder="搜索内容..."
                  allowClear
                  onChange={(e) => {
                    setSearchText(e.target.value)
                    setCurrentPage(1)
                  }}
                  style={{ marginBottom: 12 }}
                />

                {paginatedData.length > 0 ? (
                  <>
                    {paginatedData.map((record, index) =>
                      renderCard(record, (currentPage - 1) * pageSize + index)
                    )}
                    <div style={{ textAlign: 'center', color: '#999', fontSize: 12, padding: '8px 0' }}>
                      第 {currentPage} / {totalPages} 页 · 左右滑动切换
                    </div>
                  </>
                ) : (
                  <Empty description="没有匹配的数据" style={{ padding: 20 }} />
                )}
              </Card>
            )}
          </Spin>
        </div>

        {showHint && filteredData.length > pageSize && (
          <div
            style={{
              position: 'absolute',
              top: '50%',
              left: '50%',
              transform: 'translate(-50%, -50%)',
              background: 'rgba(0, 0, 0, 0.7)',
              color: '#fff',
              padding: '12px 24px',
              borderRadius: 8,
              fontSize: 14,
              pointerEvents: 'none',
              zIndex: 100
            }}
          >
            第 {currentPage} / {totalPages} 页
          </div>
        )}

        {scale !== 1 && (
          <div
            style={{
              position: 'fixed',
              bottom: 24,
              right: 16,
              background: '#fff',
              borderRadius: 24,
              padding: '4px 12px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              zIndex: 100
            }}
          >
            <ZoomOutOutlined onClick={resetScale} style={{ color: '#666', fontSize: 16 }} />
            <span style={{ fontSize: 13, minWidth: 40, textAlign: 'center' }}>
              {Math.round(scale * 100)}%
            </span>
            <ZoomInOutlined onClick={() => setScale(scale + 0.25)} style={{ color: '#666', fontSize: 16 }} />
          </div>
        )}

        <div
          style={{
            position: 'fixed',
            bottom: 80,
            right: 16,
            background: 'rgba(0,0,0,0.5)',
            color: '#fff',
            padding: '4px 10px',
            borderRadius: 12,
            fontSize: 11,
            pointerEvents: 'none'
          }}
        >
          <InfoCircleOutlined /> 双指缩放 · 左右滑动 · 长按菜单
        </div>
      </div>

      <Modal
        title={null}
        footer={null}
        open={actionSheetVisible}
        closable={false}
        transitionName="slide-up"
        maskClosable
        onCancel={() => setActionSheetVisible(false)}
        centered={false}
        style={{ top: 'auto', bottom: 0, margin: 0 }}
        bodyStyle={{ padding: 0, borderRadius: '16px 16px 0 0' }}
        width="100%"
      >
        <div style={{ padding: 8 }}>
          {actionSheetItems.map((item) => (
            <div
              key={item.key}
              onClick={item.onClick}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '14px 16px',
                fontSize: 15,
                borderBottom: '1px solid #f5f5f5',
                cursor: 'pointer'
              }}
            >
              <span style={{ fontSize: 20, color: '#1677ff', width: 28, textAlign: 'center' }}>
                {item.icon}
              </span>
              <span>{item.label}</span>
            </div>
          ))}
          <div
            onClick={() => setActionSheetVisible(false)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
              padding: '16px',
              fontSize: 15,
              color: '#999',
              cursor: 'pointer'
            }}
          >
            <CloseOutlined />
            取消
          </div>
        </div>
      </Modal>
    </>
  )
}

export default MobileReportView
