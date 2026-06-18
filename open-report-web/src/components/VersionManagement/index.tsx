import { useState, useEffect } from 'react'
import {
  Modal,
  Table,
  Button,
  Space,
  Tag,
  Descriptions,
  Popconfirm,
  message,
  Tabs,
  Select,
  Row,
  Col,
  Card
} from 'antd'
import {
  HistoryOutlined,
  RollbackOutlined,
  EyeOutlined,
  DiffOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { ReportTemplateSnapshot, TemplateVersionDiffDTO } from '@/types'
import {
  getVersionList,
  rollbackToVersion,
  compareVersions,
  getVersionDetail,
  previewPublish
} from '@/api/report'

interface VersionManagementProps {
  templateId: number
  templateName: string
  open: boolean
  onClose: () => void
  onRollback?: () => void
}

const VersionManagement = ({
  templateId,
  templateName,
  open,
  onClose,
  onRollback
}: VersionManagementProps) => {
  const [versionList, setVersionList] = useState<ReportTemplateSnapshot[]>([])
  const [loading, setLoading] = useState(false)
  const [previewVisible, setPreviewVisible] = useState(false)
  const [previewSnapshot, setPreviewSnapshot] = useState<ReportTemplateSnapshot | null>(null)
  const [baseVersion, setBaseVersion] = useState<number | null>(null)
  const [targetVersion, setTargetVersion] = useState<number | null>(null)
  const [diffResult, setDiffResult] = useState<TemplateVersionDiffDTO | null>(null)
  const [diffLoading, setDiffLoading] = useState(false)

  const fetchVersionList = async () => {
    setLoading(true)
    try {
      const res = await getVersionList(templateId)
      setVersionList(res)
    } catch (e) {
      message.error('获取版本列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (open && templateId) {
      fetchVersionList()
    }
  }, [open, templateId])

  const getStatusTag = (status?: number) => {
    const statusMap: Record<number, { color: string; text: string }> = {
      0: { color: 'default', text: '草稿' },
      1: { color: 'orange', text: '待审批' },
      2: { color: 'green', text: '已发布' },
      3: { color: 'blue', text: '已下线' },
      4: { color: 'red', text: '已驳回' }
    }
    const info = statusMap[status || 0]
    return <Tag color={info.color}>{info.text}</Tag>
  }

  const handlePreview = async (record: ReportTemplateSnapshot) => {
    try {
      const res = await getVersionDetail(templateId, record.version!)
      setPreviewSnapshot(res)
      setPreviewVisible(true)
    } catch (e) {
      message.error('获取版本详情失败')
    }
  }

  const handlePreviewPublish = async () => {
    try {
      const res = await previewPublish(templateId)
      setPreviewSnapshot(res)
      setPreviewVisible(true)
    } catch (e) {
      message.error('获取发布预览失败')
    }
  }

  const handleRollback = async (record: ReportTemplateSnapshot) => {
    try {
      await rollbackToVersion(templateId, record.version!)
      message.success(`已回滚到版本 v${record.version}`)
      fetchVersionList()
      onRollback?.()
    } catch (e) {
      message.error('回滚失败')
    }
  }

  const handleCompare = async () => {
    if (!baseVersion || !targetVersion) {
      message.warning('请选择要对比的两个版本')
      return
    }
    setDiffLoading(true)
    try {
      const res = await compareVersions(templateId, baseVersion, targetVersion)
      setDiffResult(res)
    } catch (e) {
      message.error('版本对比失败')
    } finally {
      setDiffLoading(false)
    }
  }

  const getDiffTypeTag = (diffType?: string) => {
    const typeMap: Record<string, { color: string; text: string }> = {
      ADD: { color: 'green', text: '新增' },
      DELETE: { color: 'red', text: '删除' },
      MODIFY: { color: 'orange', text: '修改' }
    }
    const info = typeMap[diffType || '']
    return info ? <Tag color={info.color}>{info.text}</Tag> : null
  }

  const versionColumns: ColumnsType<ReportTemplateSnapshot> = [
    {
      title: '版本号',
      dataIndex: 'version',
      key: 'version',
      width: 80,
      render: (v: number) => <strong>v{v}</strong>
    },
    {
      title: '模板名称',
      dataIndex: 'templateName',
      key: 'templateName',
      width: 150
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => getStatusTag(status)
    },
    {
      title: '变更说明',
      dataIndex: 'changeLog',
      key: 'changeLog'
    },
    {
      title: '创建人',
      dataIndex: 'createByName',
      key: 'createByName',
      width: 100
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handlePreview(record)}
          >
            预览
          </Button>
          <Popconfirm
            title={`确定回滚到版本 v${record.version}?`}
            description="回滚后将生成新版本，当前内容将被覆盖"
            onConfirm={() => handleRollback(record)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" size="small" icon={<RollbackOutlined />} danger>
              回滚
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  const diffColumns: ColumnsType<NonNullable<TemplateVersionDiffDTO['diffItems']>[number]> = [
    {
      title: '字段',
      dataIndex: 'fieldLabel',
      key: 'fieldLabel',
      width: 120
    },
    {
      title: '变更类型',
      dataIndex: 'diffType',
      key: 'diffType',
      width: 100,
      render: (diffType: string) => getDiffTypeTag(diffType)
    },
    {
      title: `基准版本 (v${diffResult?.baseVersion})`,
      dataIndex: 'baseValue',
      key: 'baseValue',
      render: (v: string) => (
        <div style={{ maxHeight: 120, overflow: 'auto', fontFamily: 'monospace', fontSize: 12 }}>
          {v || '-'}
        </div>
      )
    },
    {
      title: `目标版本 (v${diffResult?.targetVersion})`,
      dataIndex: 'targetValue',
      key: 'targetValue',
      render: (v: string) => (
        <div style={{ maxHeight: 120, overflow: 'auto', fontFamily: 'monospace', fontSize: 12 }}>
          {v || '-'}
        </div>
      )
    }
  ]

  const versionOptions = versionList.map(v => ({
    label: `v${v.version} - ${v.templateName}`,
    value: v.version!
  }))

  return (
    <>
      <Modal
        title={
          <Space>
            <HistoryOutlined />
            版本管理 - {templateName}
          </Space>
        }
        open={open}
        onCancel={onClose}
        width={1200}
        footer={null}
        destroyOnClose
      >
        <Tabs
          defaultActiveKey="history"
          items={[
            {
              key: 'history',
              label: '版本历史',
              children: (
                <>
                  <div style={{ marginBottom: 16 }}>
                    <Button icon={<EyeOutlined />} onClick={handlePreviewPublish}>
                      发布预览
                    </Button>
                  </div>
                  <Table
                    rowKey="id"
                    loading={loading}
                    columns={versionColumns}
                    dataSource={versionList}
                    pagination={false}
                    scroll={{ x: 1000 }}
                  />
                </>
              )
            },
            {
              key: 'compare',
              label: '版本对比',
              children: (
                <div>
                  <Card size="small" style={{ marginBottom: 16 }}>
                    <Row gutter={16}>
                      <Col span={8}>
                        <Select
                          placeholder="选择基准版本"
                          style={{ width: '100%' }}
                          options={versionOptions}
                          value={baseVersion}
                          onChange={setBaseVersion}
                        />
                      </Col>
                      <Col span={8}>
                        <Select
                          placeholder="选择目标版本"
                          style={{ width: '100%' }}
                          options={versionOptions}
                          value={targetVersion}
                          onChange={setTargetVersion}
                        />
                      </Col>
                      <Col span={8}>
                        <Button
                          type="primary"
                          icon={<DiffOutlined />}
                          onClick={handleCompare}
                          disabled={!baseVersion || !targetVersion}
                        >
                          开始对比
                        </Button>
                      </Col>
                    </Row>
                  </Card>
                  {diffResult && (
                    <>
                      <Card size="small" style={{ marginBottom: 16 }}>
                        <Descriptions size="small" column={2}>
                          <Descriptions.Item label="基准版本">
                            v{diffResult.baseVersion} - {diffResult.baseVersionName}
                          </Descriptions.Item>
                          <Descriptions.Item label="目标版本">
                            v{diffResult.targetVersion} - {diffResult.targetVersionName}
                          </Descriptions.Item>
                          <Descriptions.Item label="基准创建人">
                            {diffResult.baseCreateByName}
                          </Descriptions.Item>
                          <Descriptions.Item label="目标创建人">
                            {diffResult.targetCreateByName}
                          </Descriptions.Item>
                          <Descriptions.Item label="基准时间">
                            {diffResult.baseCreateTime}
                          </Descriptions.Item>
                          <Descriptions.Item label="目标时间">
                            {diffResult.targetCreateTime}
                          </Descriptions.Item>
                        </Descriptions>
                      </Card>
                      <Table
                        rowKey="fieldName"
                        loading={diffLoading}
                        columns={diffColumns}
                        dataSource={diffResult.diffItems}
                        pagination={false}
                        scroll={{ x: 1000 }}
                      />
                    </>
                  )}
                </div>
              )
            }
          ]}
        />
      </Modal>

      <Modal
        title={`预览版本 - ${previewSnapshot?.templateName}`}
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        width={900}
        footer={[
          <Button key="close" onClick={() => setPreviewVisible(false)}>
            关闭
          </Button>
        ]}
      >
        {previewSnapshot && (
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Descriptions size="small" bordered column={2}>
              <Descriptions.Item label="版本号">v{previewSnapshot.version}</Descriptions.Item>
              <Descriptions.Item label="状态">
                {getStatusTag(previewSnapshot.status)}
              </Descriptions.Item>
              <Descriptions.Item label="模板名称">
                {previewSnapshot.templateName}
              </Descriptions.Item>
              <Descriptions.Item label="创建人">
                {previewSnapshot.createByName}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {previewSnapshot.createTime}
              </Descriptions.Item>
              <Descriptions.Item label="变更说明">
                {previewSnapshot.changeLog || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>
                {previewSnapshot.description || '-'}
              </Descriptions.Item>
            </Descriptions>
            <Card size="small" title="模板内容">
              <pre
                style={{
                  maxHeight: 400,
                  overflow: 'auto',
                  background: '#f5f5f5',
                  padding: 16,
                  borderRadius: 4
                }}
              >
                {JSON.stringify(
                  previewSnapshot.templateJson
                    ? JSON.parse(previewSnapshot.templateJson)
                    : {},
                  null,
                  2
                )}
              </pre>
            </Card>
          </Space>
        )}
      </Modal>
    </>
  )
}

export default VersionManagement
