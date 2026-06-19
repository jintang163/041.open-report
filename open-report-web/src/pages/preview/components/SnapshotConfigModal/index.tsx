import React, { useEffect } from 'react'
import {
  Modal,
  Form,
  Input,
  InputNumber,
  Switch,
  Select,
  Button,
  Space,
  Row,
  Col,
  message,
  Descriptions,
  Tag,
  Divider,
  Tooltip
} from 'antd'
import { QuestionCircleOutlined } from '@ant-design/icons'
import {
  createSnapshotConfig,
  updateSnapshotConfig,
  deleteSnapshotConfig,
  createSnapshotManual
} from '@/api/report'
import type { ReportSnapshotConfig } from '@/types'
import dayjs from 'dayjs'

const { TextArea } = Input

interface SnapshotConfigModalProps {
  visible: boolean
  reportId: number
  reportName: string
  existingConfig: ReportSnapshotConfig | null
  onClose: () => void
  onSuccess: () => void
}

const CRON_PRESETS = [
  { label: '每小时 (0分)', value: '0 0 * * * ?' },
  { label: '每天 凌晨2点', value: '0 0 2 * * ?' },
  { label: '每天 凌晨3点', value: '0 0 3 * * ?' },
  { label: '每周一 凌晨2点', value: '0 0 2 ? * MON' },
  { label: '每月1号 凌晨2点', value: '0 0 2 1 * ?' },
  { label: '自定义', value: 'custom' }
]

