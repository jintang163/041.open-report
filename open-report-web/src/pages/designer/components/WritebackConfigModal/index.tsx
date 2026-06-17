import React, { useState, useEffect } from 'react'
import {
  Modal,
  Form,
  Input,
  Select,
  Switch,
  Button,
  Space,
  Table,
  Card,
  Typography,
  Popconfirm,
  message
} from 'antd'
import { PlusOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { useDesignerStore } from '../../store/designer'
import {
  createWritebackConfig,
  updateWritebackConfig,
  deleteWritebackConfig,
  getWritebackConfigList
} from '@/api/writeback'
import type { WritebackConfig, WritebackField, FieldType } from '@/types'

const { Title, Text } = Typography

interface WritebackConfigModalProps {
  reportId: number
  onConfigChange?: () => void
}

const WritebackConfigModal: React.FC<WritebackConfigModalProps> = ({ reportId, onConfigChange }) => {
  const {
    writebackConfigVisible,
    currentWritebackConfig,
    setWritebackConfigVisible,
    dataSources,
    setWritebackConfigs,
    addWritebackConfig,
    updateWritebackConfig: updateStoreConfig,
    removeWritebackConfig
  } = useDesignerStore()

  const [form] = Form.useForm()
  const [fieldForm] = Form.useForm()
  const [fieldModalVisible, setFieldModalVisible] = useState(false)
  const [editingField, setEditingField] = useState<WritebackField | null>(null)
  const [fields, setFields] = useState<WritebackField[]>([])
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (writebackConfigVisible) {
      if (currentWritebackConfig) {
        form.setFieldsValue({
          dataSourceId: currentWritebackConfig.dataSourceId,
          tableName: currentWritebackConfig.tableName,
          primaryKeyField: currentWritebackConfig.primaryKeyField,
          primaryKeyColumn: currentWritebackConfig.primaryKeyColumn,
          versionField: currentWritebackConfig.versionField,
          logicDeleteField: currentWritebackConfig.logicDeleteField,
          logicDeleteValue: currentWritebackConfig.logicDeleteValue,
          logicNotDeleteValue: currentWritebackConfig.logicNotDeleteValue,
          batchSupport: currentWritebackConfig.batchSupport === 1,
          transactionEnable: currentWritebackConfig.transactionEnable === 1
        })
        setFields(currentWritebackConfig.fields || [])
      } else {
        form.resetFields()
        form.setFieldsValue({
          batchSupport: true,
          transactionEnable: true
        })
        setFields([])
      }
    }
  }, [writebackConfigVisible, currentWritebackConfig, form])

  const handleCancel = () => {
    setWritebackConfigVisible(false)
    setEditingField(null)
    setFieldModalVisible(false)
  }

  const handleOk = async () => {
    try {
      const values = await form.validateFields()
      setSubmitting(true)

      const configData: WritebackConfig = {
        reportId,
        dataSourceId: values.dataSourceId,
        tableName: values.tableName,
        primaryKeyField: values.primaryKeyField,
        primaryKeyColumn: values.primaryKeyColumn,
        versionField: values.versionField,
        logicDeleteField: values.logicDeleteField,
        logicDeleteValue: values.logicDeleteValue,
        logicNotDeleteValue: values.logicNotDeleteValue,
        batchSupport: values.batchSupport ? 1 : 0,
        transactionEnable: values.transactionEnable ? 1 : 0,
        fields
      }

      if (currentWritebackConfig?.id) {
        configData.id = currentWritebackConfig.id
        await updateWritebackConfig(configData)
        updateStoreConfig(configData)
        message.success('更新成功')
      } else {
        await createWritebackConfig(configData)
        const configs = await getWritebackConfigList(reportId)
        setWritebackConfigs(configs)
        message.success('创建成功')
      }

      onConfigChange?.()
      handleCancel()
    } catch (error: any) {
      message.error(error.message || '保存失败')
    } finally {
      setSubmitting(false)
    }
  }

  const handleAddField = () => {
    setEditingField(null)
    fieldForm.resetFields()
    fieldForm.setFieldsValue({
      fieldType: 'STRING' as FieldType,
      editable: true,
      required: false
    })
    setFieldModalVisible(true)
  }

  const handleEditField = (field: WritebackField) => {
    setEditingField(field)
    fieldForm.setFieldsValue({
      ...field,
      editable: field.editable === 1,
      required: field.required === 1
    })
    setFieldModalVisible(true)
  }

  const handleDeleteField = (index: number) => {
    setFields(fields.filter((_, i) => i !== index))
  }

  const handleFieldOk = async () => {
    try {
      const values = await fieldForm.validateFields()
      const newField: WritebackField = {
        ...editingField,
        cellPosition: values.cellPosition,
        fieldName: values.fieldName,
        fieldType: values.fieldType,
        editable: values.editable ? 1 : 0,
        required: values.required ? 1 : 0,
        defaultValue: values.defaultValue,
        validationRule: values.validationRule,
        validationMessage: values.validationMessage
      }

      if (editingField) {
        setFields(fields.map(f => f.cellPosition === editingField.cellPosition ? newField : f))
      } else {
        setFields([...fields, newField])
      }
      setFieldModalVisible(false)
      setEditingField(null)
    } catch {
    }
  }

  const handleDeleteConfig = async () => {
    if (!currentWritebackConfig?.id) return
    try {
      await deleteWritebackConfig(currentWritebackConfig.id)
      removeWritebackConfig(currentWritebackConfig.id)
      message.success('删除成功')
      onConfigChange?.()
      handleCancel()
    } catch (error: any) {
      message.error(error.message || '删除失败')
    }
  }

  const fieldColumns = [
    {
      title: '单元格',
      dataIndex: 'cellPosition',
      key: 'cellPosition',
      width: 100
    },
    {
      title: '字段名',
      dataIndex: 'fieldName',
      key: 'fieldName'
    },
    {
      title: '类型',
      dataIndex: 'fieldType',
      key: 'fieldType',
      width: 100
    },
    {
      title: '可编辑',
      dataIndex: 'editable',
      key: 'editable',
      width: 80,
      render: (v: number) => (v === 1 ? '是' : '否')
    },
    {
      title: '必填',
      dataIndex: 'required',
      key: 'required',
      width: 80,
      render: (v: number) => (v === 1 ? '是' : '否')
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: WritebackField, index: number) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditField(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除此字段？"
            onConfirm={() => handleDeleteField(index)}
          >
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  return (
    <>
      <Modal
        title={currentWritebackConfig ? '编辑回写配置' : '新建回写配置'}
        open={writebackConfigVisible}
        onCancel={handleCancel}
        onOk={handleOk}
        confirmLoading={submitting}
        width={800}
        footer={(_, { OkBtn, CancelBtn }) => (
          <Space>
            {currentWritebackConfig && (
              <Popconfirm title="确定删除此回写配置？" onConfirm={handleDeleteConfig}>
                <Button danger>删除</Button>
              </Popconfirm>
            )}
            <CancelBtn />
            <OkBtn />
          </Space>
        )}
      >
        <Form form={form} layout="vertical">
          <Card size="small" title="基本配置" style={{ marginBottom: 12 }}>
            <Space direction="vertical" style={{ width: '100%' }} size={12}>
              <Form.Item
                label="数据源"
                name="dataSourceId"
                rules={[{ required: true, message: '请选择数据源' }]}
                style={{ marginBottom: 0 }}
              >
                <Select placeholder="请选择数据源">
                  {dataSources.map(ds => (
                    <Select.Option key={ds.id} value={ds.id}>
                      {ds.name}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>

              <Space wrap>
                <Form.Item
                  label="目标表名"
                  name="tableName"
                  rules={[{ required: true, message: '请输入表名' }]}
                  style={{ marginBottom: 0, width: 200 }}
                >
                  <Input placeholder="例如: user_info" />
                </Form.Item>

                <Form.Item
                  label="主键字段"
                  name="primaryKeyField"
                  rules={[{ required: true, message: '请输入主键字段' }]}
                  style={{ marginBottom: 0, width: 200 }}
                >
                  <Input placeholder="例如: id" />
                </Form.Item>

                <Form.Item
                  label="主键列"
                  name="primaryKeyColumn"
                  style={{ marginBottom: 0, width: 120 }}
                >
                  <Input placeholder="例如: A" />
                </Form.Item>
              </Space>

              <Space wrap>
                <Form.Item
                  label="版本字段(乐观锁)"
                  name="versionField"
                  style={{ marginBottom: 0, width: 180 }}
                >
                  <Input placeholder="例如: version" />
                </Form.Item>

                <Form.Item
                  label="逻辑删除字段"
                  name="logicDeleteField"
                  style={{ marginBottom: 0, width: 180 }}
                >
                  <Input placeholder="例如: deleted" />
                </Form.Item>

                <Form.Item
                  label="删除值"
                  name="logicDeleteValue"
                  style={{ marginBottom: 0, width: 100 }}
                >
                  <Input placeholder="例如: 1" />
                </Form.Item>

                <Form.Item
                  label="未删除值"
                  name="logicNotDeleteValue"
                  style={{ marginBottom: 0, width: 100 }}
                >
                  <Input placeholder="例如: 0" />
                </Form.Item>
              </Space>

              <Space>
                <Form.Item name="batchSupport" valuePropName="checked" style={{ marginBottom: 0 }}>
                  <Switch checkedChildren="支持批量" unCheckedChildren="仅单行" />
                </Form.Item>

                <Form.Item name="transactionEnable" valuePropName="checked" style={{ marginBottom: 0 }}>
                  <Switch checkedChildren="启用事务" unCheckedChildren="不启用" />
                </Form.Item>
              </Space>
            </Space>
          </Card>

          <Card
            size="small"
            title="字段映射"
            extra={
              <Button type="primary" size="small" icon={<PlusOutlined />} onClick={handleAddField}>
                添加字段
              </Button>
            }
          >
            <Table
              size="small"
              columns={fieldColumns}
              dataSource={fields}
              rowKey="cellPosition"
              pagination={false}
              scroll={{ y: 200 }}
            />
          </Card>
        </Form>
      </Modal>

      <Modal
        title={editingField ? '编辑字段' : '添加字段'}
        open={fieldModalVisible}
        onCancel={() => setFieldModalVisible(false)}
        onOk={handleFieldOk}
        width={500}
      >
        <Form form={fieldForm} layout="vertical">
          <Space direction="vertical" style={{ width: '100%' }} size={12}>
            <Space wrap>
              <Form.Item
                label="单元格位置"
                name="cellPosition"
                rules={[{ required: true, message: '请输入单元格位置' }]}
                style={{ marginBottom: 0, width: 120 }}
              >
                <Input placeholder="例如: A1" />
              </Form.Item>

              <Form.Item
                label="字段名"
                name="fieldName"
                rules={[{ required: true, message: '请输入字段名' }]}
                style={{ marginBottom: 0, width: 180 }}
              >
                <Input placeholder="例如: user_name" />
              </Form.Item>

              <Form.Item
                label="字段类型"
                name="fieldType"
                rules={[{ required: true, message: '请选择类型' }]}
                style={{ marginBottom: 0, width: 120 }}
              >
                <Select>
                  <Select.Option value="STRING">字符串</Select.Option>
                  <Select.Option value="NUMBER">数值</Select.Option>
                  <Select.Option value="DATE">日期</Select.Option>
                  <Select.Option value="DATETIME">日期时间</Select.Option>
                  <Select.Option value="BOOLEAN">布尔值</Select.Option>
                </Select>
              </Form.Item>
            </Space>

            <Space>
              <Form.Item name="editable" valuePropName="checked" style={{ marginBottom: 0 }}>
                <Switch checkedChildren="可编辑" unCheckedChildren="只读" />
              </Form.Item>

              <Form.Item name="required" valuePropName="checked" style={{ marginBottom: 0 }}>
                <Switch checkedChildren="必填" unCheckedChildren="可选" />
              </Form.Item>
            </Space>

            <Form.Item
              label="默认值"
              name="defaultValue"
              style={{ marginBottom: 0 }}
            >
              <Input placeholder="选填" />
            </Form.Item>

            <Form.Item
              label="校验规则(正则表达式)"
              name="validationRule"
              style={{ marginBottom: 0 }}
            >
              <Input placeholder="例如: ^\\d+$" />
            </Form.Item>

            <Form.Item
              label="校验失败提示"
              name="validationMessage"
              style={{ marginBottom: 0 }}
            >
              <Input placeholder="例如: 只能输入数字" />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </>
  )
}

export default WritebackConfigModal
