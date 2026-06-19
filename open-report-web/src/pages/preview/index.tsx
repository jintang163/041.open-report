import React, { useEffect, useState, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Spin,
  Typography,
  Divider,
  Result,
  Button,
  Tabs,
  Space,
  Alert,
  Tag,
  Switch,
  message,
  Tooltip,
  Segmented,
  Dropdown,
  Badge,
  Layout
} from 'antd'
import {
  ArrowLeftOutlined,
  EditOutlined,
  HistoryOutlined,
  SyncOutlined,
  DatabaseOutlined,
  CloudServerOutlined,
  SettingOutlined,
  BarChartOutlined,
  ClockCircleOutlined,
  DownOutlined,
  ShareAltOutlined,
  WarningOutlined
} from '@ant-design/icons'
import ParamPanel from './components/ParamPanel'
import ReportTable from './components/ReportTable'
import ReportChart from './components/ReportChart'
import ReportToolbar from './components/ReportToolbar'
import MobileReportView from './components/MobileReportView'
import EditableTable from './components/EditableTable'
import WritebackHistoryModal from './components/WritebackHistoryModal'
import SnapshotConfigModal from './components/SnapshotConfigModal'
import SnapshotHistoryPanel from './components/SnapshotHistoryPanel'
import SnapshotCompareModal from './components/SnapshotCompareModal'
import LineageTreePanel from './components/LineageTreePanel'
import LineageTraceModal from './components/LineageTraceModal'
import ImpactAnalysisModal from './components/ImpactAnalysisModal'
import { usePreviewStore, SnapshotMode } from './store/preview'
import { useWritebackStore } from './store/writeback'
import { getReportById, compareSnapshotWithRealtime } from '@/api/report'
import { ReportTemplate, SnapshotComparisonResult } from '@/types'
import { isMobileDevice, formatParamValue } from './utils/report'
import { useReportWebSocket } from '@/hooks/useWebSocket'
import dayjs from 'dayjs'

const { Title } = Typography
const { Sider, Content } = Layout

type ViewMode = 'view' | 'edit'

const PreviewPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [reportInfo, setReportInfo] = useState<ReportTemplate | null>(null)
  const [initLoading, setInitLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [viewMode, setViewMode] = useState<ViewMode>('view')
  const [autoRefresh, setAutoRefresh] = useState<boolean>(true)

  const [snapshotConfigVisible, setSnapshotConfigVisible] = useState(false)
  const [snapshotHistoryVisible, setSnapshotHistoryVisible] = useState(false)
  const [compareModalVisible, setCompareModalVisible] = useState(false)
  const [compareLoading, setCompareLoading] = useState(false)
  const [compareResult, setCompareResult] = useState<SnapshotComparisonResult | null>(null)
  const [compareMode, setCompareMode] = useState<'snapshot-snapshot' | 'snapshot-realtime'>('snapshot-realtime')

  const [lineageVisible, setLineageVisible] = useState(false)
  const [traceModalVisible, setTraceModalVisible] = useState(false)
  const [traceField, setTraceField] = useState<{ field: string; title?: string } | null>(null)
  const [impactModalVisible, setImpactModalVisible] = useState(false)

  const setReportId = usePreviewStore((state) => state.setReportId)
  const setReportName = usePreviewStore((state) => state.setReportName)
  const params = usePreviewStore((state) => state.params)
  const paramValues = usePreviewStore((state) => state.paramValues)
  const loadParams = usePreviewStore((state) => state.loadParams)
  const executeReport = usePreviewStore((state) => state.executeReport)
  const executeReportWithSnapshotMode = usePreviewStore((state) => state.executeReportWithSnapshotMode)
  const reportData = usePreviewStore((state) => state.reportData)
  const isFullscreen = usePreviewStore((state) => state.isFullscreen)
  const isMobile = usePreviewStore((state) => state.isMobile)
  const toggleMobile = usePreviewStore((state) => state.toggleMobile)
  const reset = usePreviewStore((state) => state.reset)
  const exportExcel = usePreviewStore((state) => state.exportExcel)
  const exportPdf = usePreviewStore((state) => state.exportPdf)
  const reportName = usePreviewStore((state) => state.reportName)
  const snapshotMode = usePreviewStore((state) => state.snapshotMode)
  const setSnapshotMode = usePreviewStore((state) => state.setSnapshotMode)
  const selectedSnapshotId = usePreviewStore((state) => state.selectedSnapshotId)
  const setSelectedSnapshotId = usePreviewStore((state) => state.setSelectedSnapshotId)
  const snapshotInfo = usePreviewStore((state) => state.snapshotInfo)
  const snapshotConfig = usePreviewStore((state) => state.snapshotConfig)
  const loadSnapshotConfig = usePreviewStore((state) => state.loadSnapshotConfig)
  const snapshotList = usePreviewStore((state) => state.snapshotList)
  const loadSnapshotList = usePreviewStore((state) => state.loadSnapshotList)
  const snapshotLoading = usePreviewStore((state) => state.snapshotLoading)

  const {
    writebackConfigs,
    historyVisible,
    loadWritebackConfigs,
    setHistoryVisible,
    reset: resetWriteback
  } = useWritebackStore()

  const refreshLockRef = useRef(false)

  const { isConnected, shouldRefresh, acknowledgeRefresh, lastMessage } = useReportWebSocket(
    id,
    () => {
      if (autoRefresh && !refreshLockRef.current) {
        refreshLockRef.current = true
        message.info('检测到数据变更，正在自动刷新...')
        setTimeout(() => {
          executeReport().finally(() => {
            refreshLockRef.current = false
            acknowledgeRefresh()
          })
        }, 300)
      }
    }
  )

  const handleManualRefresh = async () => {
    message.loading({ content: '正在刷新数据...', key: 'refresh', duration: 0 })
    try {
      if (snapshotMode === 'realtime') {
        await executeReport()
      } else {
        await executeReportWithSnapshotMode()
      }
      acknowledgeRefresh()
      message.success({ content: '刷新成功', key: 'refresh' })
    } catch {
      message.error({ content: '刷新失败', key: 'refresh' })
    }
  }

  const handleSnapshotModeChange = async (mode: SnapshotMode) => {
    setSnapshotMode(mode)
    if (mode === 'snapshot' && !selectedSnapshotId) {
      message.info('请从快照历史中选择一个快照')
      setSnapshotHistoryVisible(true)
      return
    }
    message.loading({ content: '正在加载数据...', key: 'snapshot-load', duration: 0 })
    try {
      await executeReportWithSnapshotMode()
      message.success({ content: mode === 'realtime' ? '已切换到实时数据' : (mode === 'latest' ? '已加载最新快照' : '已加载快照数据'), key: 'snapshot-load' })
    } catch (err: any) {
      message.error({ content: err?.message || '加载失败', key: 'snapshot-load' })
    }
  }

  const handleSelectSnapshot = async (snapshotId: number) => {
    setSelectedSnapshotId(snapshotId)
    setSnapshotMode('snapshot')
    setSnapshotHistoryVisible(false)
    message.loading({ content: '正在加载快照数据...', key: 'snapshot-load', duration: 0 })
    try {
      await executeReportWithSnapshotMode()
      message.success({ content: '快照数据加载成功', key: 'snapshot-load' })
    } catch (err: any) {
      message.error({ content: err?.message || '加载失败', key: 'snapshot-load' })
    }
  }

  const handleCompareWithRealtime = async (snapshotId: number) => {
    setCompareMode('snapshot-realtime')
    setCompareModalVisible(true)
    setCompareLoading(true)
    setCompareResult(null)
    try {
      const formattedParams = formatParamValue(params, paramValues)
      const result = await compareSnapshotWithRealtime(snapshotId, formattedParams)
      setCompareResult(result)
    } catch (err: any) {
      setCompareResult({ success: false, message: err?.message || '对比失败' })
    } finally {
      setCompareLoading(false)
    }
  }

  const handleSnapshotConfigSuccess = async () => {
    await loadSnapshotConfig()
    await loadSnapshotList()
  }

  const handleRefreshSnapshotList = async () => {
    await loadSnapshotList()
    await loadSnapshotConfig()
  }

  const handleFieldLineageTrace = (fieldName: string, fieldTitle?: string) => {
    setTraceField({ field: fieldName, title: fieldTitle })
    setTraceModalVisible(true)
  }

  const handleReportClick = (reportId: number) => {
    navigate(`/preview/${reportId}`)
  }

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

        await Promise.all([
          loadParams(),
          executeReport(),
          loadWritebackConfigs(reportId),
          loadSnapshotConfig(),
          loadSnapshotList()
        ])
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
  }, [id, setReportId, setReportName, loadParams, executeReport, reset, loadWritebackConfigs, resetWriteback, loadSnapshotConfig, loadSnapshotList])

  const handleMobileExport = useCallback(async (type: 'excel' | 'pdf' | 'html') => {
    if (type === 'excel') {
      await exportExcel()
    } else if (type === 'pdf') {
      await exportPdf()
    } else if (type === 'html') {
      message.info('HTML 导出功能开发中')
    }
  }, [exportExcel, exportPdf])

  const handleMobileShare = useCallback(() => {
    const shareData = {
      title: reportName || '报表分享',
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
  }, [reportName, reportInfo])

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

  const snapshotModeOptions = [
    { label: <span><CloudServerOutlined /> 实时模式</span>, value: 'realtime' },
    { label: <span><ClockCircleOutlined /> 最新快照</span>, value: 'latest' },
    { label: <span><DatabaseOutlined /> 指定快照</span>, value: 'snapshot' }
  ]

  return (
    <div style={pageStyle}>
      <div style={{ maxWidth: isFullscreen ? '100%' : 1600, margin: '0 auto' }}>
        {!isFullscreen && (
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 16, flexWrap: 'wrap', gap: 12 }}>
            <Button
              icon={<ArrowLeftOutlined />}
              onClick={() => navigate(-1)}
              style={{ marginRight: 8 }}
            >
              返回
            </Button>
            <Title level={4} style={{ margin: 0, flex: 1, minWidth: 200 }}>
              {reportInfo?.name || '报表预览'}
            </Title>
            <Space wrap size="small">
              <Tag color={isConnected ? 'green' : 'default'}>
                {isConnected ? '● 实时连接' : '○ 离线'}
              </Tag>
              <Switch
                size="small"
                checked={autoRefresh}
                onChange={setAutoRefresh}
                checkedChildren="自动刷新"
                unCheckedChildren="自动刷新"
              />
              <Badge count={snapshotList.length} size="small" offset={[-4, 4]}>
                <Dropdown
                  menu={{
                    selectedKeys: [snapshotMode],
                    onClick: ({ key }) => handleSnapshotModeChange(key as SnapshotMode),
                    items: snapshotModeOptions.map(opt => ({
                      key: opt.value,
                      label: opt.label
                    }))
                  }}
                  trigger={['click']}
                  placement="bottomRight"
                >
                  <Button>
                    <Space>
                      {snapshotMode === 'realtime' && <><CloudServerOutlined /> 实时模式</>}
                      {snapshotMode === 'latest' && <><ClockCircleOutlined /> 最新快照</>}
                      {snapshotMode === 'snapshot' && <><DatabaseOutlined /> 指定快照</>}
                      <DownOutlined />
                    </Space>
                  </Button>
                </Dropdown>
              </Badge>
              <Tooltip title="快照历史记录">
                <Button
                  icon={<HistoryOutlined />}
                  onClick={() => setSnapshotHistoryVisible(true)}
                >
                  历史快照
                </Button>
              </Tooltip>
              <Tooltip title="快照配置管理">
                <Button
                  icon={<SettingOutlined />}
                  type={snapshotConfig?.enabled === 1 ? 'primary' : 'default'}
                  onClick={() => setSnapshotConfigVisible(true)}
                >
                  快照配置
                </Button>
              </Tooltip>
              {snapshotInfo?.snapshotId && (
                <Tooltip title="与实时数据对比">
                  <Button
                    icon={<BarChartOutlined />}
                    onClick={() => handleCompareWithRealtime(snapshotInfo.snapshotId!)}
                  >
                    数据对比
                  </Button>
                </Tooltip>
              )}
              <Tooltip title="数据血缘分析">
                <Button
                  icon={<ShareAltOutlined />}
                  type={lineageVisible ? 'primary' : 'default'}
                  onClick={() => setLineageVisible(!lineageVisible)}
                >
                  血缘分析
                </Button>
              </Tooltip>
              <Tooltip title="影响分析 - 检查表/字段变更影响">
                <Button
                  icon={<WarningOutlined />}
                  onClick={() => setImpactModalVisible(true)}
                >
                  影响分析
                </Button>
              </Tooltip>
              <Button
                icon={<SyncOutlined spin={shouldRefresh} />}
                onClick={handleManualRefresh}
                type={shouldRefresh ? 'primary' : 'default'}
              >
                {shouldRefresh ? '有新数据' : '刷新'}
              </Button>
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

        {snapshotInfo?.isSnapshot && (
          <Alert
            type="info"
            showIcon
            icon={<DatabaseOutlined />}
            style={{ marginBottom: 16 }}
            message={
              <Space>
                <strong>快照模式</strong>
                <Tag color="cyan">{snapshotInfo.snapshotName}</Tag>
                <span>版本: {snapshotInfo.dataVersion}</span>
                <span>
                  生成时间: {snapshotInfo.snapshotTime ? dayjs(snapshotInfo.snapshotTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
                </span>
                {snapshotInfo.expireTime && (
                  <Tag color="orange">
                    有效期至: {dayjs(snapshotInfo.expireTime).format('YYYY-MM-DD HH:mm')}
                  </Tag>
                )}
              </Space>
            }
            action={
              snapshotMode !== 'realtime' && (
                <Button size="small" onClick={() => handleSnapshotModeChange('realtime')}>
                  切换到实时数据
                </Button>
              )
            }
          />
        )}

        {shouldRefresh && !autoRefresh && (
          <Alert
            message="检测到报表有新数据变更"
            description={
              lastMessage
                ? `变更类型: ${lastMessage.type}${
                    lastMessage.payload?.changeType ? ' (' + lastMessage.payload.changeType + ')' : ''
                  }`
                : ''
            }
            type="info"
            showIcon
            action={
              <Space>
                <Button size="small" type="primary" onClick={handleManualRefresh}>
                  立即刷新
                </Button>
                <Button size="small" onClick={acknowledgeRefresh}>
                  忽略
                </Button>
              </Space>
            }
            style={{ marginBottom: 16 }}
            closable
            onClose={acknowledgeRefresh}
          />
        )}

        <ParamPanel collapsible={!isMobile} />

        <ReportToolbar showViewMode contentId="report-content" />

        <Layout style={{ background: 'transparent', gap: lineageVisible ? 16 : 0 }}>
          {lineageVisible && !isMobile && (
            <Sider
              width={360}
              style={{
                background: 'transparent',
                height: 'calc(100vh - 200px)',
                position: 'sticky',
                top: 16,
                overflow: 'hidden'
              }}
            >
              <LineageTreePanel
                reportId={Number(id)}
                reportName={reportName || reportInfo?.name || ''}
                onFieldClick={(fieldName) => handleFieldLineageTrace(fieldName)}
              />
            </Sider>
          )}

          <Content>
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
            <MobileReportView onExport={handleMobileExport} onShare={handleMobileShare} />
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
          </Content>
        </Layout>

        {id && (
          <WritebackHistoryModal
            reportId={Number(id)}
            visible={historyVisible}
            onClose={() => setHistoryVisible(false)}
          />
        )}

        {snapshotConfigVisible && (
          <SnapshotConfigModal
            visible={snapshotConfigVisible}
            reportId={Number(id)}
            reportName={reportName || reportInfo?.name || ''}
            existingConfig={snapshotConfig}
            onClose={() => setSnapshotConfigVisible(false)}
            onSuccess={handleSnapshotConfigSuccess}
          />
        )}

        <SnapshotHistoryPanel
          visible={snapshotHistoryVisible}
          snapshotList={snapshotList}
          loading={snapshotLoading}
          selectedSnapshotId={selectedSnapshotId}
          onClose={() => setSnapshotHistoryVisible(false)}
          onSelectSnapshot={handleSelectSnapshot}
          onCompare={handleCompareWithRealtime}
          onRefresh={handleRefreshSnapshotList}
        />

        <SnapshotCompareModal
          visible={compareModalVisible}
          loading={compareLoading}
          result={compareResult}
          mode={compareMode}
          onClose={() => {
            setCompareModalVisible(false)
            setCompareResult(null)
          }}
          onRefresh={() => {
            if (selectedSnapshotId && compareMode === 'snapshot-realtime') {
              handleCompareWithRealtime(selectedSnapshotId)
            }
          }}
        />

        {traceField && (
          <LineageTraceModal
            visible={traceModalVisible}
            reportId={Number(id)}
            reportField={traceField.field}
            fieldTitle={traceField.title}
            onClose={() => {
              setTraceModalVisible(false)
              setTraceField(null)
            }}
          />
        )}

        <ImpactAnalysisModal
          visible={impactModalVisible}
          onClose={() => setImpactModalVisible(false)}
          onReportClick={handleReportClick}
        />
      </div>
    </div>
  )
}

export default PreviewPage
