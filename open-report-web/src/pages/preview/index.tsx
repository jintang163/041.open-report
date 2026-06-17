import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Spin, Typography, Divider, Result, Button, Tabs, Space } from 'antd'
import { ArrowLeftOutlined, EditOutlined, HistoryOutlined } from '@ant-design/icons'
import ParamPanel from './components/ParamPanel'
import ReportTable from './components/ReportTable'
import ReportChart from './components/ReportChart'
import ReportToolbar from './components/ReportToolbar'
import MobileReportView from './components/MobileReportView'
import EditableTable from './components/EditableTable'
import WritebackHistoryModal from './components/WritebackHistoryModal'
import { usePreviewStore } from './store/preview'
import { useWritebackStore } from './store/writeback'
import { getReportById } from '@/api/report'
import { ReportTemplate } from '@/types'
import { isMobileDevice } from './utils/report'

const { Title } = Typography

type ViewMode = 'view' | 'edit'

const PreviewPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [reportInfo, setReportInfo] = useState<ReportTemplate | null>(null)
  const [initLoading, setInitLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [viewMode, setViewMode] = useState<ViewMode>('view')

  const setReportId = usePreviewStore((state) => state.setReportId)
  const setReportName = usePreviewStore((state) => state.setReportName)
  const loadParams = usePreviewStore((state) => state.loadParams)
  const executeReport = usePreviewStore((state) => state.executeReport)
  const reportData = usePreviewStore((state) => state.reportData)
  const isFullscreen = usePreviewStore((state) => state.isFullscreen)
  const isMobile = usePreviewStore((state) => state.isMobile)
  const toggleMobile = usePreviewStore((state) => state.toggleMobile)
  const reset = usePreviewStore((state) => state.reset)

  const {
    writebackConfigs,
    historyVisible,
    loadWritebackConfigs,
    setHistoryVisible,
    reset: resetWriteback
  } = useWritebackStore()

  useEffect(() => {
    toggleMobile(isMobileDevice())
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
        await executeReport()
        await loadWritebackConfigs(reportId)
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
      resetWriteback()
    }
  }, [id, setReportId, setReportName, loadParams, executeReport, reset, loadWritebackConfigs, resetWriteback])

  const pageStyle: React.CSSProperties = isFullscreen
    ? {
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 1000,
        background: '#f5f5f5',
        padding: 16,
        overflow: 'auto'
      }
    : {
        padding: 16,
        background: '#f5f5f5',
        minHeight: '100vh'
      }

  if (initLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" tip="加载报表中..." />
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ padding: 24 }}>
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
    <div style={pageStyle}>
      <div style={{ maxWidth: isFullscreen ? '100%' : 1600, margin: '0 auto' }}>
        {!isFullscreen && (
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16 }}>
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate(-1)}
              style={{ marginRight: 16 }}
            >
              返回
            </Button>
            <Title level={4} style={{ margin: 0, flex: 1 }}>
              {reportInfo?.name || '报表预览'}
            </Title>
            <Space>
              {writebackConfigs.length > 0 && (
                <>
                  <Tabs
                    activeKey={viewMode}
                    onChange={(key) => setViewMode(key as ViewMode)}
                    size="small"
                    style={{ marginBottom: 0 }}
                    items={[
                      {
                        key: 'view',
                        label: (
                          <span>
                            <HistoryOutlined style={{ marginRight: 4 }} />
                            查看模式
                          </span>
                        )
                      },
                      {
                        key: 'edit',
                        label: (
                          <span>
                            <EditOutlined style={{ marginRight: 4 }} />
                            填报模式
                          </span>
                        )
                      }
                    ]}
                  />
                  <Button
                    icon={<HistoryOutlined />}
                    onClick={() => setHistoryVisible(true)}
                  >
                    提交历史
                  </Button>
                </>
              )}
            </Space>
          </div>
        )}

        <ParamPanel collapsible={!isMobile} />

        <ReportToolbar showViewMode contentId="report-content" />

        <div id="report-content">
          {reportData?.title && (
            <>
              <div style={{ background: '#fff', borderRadius: 8, padding: '16px 24px', marginBottom: 16 }}>
                <Title level={3} style={{ margin: 0, textAlign: 'center' }}>
                  {reportData.title}
                </Title>
                {reportData.summary && (
                  <div style={{ textAlign: 'center', color: '#666', marginTop: 8 }}>
                    {reportData.summary}
                  </div>
                )}
              </div>
              <Divider style={{ margin: '0 0 16px 0' }} />
            </>
          )}

          {viewMode === 'edit' && writebackConfigs.length > 0 ? (
            <div
              style={{
                background: '#fff',
                borderRadius: 8,
                padding: 16,
                marginBottom: 16
              }}
            >
              <EditableTable
                reportId={Number(id)}
                data={reportData?.data || []}
                loading={reportData === null}
                onSubmit={() => executeReport()}
              />
            </div>
          ) : isMobile ? (
            <MobileReportView />
          ) : (
            <>
              {reportData?.charts && reportData.charts.length > 0 && <ReportChart />}

              {reportData?.html ? (
                <div
                  style={{
                    background: '#fff',
                    borderRadius: 8,
                    padding: 16,
                    marginBottom: 16,
                    overflow: 'auto'
                  }}
                  dangerouslySetInnerHTML={{ __html: reportData.html }}
                />
              ) : (
                <ReportTable showSearch />
              )}
            </>
          )}
        </div>

        {id && (
          <WritebackHistoryModal
            reportId={Number(id)}
            visible={historyVisible}
            onClose={() => setHistoryVisible(false)}
          />
        )}
      </div>
    </div>
  )
}

export default PreviewPage
