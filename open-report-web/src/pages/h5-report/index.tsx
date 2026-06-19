import React, { useEffect, useState, useRef, useCallback } from 'react'
import { useParams, useNavigate, useSearchParams, useLocation } from 'react-router-dom'
import {
  Spin,
  Typography,
  Result,
  Button,
  Drawer,
  message,
  Modal,
  Input
} from 'antd'
import {
  ArrowLeftOutlined,
  SyncOutlined,
  SettingOutlined,
  ReloadOutlined,
  LockOutlined
} from '@ant-design/icons'
import ParamPanel from '../preview/components/ParamPanel'
import MobileReportView from '../preview/components/MobileReportView'
import { usePreviewStore } from '../preview/store/preview'
import {
  getReportById,
  exportReportExcel,
  exportReportPdf,
  getPublicReportInfo,
  executePublicReport,
  getPublicReportParameters,
  exportPublicReportExcel,
  exportPublicReportPdf
} from '@/api/report'
import { ReportTemplate } from '@/types'
import { downloadBlob } from '../preview/utils/report'

const { Title } = Typography

interface PublicReportInfo {
  id: number
  name: string
  description?: string
  params?: any[]
}

const H5ReportPage: React.FC = () => {
  const { id, token } = useParams<{ id: string; token: string }>()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const [reportInfo, setReportInfo] = useState<ReportTemplate | null>(null)
  const [publicReportInfo, setPublicReportInfo] = useState<PublicReportInfo | null>(null)
  const [initLoading, setInitLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [paramDrawerVisible, setParamDrawerVisible] = useState(false)
  const [passwordModalVisible, setPasswordModalVisible] = useState(false)
  const [sharePassword, setSharePassword] = useState('')
  const [pendingParams, setPendingParams] = useState<Record<string, any> | null>(null)

  const isShareMode = location.pathname.startsWith('/h5/share/')

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

  const loadPublicReportWithPassword = useCallback(async (password?: string) => {
    if (!token) return

    try {
      setInitLoading(true)
      const info = await getPublicReportInfo(token, password)
      setPublicReportInfo(info)
      setReportId(info.id)
      setReportName(info.name || '')

      const paramConfigs = await getPublicReportParameters(token, password)
      if (paramConfigs && paramConfigs.length > 0) {
        usePreviewStore.setState({ paramConfigs })
      }

      const urlParams: Record<string, any> = {}
      searchParams.forEach((value, key) => {
        urlParams[key] = value
      })

      const allParams = { ...params, ...urlParams }
      if (Object.keys(allParams).length > 0) {
        usePreviewStore.setState({ params: allParams })
      }

      const data = await executePublicReport(token, allParams, password)
      usePreviewStore.setState({ reportData: data })
      setError(null)
    } catch (err: any) {
      if (err?.message?.includes('密码') || err?.code === 'PASSWORD_REQUIRED') {
        setPasswordModalVisible(true)
        setPendingParams({ ...params })
      } else {
        console.error('加载分享报表失败:', err)
        setError(err.message || '分享链接无效或已过期')
      }
    } finally {
      setInitLoading(false)
    }
  }, [token, searchParams, params, setReportId, setReportName])

  const loadRegularReport = useCallback(async () => {
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
        usePreviewStore.setState({ params: mergedParams })
        await executeReportStore()
      } else {
        await executeReportStore()
      }
      setError(null)
    } catch (err: any) {
      console.error('加载报表失败:', err)
      setError(err.message || '加载报表失败')
    } finally {
      setInitLoading(false)
    }
  }, [id, searchParams, setReportId, setReportName, loadParams, executeReportStore, params])

  useEffect(() => {
    if (isShareMode) {
      loadPublicReportWithPassword()
    } else {
      loadRegularReport()
    }

    return () => {
      reset()
    }
  }, [isShareMode, loadPublicReportWithPassword, loadRegularReport, reset])

  const handlePasswordSubmit = useCallback(async () => {
    if (!sharePassword.trim()) {
      message.warning('请输入访问密码')
      return
    }
    setPasswordModalVisible(false)
    await loadPublicReportWithPassword(sharePassword)
    setSharePassword('')
  }, [sharePassword, loadPublicReportWithPassword])

  const handleRefresh = useCallback(async () => {
    if (refreshLockRef.current) return
    try {
      refreshLockRef.current = true
      message.loading({ content: '正在刷新...', key: 'refresh', duration: 0 })

      if (isShareMode && token) {
        const password = sharePassword || undefined
        const data = await executePublicReport(token, params, password)
        usePreviewStore.setState({ reportData: data })
      } else {
        await executeReportStore()
      }

      message.success({ content: '刷新成功', key: 'refresh' })
    } catch {
      message.error({ content: '刷新失败', key: 'refresh' })
    } finally {
      refreshLockRef.current = false
    }
  }, [isShareMode, token, params, sharePassword, executeReportStore])

  const handleExport = useCallback(async (type: 'excel' | 'pdf' | 'html') => {
    try {
      message.loading({ content: `正在导出${type.toUpperCase()}...`, key: 'export', duration: 0 })
      let blob: Blob

      if (isShareMode && token) {
        const password = sharePassword || undefined
        if (type === 'excel') {
          blob = await exportPublicReportExcel(token, params, password)
        } else if (type === 'pdf') {
          blob = await exportPublicReportPdf(token, params, password)
        } else {
          blob = new Blob(['<html><body>HTML export placeholder</body></html>'], { type: 'text/html' })
        }
      } else if (id) {
        if (type === 'excel') {
          blob = await exportReportExcel(Number(id), params)
        } else if (type === 'pdf') {
          blob = await exportReportPdf(Number(id), params)
        } else {
          blob = new Blob(['<html><body>HTML export placeholder</body></html>'], { type: 'text/html' })
        }
      } else {
        throw new Error('无效的报表标识')
      }

      const fileName = isShareMode
        ? `${publicReportInfo?.name || 'report'}.${type === 'excel' ? 'xlsx' : type === 'pdf' ? 'pdf' : 'html'}`
        : `${reportInfo?.name || 'report'}.${type === 'excel' ? 'xlsx' : type === 'pdf' ? 'pdf' : 'html'}`

      downloadBlob(blob, fileName)
      message.success({ content: '导出成功', key: 'export' })
    } catch {
      message.error({ content: '导出失败', key: 'export' })
    }
  }, [isShareMode, token, id, params, sharePassword, publicReportInfo, reportInfo])

  const handleShare = useCallback(() => {
    const shareData = {
      title: isShareMode ? publicReportInfo?.name || '报表分享' : reportInfo?.name || '报表分享',
      text: isShareMode ? publicReportInfo?.description || '分享一份报表给您查看' : reportInfo?.description || '分享一份报表给您查看',
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
  }, [isShareMode, publicReportInfo, reportInfo])

  const currentReportName = isShareMode ? publicReportInfo?.name : reportInfo?.name
  const currentReportDesc = isShareMode ? publicReportInfo?.description : reportInfo?.description

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
            {currentReportName || '报表预览'}
            {isShareMode && (
              <LockOutlined
                style={{ fontSize: 12, color: '#999', marginLeft: 6 }}
              />
            )}
          </Title>
          {currentReportDesc && (
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
              {currentReportDesc}
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

      <Modal
        title="请输入访问密码"
        open={passwordModalVisible}
        onCancel={() => {
          setPasswordModalVisible(false)
          setSharePassword('')
        }}
        onOk={handlePasswordSubmit}
        okText="确认"
        cancelText="取消"
      >
        <Input.Password
          placeholder="请输入分享密码"
          value={sharePassword}
          onChange={(e) => setSharePassword(e.target.value)}
          onPressEnter={handlePasswordSubmit}
          autoFocus
        />
      </Modal>
    </div>
  )
}

export default H5ReportPage
