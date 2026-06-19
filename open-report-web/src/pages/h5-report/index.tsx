import React, { useEffect, useState, useRef, useCallback } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import {
  Spin,
  Typography,
  Divider,
  Result,
  Button,
  Drawer,
  message
} from 'antd'
import {
  ArrowLeftOutlined,
  SyncOutlined,
  SettingOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import ParamPanel from '../preview/components/ParamPanel'
import MobileReportView from '../preview/components/MobileReportView'
import { usePreviewStore } from '../preview/store/preview'
import { getReportById, exportReportExcel, exportReportPdf } from '@/api/report'
import { ReportTemplate } from '@/types'
import { downloadBlob } from '../preview/utils/report'

const { Title } = Typography

const H5ReportPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [reportInfo, setReportInfo] = useState<ReportTemplate | null>(null)
  const [initLoading, setInitLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [paramDrawerVisible, setParamDrawerVisible] = useState(false)

  const setReportId = usePreviewStore((state) => state.setReportId)
  const setReportName = usePreviewStore((state) => state.setReportName)
  const loadParams = usePreviewStore((state) => state.loadParams)
  const executeReportStore = usePreviewStore((state) => state.executeReport)
  const reportData = usePreviewStore((state) => state.reportData)
  const toggleMobile = usePreviewStore((state) => state.toggleMobile)
  const reset = usePreviewStore((state) => state.reset)
  const params = usePreviewStore((state) => state.params)

  const refreshLockRef = useRef(false)

  useEffect(() => {
    toggleMobile(true)
  }, [toggleMobile])

  useEffect(() => {
    const loadReport = async () => {
      if (!id) {
        setError('报表ID不能为空')
        setInitLoading(false)
        return
      }

      const reportId = Number(id)
      if (isNaN(reportId)) {
        setError('无效的报表ID')
        setInitLoading(false)
        return
      }

      try {
        setInitLoading(true)
        setReportId(reportId)

        const report = await getReportById(reportId)
        setReportInfo(report)
        setReportName(report.name || '')

        await loadParams()

        const urlParams: Record<string, any> = {}
        searchParams.forEach((value, key) => {
          urlParams[key] = value
        })
        if (Object.keys(urlParams).length > 0) {
          const mergedParams = { ...params, ...urlParams }
          Object.entries(mergedParams).forEach(([k, v]) => {
            usePreviewStore.setState({ params: mergedParams })
          })
          await executeReportStore()
        } else {
          await executeReportStore()
        }
      } catch (err: any) {
        console.error('加载报表失败:', err)
        setError(err.message || '加载报表失败')
      } finally {
        setInitLoading(false)
      }
    }

    loadReport()

    return () => {
      reset()
    }
  }, [id, searchParams, setReportId, setReportName, loadParams, executeReportStore, reset, params])

  const handleRefresh = useCallback(async () => {
    if (refreshLockRef.current) return
    try {
      refreshLockRef.current = true
      message.loading({ content: '正在刷新...', key: 'refresh', duration: 0 })
      await executeReportStore()
      message.success({ content: '刷新成功', key: 'refresh' })
    } catch {
      message.error({ content: '刷新失败', key: 'refresh' })
    } finally {
      refreshLockRef.current = false
    }
  }, [executeReportStore])

  const handleExport = useCallback(async (type: 'excel' | 'pdf' | 'html') => {
    if (!id) return
    try {
      message.loading({ content: `正在导出${type.toUpperCase()}...`, key: 'export', duration: 0 })
      let blob: Blob
      if (type === 'excel') {
        blob = await exportReportExcel(Number(id), params)
      } else if (type === 'pdf') {
        blob = await exportReportPdf(Number(id), params)
      } else {
        blob = new Blob(['<html><body>HTML export placeholder</body></html>'], { type: 'text/html' })
      }
      downloadBlob(blob, `${reportInfo?.name || 'report'}.${type === 'excel' ? 'xlsx' : type === 'pdf' ? 'pdf' : 'html'}`)
      message.success({ content: '导出成功', key: 'export' })
    } catch {
      message.error({ content: '导出失败', key: 'export' })
    }
  }, [id, params, reportInfo])

  const handleShare = useCallback(() => {
    const shareData = {
      title: reportInfo?.name || '报表分享',
      text: reportInfo?.description || '分享一份报表给您查看',
      url: window.location.href
    }
    if (navigator.share) {
      navigator.share(shareData)
        .then(() => message.success('分享成功'))
        .catch(() => {})
    } else {
      navigator.clipboard?.writeText(window.location.href)
      message.success('分享链接已复制到剪贴板')
    }
  }, [reportInfo])

  if (initLoading) {
    return (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          background: '#f5f5f5'
        }}
      >
        <Spin size="large" tip="加载报表中..." />
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ padding: 24, background: '#f5f5f5', minHeight: '100vh' }}>
        <Result
          status="error"
          title="加载失败"
          subTitle={error}
          extra={
            <Button type="primary" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
              返回
            </Button>
          }
        />
      </div>
    )
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        background: '#f5f5f5',
        paddingBottom: 80
      }}
    >
      <div
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 100,
          background: '#fff',
          padding: '12px 16px',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          boxShadow: '0 1px 4px rgba(0,0,0,0.06)'
        }}
      >
        <Button
          icon={<ArrowLeftOutlined />}
          type="text"
          onClick={() => navigate(-1)}
        />
        <div style={{ flex: 1, overflow: 'hidden' }}>
          <Title
            level={5}
            style={{
              margin: 0,
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis'
            }}
          >
            {reportInfo?.name || '报表预览'}
          </Title>
          {reportInfo?.description && (
            <div
              style={{
                fontSize: 12,
                color: '#999',
                marginTop: 2,
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis'
              }}
            >
              {reportInfo.description}
            </div>
          )}
        </div>
        <Button
          icon={<SettingOutlined />}
          type="text"
          onClick={() => setParamDrawerVisible(true)}
        />
        <Button
          icon={<ReloadOutlined />}
          type="text"
          onClick={handleRefresh}
        />
      </div>

      <div style={{ padding: 12 }}>
        {reportData?.title && (
          <>
            <div
              style={{
                background: '#fff',
                borderRadius: 8,
                padding: '12px 16px',
                marginBottom: 12
              }}
            >
              <Title level={4} style={{ margin: 0, textAlign: 'center' }}>
                {reportData.title}
              </Title>
              {reportData.summary && (
                <div style={{ textAlign: 'center', color: '#666', marginTop: 6, fontSize: 13 }}>
                  {reportData.summary}
                </div>
              )}
            </div>
          </>
        )}

        <MobileReportView onExport={handleExport} onShare={handleShare} />
      </div>

      <Drawer
        title="筛选参数"
        placement="bottom"
        height="70%"
        open={paramDrawerVisible}
        onClose={() => setParamDrawerVisible(false)}
        extra={
          <Button
            type="primary"
            size="small"
            icon={<SyncOutlined />}
            onClick={async () => {
              setParamDrawerVisible(false)
              await handleRefresh()
            }}
          >
            执行查询
          </Button>
        }
      >
        <ParamPanel />
      </Drawer>
    </div>
  )
}

export default H5ReportPage
