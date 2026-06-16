import React from 'react'
import { Button, Space, Tooltip, Dropdown, message } from 'antd'
import {
  ExportOutlined,
  PrinterOutlined,
  ReloadOutlined,
  FullscreenOutlined,
  FullscreenExitOutlined,
  FileExcelOutlined,
  FilePdfOutlined,
  PhoneOutlined,
  DesktopOutlined
} from '@ant-design/icons'
import { usePreviewStore } from '../../store/preview'
import { handlePrint } from '../../utils/report'

interface ReportToolbarProps {
  showExport?: boolean
  showPrint?: boolean
  showRefresh?: boolean
  showFullscreen?: boolean
  showViewMode?: boolean
  onRefresh?: () => void
  contentId?: string
}

const ReportToolbar: React.FC<ReportToolbarProps> = ({
  showExport = true,
  showPrint = true,
  showRefresh = true,
  showFullscreen = true,
  showViewMode = false,
  onRefresh,
  contentId = 'report-content'
}) => {
  const exportExcel = usePreviewStore((state) => state.exportExcel)
  const exportPdf = usePreviewStore((state) => state.exportPdf)
  const executeReport = usePreviewStore((state) => state.executeReport)
  const exporting = usePreviewStore((state) => state.exporting)
  const loading = usePreviewStore((state) => state.loading)
  const isFullscreen = usePreviewStore((state) => state.isFullscreen)
  const toggleFullscreen = usePreviewStore((state) => state.toggleFullscreen)
  const isMobile = usePreviewStore((state) => state.isMobile)
  const toggleMobile = usePreviewStore((state) => state.toggleMobile)

  const handleRefresh = async () => {
    if (onRefresh) {
      onRefresh()
    } else {
      await executeReport()
      message.success('刷新成功')
    }
  }

  const handlePrintClick = () => {
    handlePrint(contentId)
  }

  const exportMenu = {
    items: [
      {
        key: 'excel',
        icon: <FileExcelOutlined />,
        label: '导出 Excel',
        onClick: () => exportExcel()
      },
      {
        key: 'pdf',
        icon: <FilePdfOutlined />,
        label: '导出 PDF',
        onClick: () => exportPdf()
      }
    ]
  }

  const viewModeMenu = {
    items: [
      {
        key: 'desktop',
        icon: <DesktopOutlined />,
        label: '桌面端视图',
        onClick: () => toggleMobile(false)
      },
      {
        key: 'mobile',
        icon: <PhoneOutlined />,
        label: '移动端视图',
        onClick: () => toggleMobile(true)
      }
    ]
  }

  return (
    <div
      style={{
        background: '#fff',
        borderRadius: 8,
        padding: '12px 16px',
        marginBottom: 16,
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}
    >
      <div />
      <Space>
        {showExport && (
          <Dropdown menu={exportMenu} placement="bottomRight">
            <Button icon={<ExportOutlined />} loading={exporting}>
              导出
            </Button>
          </Dropdown>
        )}
        {showPrint && (
          <Tooltip title="打印报表">
            <Button icon={<PrinterOutlined />} onClick={handlePrintClick}>
              打印
            </Button>
          </Tooltip>
        )}
        {showRefresh && (
          <Tooltip title="刷新数据">
            <Button icon={<ReloadOutlined />} onClick={handleRefresh} loading={loading}>
              刷新
            </Button>
          </Tooltip>
        )}
        {showViewMode && (
          <Dropdown menu={viewModeMenu} placement="bottomRight">
            <Button icon={isMobile ? <PhoneOutlined /> : <DesktopOutlined />}>
              视图
            </Button>
          </Dropdown>
        )}
        {showFullscreen && (
          <Tooltip title={isFullscreen ? '退出全屏' : '全屏显示'}>
            <Button icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />} onClick={toggleFullscreen} />
          </Tooltip>
        )}
      </Space>
    </div>
  )
}

export default ReportToolbar