const SnapshotConfigModal: React.FC<SnapshotConfigModalProps> = ({
  visible,
  reportId,
  reportName,
  existingConfig,
  onClose,
  onSuccess
}) => {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = React.useState(false)
  const [creatingSnapshot, setCreatingSnapshot] = React.useState(false)
  const [customCron, setCustomCron] = React.useState(false)

  useEffect(() => {
    if (visible) {
      if (existingConfig) {
        form.setFieldsValue({
          enabled: existingConfig.enabled === 1,
          cronPreset: CRON_PRESETS.find(p => p.value === existingConfig.cronExpression)?.value || 'custom',
          cronExpression: existingConfig.cronExpression,
          retentionDays: existingConfig.retentionDays || 30,
          maxSnapshots: existingConfig.maxSnapshots || 100,
          snapshotType: existingConfig.snapshotType || 'FULL',
          shardEnabled: existingConfig.shardEnabled === 1,
          shardThresholdRows: existingConfig.shardThresholdRows || 50000,
          shardPageSize: existingConfig.shardPageSize || 1000,
          description: existingConfig.description || ''
        })
        setCustomCron(!CRON_PRESETS.find(p => p.value === existingConfig.cronExpression))
      } else {
        form.resetFields()
        form.setFieldsValue({
          enabled: true,
          cronPreset: '0 0 2 * * ?',
          retentionDays: 30,
          maxSnapshots: 100,
          snapshotType: 'FULL',
          shardEnabled: true,
          shardThresholdRows: 50000,
          shardPageSize: 1000
        })
        setCustomCron(false)
      }
    }
  }, [visible, existingConfig, form])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setSubmitting(true)

      const cronValue = customCron ? values.cronExpression : values.cronPreset

      const payload: Partial<ReportSnapshotConfig> = {
        reportId,
        reportName,
        enabled: values.enabled ? 1 : 0,
        cronExpression: cronValue,
        retentionDays: values.retentionDays,
        maxSnapshots: values.maxSnapshots,
        snapshotType: values.snapshotType,
        shardEnabled: values.shardEnabled ? 1 : 0,
        shardThresholdRows: values.shardThresholdRows,
        shardPageSize: values.shardPageSize,
        description: values.description
      }

      if (existingConfig?.id) {
        payload.id = existingConfig.id
        await updateSnapshotConfig(payload)
        message.success('快照配置更新成功')
      } else {
        await createSnapshotConfig(payload)
        message.success('快照配置创建成功')
      }

      onSuccess()
      onClose()
    } catch (err: any) {
      if (err?.errorFields) {
        return
      }
      message.error(existingConfig?.id ? '更新失败' : '创建失败')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async () => {
    if (!existingConfig?.id) return
    Modal.confirm({
      title: '确认删除',
      content: '删除快照配置将同时删除所有相关快照数据，是否继续？',
      okText: '确认删除',
      okType: 'danger',
      onOk: async () => {
        try {
          await deleteSnapshotConfig(existingConfig.id!)
          message.success('删除成功')
          onSuccess()
          onClose()
        } catch {
          message.error('删除失败')
        }
      }
    })
  }

  const handleCreateNow = async () => {
    if (!existingConfig?.id) return
    try {
      setCreatingSnapshot(true)
      const result = await createSnapshotManual(existingConfig.id)
      if (result?.success) {
        message.success(
          `快照创建成功！行数: ${result.rowCount?.toLocaleString() || 0}, 耗时: ${(result.executeTime || 0) / 1000}s`
        )
        onSuccess()
      } else {
        message.error(result?.message || '创建失败')
      }
    } catch (err: any) {
      message.error(err?.message || '创建失败')
    } finally {
      setCreatingSnapshot(false)
    }
  }

  const formatBytes = (bytes: number) => {
    if (!bytes) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  return (
    <Modal
      title={existingConfig?.id ? '编辑快照配置' : '新建快照配置'}
      open={visible}
      onCancel={onClose}
      width={720}
      footer={[
        <Space key="footer">
          {existingConfig?.id && (
            <Button danger onClick={handleDelete}>
              删除配置
            </Button>
          )}
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" loading={submitting} onClick={handleSubmit}>
            {existingConfig?.id ? '保存修改' : '创建配置'}
          </Button>
        </Space>
      ]}
    >
      {existingConfig && (
        <>
          <Descriptions column={2} size="small" bordered style={{ marginBottom: 16 }}>
            <Descriptions.Item label="已生成快照">
              <Tag color="blue">{existingConfig.snapshotCount || 0} 个</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="最近快照时间">
              {existingConfig.lastSnapshotTime
                ? dayjs(existingConfig.lastSnapshotTime).format('YYYY-MM-DD HH:mm:ss')
                : '-'}
            </Descriptions.Item>
          </Descriptions>
          <Space style={{ marginBottom: 16 }}>
            <Button type="dashed" icon={<QuestionCircleOutlined />} onClick={handleCreateNow} loading={creatingSnapshot}>
              立即生成快照
            </Button>
          </Space>
          <Divider style={{ margin: '12px 0 16px' }} />
        </>
      )}

      <Form form={form} layout="vertical">
        <Row gutter={16}>
          <Col span={24}>
            <Form.Item name="enabled" label="启用快照" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="cronPreset"
              label="刷新周期"
              rules={[{ required: true, message: '请选择刷新周期' }]}
            >
              <Select
                options={CRON_PRESETS}
                onChange={(val) => setCustomCron(val === 'custom')}
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            {customCron && (
              <Form.Item
                name="cronExpression"
                label={
                  <Space>
                    Cron 表达式
                    <Tooltip title="Cron 表达式格式: 秒 分 时 日 月 周，例：0 0 2 * * ? 表示每天凌晨2点">
                      <QuestionCircleOutlined />
                    </Tooltip>
                  </Space>
                }
                rules={[{ required: true, message: '请输入 Cron 表达式' }]}
              >
                <Input placeholder="例如：0 0 2 * * ?" />
              </Form.Item>
            )}
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="retentionDays"
              label={
                <Space>
                  快照保留天数
                  <Tooltip title="超过保留天数的快照将被自动清理">
                    <QuestionCircleOutlined />
                  </Tooltip>
                </Space>
              }
              rules={[{ required: true, message: '请输入保留天数' }]}
            >
              <InputNumber min={1} max={3650} style={{ width: '100%' }} addonAfter="天" />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="maxSnapshots"
              label={
                <Space>
                  最大快照数量
                  <Tooltip title="超过最大数量时，最旧的快照将被删除">
                    <QuestionCircleOutlined />
                  </Tooltip>
                </Space>
              }
              rules={[{ required: true, message: '请输入最大快照数量' }]}
            >
              <InputNumber min={1} max={10000} style={{ width: '100%' }} addonAfter="个" />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="snapshotType"
              label="快照类型"
              rules={[{ required: true, message: '请选择快照类型' }]}
            >
              <Select
                options={[
                  { label: '完整快照', value: 'FULL' },
                  { label: '增量快照', value: 'INCREMENTAL' }
                ]}
              />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="shardEnabled" label="启用分片存储" valuePropName="checked">
              <Switch checkedChildren="启用" unCheckedChildren="停用" />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              name="shardThresholdRows"
              label={
                <Space>
                  分片阈值（行）
                  <Tooltip title="数据行数超过此阈值时自动启用分片存储">
                    <QuestionCircleOutlined />
                  </Tooltip>
                </Space>
              }
              rules={[{ required: true, message: '请输入分片阈值' }]}
            >
              <InputNumber min={1000} max={1000000} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="shardPageSize"
              label={
                <Space>
                  分片每页行数
                  <Tooltip title="每个分片存储的行数">
                    <QuestionCircleOutlined />
                  </Tooltip>
                </Space>
              }
              rules={[{ required: true, message: '请输入每页行数' }]}
            >
              <InputNumber min={100} max={50000} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={24}>
            <Form.Item name="description" label="配置说明">
              <TextArea rows={3} placeholder="可选，描述该快照配置的用途" maxLength={500} showCount />
            </Form.Item>
          </Col>
        </Row>
      </Form>
    </Modal>
  )
}

export default SnapshotConfigModal
