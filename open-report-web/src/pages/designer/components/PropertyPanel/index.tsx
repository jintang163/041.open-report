import { useState, useEffect } from 'react'
import {
  Tabs,
  Form,
  Input,
  InputNumber,
  Select,
  ColorPicker,
  Radio,
  Checkbox,
  Switch,
  Button,
  Typography,
  Empty,
  Divider,
  Space,
  Card,
  List,
  Popconfirm
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  SettingOutlined,
  SaveOutlined
} from '@ant-design/icons'
import { useDesignerStore, type CellStyle, type CellDataBinding, type ChartConfig, type ConditionalFormatRule } from '../../store/designer'
import {
  cellPositionToRef,
  getCellValue,
  getCellCustomData,
  setCellStyle,
  setCellCustomData,
  setRowHeight,
  setColumnWidth,
  refreshLuckysheet,
  getSelectedRange,
  setRangeStyle
} from '../../utils/luckysheet'

const { Text } = Typography

const DATA_FORMATS = [
  { value: '', label: '常规' },
  { value: '0', label: '数值 (整数)' },
  { value: '0.00', label: '数值 (2位小数)' },
  { value: '0.00%', label: '百分比' },
  { value: '#,##0.00', label: '千分位' },
  { value: 'yyyy-MM-dd', label: '日期 (yyyy-MM-dd)' },
  { value: 'yyyy-MM-dd HH:mm:ss', label: '日期时间' },
  { value: 'HH:mm:ss', label: '时间' },
  { value: '¥#,##0.00', label: '货币 (人民币)' },
  { value: '$#,##0.00', label: '货币 (美元)' }
]

const PropertyPanel: React.FC = () => {
  const {
    selectedCell,
    dataSources,
    conditionalFormats,
    charts,
    selectedChartId,
    setConditionalFormatVisible,
    setChartConfigVisible,
    setExpressionEditorVisible,
    setSelectedChartId,
    removeChart,
    cellValue,
    writebackConfigs,
    setWritebackConfigVisible,
    templateId
  } = useDesignerStore()

  const [form] = Form.useForm()
  const [styleForm] = Form.useForm()
  const [bindingForm] = Form.useForm()
  const [writebackForm] = Form.useForm()

  const [cellData, setCellData] = useState<any>({})
  const [customData, setCustomData] = useState<any>({})
  const [binding, setBinding] = useState<CellDataBinding | null>(null)
  const [cellWritebackConfig, setCellWritebackConfig] = useState<any>({})

  useEffect(() => {
    if (selectedCell) {
      const value = getCellValue(selectedCell.row, selectedCell.col)
      const custom = getCellCustomData(selectedCell.row, selectedCell.col)
      const cellPos = cellPositionToRef(selectedCell.row, selectedCell.col)

      setCellData({ value })
      setCustomData(custom || {})
      setBinding(custom?.dataBinding || null)

      const writebackConfig = custom?.writeback || {}
      setCellWritebackConfig(writebackConfig)

      form.setFieldsValue({
        value: value ?? '',
        rowHeight: 25,
        colWidth: 80
      })

      styleForm.setFieldsValue({
        fontSize: 12,
        fontFamily: 'Microsoft YaHei',
        fontWeight: 'normal',
        fontStyle: 'normal',
        textDecoration: 'none',
        color: '#000000',
        backgroundColor: '#ffffff',
        textAlign: 'left',
        verticalAlign: 'middle',
        wrapText: false
      })

      bindingForm.setFieldsValue({
        type: custom?.dataBinding?.type || 'static',
        dataset: custom?.dataBinding?.dataset,
        field: custom?.dataBinding?.field,
        expression: custom?.dataBinding?.expression || value,
        format: custom?.dataBinding?.format || ''
      })

      writebackForm.setFieldsValue({
        editable: writebackConfig.editable || false,
        fieldName: writebackConfig.fieldName || '',
        fieldType: writebackConfig.fieldType || 'STRING',
        required: writebackConfig.required || false,
        validationRule: writebackConfig.validationRule || '',
        validationMessage: writebackConfig.validationMessage || '',
        cellPosition: cellPos
      })
    }
  }, [selectedCell, form, styleForm, bindingForm, writebackForm])

  const handleStyleChange = (changedValues: any) => {
    if (!selectedCell) return

    const style: Partial<CellStyle> = {}
    Object.keys(changedValues).forEach((key) => {
      ;(style as any)[key] = changedValues[key]
    })

    const range = getSelectedRange()
    if (range && (range.start.row !== range.end.row || range.start.col !== range.end.col)) {
      setRangeStyle(range, style)
    } else {
      setCellStyle(selectedCell.row, selectedCell.col, style)
    }
    refreshLuckysheet()
  }

  const handleRowHeightChange = (value: number) => {
    if (!selectedCell || !value) return
    setRowHeight(selectedCell.row, value)
    refreshLuckysheet()
  }

  const handleColWidthChange = (value: number) => {
    if (!selectedCell || !value) return
    setColumnWidth(selectedCell.col, value)
    refreshLuckysheet()
  }

  const handleBindingSave = async () => {
    if (!selectedCell) return
    try {
      const values = await bindingForm.validateFields()
      const newBinding: CellDataBinding = {
        type: values.type,
        dataset: values.dataset,
        field: values.field,
        expression: values.expression,
        format: values.format
      }

      setBinding(newBinding)
      setCellCustomData(selectedCell.row, selectedCell.col, {
        dataBinding: newBinding,
        expression: values.expression
      })
      refreshLuckysheet()
    } catch {
    }
  }

  const getAllDatasets = () => {
    const datasets: any[] = []
    dataSources.forEach((ds) => {
      if (ds.dataSets) {
        datasets.push(...ds.dataSets)
      }
    })
    return datasets
  }

  const getDatasetByName = (name: string) => {
    return getAllDatasets().find((d) => d.name === name)
  }

  const handleOpenExpressionEditor = () => {
    setExpressionEditorVisible(true, bindingForm.getFieldValue('expression'))
  }

  const handleWritebackSave = async () => {
    if (!selectedCell) return
    try {
      const values = await writebackForm.validateFields()
      const newWritebackConfig = {
        editable: values.editable,
        fieldName: values.fieldName,
        fieldType: values.fieldType,
        required: values.required,
        validationRule: values.validationRule,
        validationMessage: values.validationMessage,
        cellPosition: values.cellPosition
      }

      setCellWritebackConfig(newWritebackConfig)
      const existingCustom = getCellCustomData(selectedCell.row, selectedCell.col) || {}
      setCellCustomData(selectedCell.row, selectedCell.col, {
        ...existingCustom,
        writeback: newWritebackConfig
      })
      refreshLuckysheet()
    } catch {
    }
  }

  const cellPropertiesTab = (
    <div style={{ padding: 12 }}>
      {selectedCell ? (
        <Form form={form} layout="vertical">
          <Card size="small" title="基本信息" style={{ marginBottom: 12 }}>
            <Space direction="vertical" style={{ width: '100%' }} size={12}>
              <div>
                <Text type="secondary">单元格位置</Text>
                <div>
                  <Text strong>
                    {cellPositionToRef(selectedCell.row, selectedCell.col)}
                  </Text>
                  <Text type="secondary" style={{ marginLeft: 12 }}>
                    行: {selectedCell.row + 1}, 列: {selectedCell.col + 1}
                  </Text>
                </div>
              </div>

              <Form.Item label="单元格值" name="value">
                <Input.TextArea
                  rows={3}
                  placeholder="输入单元格内容或公式"
                  onChange={(e) => {
                    // can be handled on blur
                  }}
                />
              </Form.Item>
            </Space>
          </Card>

          <Card size="small" title="尺寸">
            <Space>
              <Form.Item label="行高" name="rowHeight" style={{ marginBottom: 0 }}>
                <InputNumber
                  min={15}
                  max={400}
                  onBlur={(e) => handleRowHeightChange(parseInt(e.target.value, 10))}
                />
              </Form.Item>
              <Form.Item label="列宽" name="colWidth" style={{ marginBottom: 0 }}>
                <InputNumber
                  min={30}
                  max={500}
                  onBlur={(e) => handleColWidthChange(parseInt(e.target.value, 10))}
                />
              </Form.Item>
            </Space>
          </Card>
        </Form>
      ) : (
        <Empty description={<Text type="secondary">请选择单元格</Text>} />
      )}
    </div>
  )

  const styleTab = (
    <div style={{ padding: 12 }}>
      {selectedCell ? (
        <Form
          form={styleForm}
          layout="vertical"
          onValuesChange={handleStyleChange}
        >
          <Card size="small" title="字体" style={{ marginBottom: 12 }}>
            <Space direction="vertical" style={{ width: '100%' }} size={12}>
              <Space wrap>
                <Form.Item label="字体" name="fontFamily" style={{ marginBottom: 0 }}>
                  <Select
                    style={{ width: 140 }}
                    options={[
                      { value: 'Arial', label: 'Arial' },
                      { value: 'Microsoft YaHei', label: '微软雅黑' },
                      { value: 'SimSun', label: '宋体' },
                      { value: 'SimHei', label: '黑体' },
                      { value: 'KaiTi', label: '楷体' },
                      { value: 'Times New Roman', label: 'Times New Roman' }
                    ]}
                  />
                </Form.Item>
                <Form.Item label="字号" name="fontSize" style={{ marginBottom: 0 }}>
                  <InputNumber min={8} max={72} style={{ width: 80 }} />
                </Form.Item>
              </Space>

              <Space>
                <Form.Item name="fontWeight" valuePropName="checked" noStyle>
                  <Checkbox
                    checked={styleForm.getFieldValue('fontWeight') === 'bold'}
                    onChange={(e) =>
                      handleStyleChange({ fontWeight: e.target.checked ? 'bold' : 'normal' })
                    }
                    style={{ fontWeight: 'bold' }}
                  >
                    B
                  </Checkbox>
                </Form.Item>
                <Form.Item name="fontStyle" valuePropName="checked" noStyle>
                  <Checkbox
                    checked={styleForm.getFieldValue('fontStyle') === 'italic'}
                    onChange={(e) =>
                      handleStyleChange({ fontStyle: e.target.checked ? 'italic' : 'normal' })
                    }
                    style={{ fontStyle: 'italic' }}
                  >
                    I
                  </Checkbox>
                </Form.Item>
                <Form.Item name="textDecoration" noStyle>
                  <Checkbox.Group
                    value={
                      styleForm.getFieldValue('textDecoration') === 'underline'
                        ? ['underline']
                        : styleForm.getFieldValue('textDecoration') === 'line-through'
                          ? ['line-through']
                          : styleForm.getFieldValue('textDecoration') === 'underline-line-through'
                            ? ['underline', 'line-through']
                            : []
                    }
                    onChange={(vals) => {
                      let val = 'none'
                      if (vals.includes('underline') && vals.includes('line-through')) {
                        val = 'underline-line-through'
                      } else if (vals.includes('underline')) {
                        val = 'underline'
                      } else if (vals.includes('line-through')) {
                        val = 'line-through'
                      }
                      handleStyleChange({ textDecoration: val })
                    }}
                  >
                    <Checkbox value="underline" style={{ textDecoration: 'underline' }}>
                      U
                    </Checkbox>
                    <Checkbox value="line-through" style={{ textDecoration: 'line-through' }}>
                      S
                    </Checkbox>
                  </Checkbox.Group>
                </Form.Item>
              </Space>

              <Space>
                <Form.Item label="文字颜色" name="color" style={{ marginBottom: 0 }}>
                  <ColorPicker showText />
                </Form.Item>
                <Form.Item label="背景色" name="backgroundColor" style={{ marginBottom: 0 }}>
                  <ColorPicker showText />
                </Form.Item>
              </Space>
            </Space>
          </Card>

          <Card size="small" title="对齐">
            <Space direction="vertical" style={{ width: '100%' }} size={12}>
              <Form.Item label="水平对齐" name="textAlign" style={{ marginBottom: 0 }}>
                <Radio.Group>
                  <Radio.Button value="left">左</Radio.Button>
                  <Radio.Button value="center">中</Radio.Button>
                  <Radio.Button value="right">右</Radio.Button>
                </Radio.Group>
              </Form.Item>

              <Form.Item label="垂直对齐" name="verticalAlign" style={{ marginBottom: 0 }}>
                <Radio.Group>
                  <Radio.Button value="top">上</Radio.Button>
                  <Radio.Button value="middle">中</Radio.Button>
                  <Radio.Button value="bottom">下</Radio.Button>
                </Radio.Group>
              </Form.Item>

              <Form.Item name="wrapText" valuePropName="checked">
                <Switch checkedChildren="自动换行" unCheckedChildren="不换行" />
              </Form.Item>
            </Space>
          </Card>
        </Form>
      ) : (
        <Empty description={<Text type="secondary">请选择单元格</Text>} />
      )}
    </div>
  )

  const dataBindingTab = (
    <div style={{ padding: 12 }}>
      {selectedCell ? (
        <Form form={bindingForm} layout="vertical">
          <Card size="small" title="数据绑定类型" style={{ marginBottom: 12 }}>
            <Form.Item name="type" rules={[{ required: true }]} style={{ marginBottom: 0 }}>
              <Radio.Group>
                <Radio value="static">静态值</Radio>
                <Radio value="field">字段绑定</Radio>
                <Radio value="expression">表达式</Radio>
              </Radio.Group>
            </Form.Item>
          </Card>

          <Form.Item noStyle shouldUpdate={(prev, cur) => prev.type !== cur.type}>
            {({ getFieldValue }) => {
              const type = getFieldValue('type')

              if (type === 'field') {
                return (
                  <Card size="small" title="字段设置" style={{ marginBottom: 12 }}>
                    <Space direction="vertical" style={{ width: '100%' }} size={12}>
                      <Form.Item
                        label="数据集"
                        name="dataset"
                        rules={[{ required: true, message: '请选择数据集' }]}
                        style={{ marginBottom: 0 }}
                      >
                        <Select
                          placeholder="请选择数据集"
                          onChange={() => bindingForm.setFieldsValue({ field: undefined })}
                        >
                          {getAllDatasets().map((ds) => (
                            <Select.Option key={ds.id} value={ds.name}>
                              {ds.name}
                            </Select.Option>
                          ))}
                        </Select>
                      </Form.Item>

                      <Form.Item noStyle shouldUpdate={(prev, cur) => prev.dataset !== cur.dataset}>
                        {({ getFieldValue: getVal }) => {
                          const datasetName = getVal('dataset')
                          const dataset = getDatasetByName(datasetName)
                          return (
                            <Form.Item
                              label="字段"
                              name="field"
                              rules={[{ required: true, message: '请选择字段' }]}
                              style={{ marginBottom: 0 }}
                            >
                              <Select placeholder="请选择字段">
                                {(dataset?.fields || []).map((f: any) => (
                                  <Select.Option key={f.name} value={f.name}>
                                    {f.label || f.name} ({f.type})
                                  </Select.Option>
                                ))}
                              </Select>
                            </Form.Item>
                          )
                        }}
                      </Form.Item>
                    </Space>
                  </Card>
                )
              }

              if (type === 'expression') {
                return (
                  <Card size="small" title="表达式设置" style={{ marginBottom: 12 }}>
                    <Space direction="vertical" style={{ width: '100%' }} size={12}>
                      <Form.Item
                        label="表达式"
                        name="expression"
                        rules={[{ required: true, message: '请输入表达式' }]}
                        style={{ marginBottom: 0 }}
                      >
                        <Input.TextArea
                          rows={4}
                          placeholder="例如: SUM(${sales.amount}) 或 ${user.name}"
                        />
                      </Form.Item>
                      <Button type="link" onClick={handleOpenExpressionEditor} style={{ padding: 0 }}>
                        打开表达式编辑器
                      </Button>
                    </Space>
                  </Card>
                )
              }

              return (
                <Card size="small" title="静态值" style={{ marginBottom: 12 }}>
                  <Form.Item name="expression" style={{ marginBottom: 0 }}>
                    <Input.TextArea rows={3} placeholder="请输入静态值" />
                  </Form.Item>
                </Card>
              )
            }}
          </Form.Item>

          <Card size="small" title="数据格式" style={{ marginBottom: 12 }}>
            <Form.Item label="显示格式" name="format" style={{ marginBottom: 0 }}>
              <Select placeholder="请选择显示格式" allowClear>
                {DATA_FORMATS.map((f) => (
                  <Select.Option key={f.value} value={f.value}>
                    {f.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Card>

          <Button type="primary" onClick={handleBindingSave} block>
            保存绑定
          </Button>

          {binding && (
            <>
              <Divider>当前绑定</Divider>
              <Card size="small" type="inner">
                <Space direction="vertical" size={4}>
                  <Text type="secondary">类型: {binding.type}</Text>
                  {binding.dataset && <Text type="secondary">数据集: {binding.dataset}</Text>}
                  {binding.field && <Text type="secondary">字段: {binding.field}</Text>}
                  {binding.expression && <Text type="secondary">表达式: <Text code>{binding.expression}</Text></Text>}
                  {binding.format && <Text type="secondary">格式: {binding.format}</Text>}
                </Space>
              </Card>
            </>
          )}
        </Form>
      ) : (
        <Empty description={<Text type="secondary">请选择单元格</Text>} />
      )}
    </div>
  )

  const chartTab = (
    <div style={{ padding: 12 }}>
      <div style={{ marginBottom: 12 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setChartConfigVisible(true)} block>
          新建图表
        </Button>
      </div>

      {charts.length === 0 ? (
        <Empty description={<Text type="secondary">暂无图表，点击上方按钮新建</Text>} />
      ) : (
        <List
          bordered
          dataSource={charts}
          renderItem={(item: ChartConfig) => (
            <List.Item
              style={{
                background: selectedChartId === item.id ? '#e6f4ff' : undefined,
                cursor: 'pointer',
                padding: '8px 12px'
              }}
              onClick={() => setSelectedChartId(item.id)}
              actions={[
                <Button
                  key="edit"
                  type="text"
                  size="small"
                  icon={<EditOutlined />}
                  onClick={(e) => { e.stopPropagation(); setChartConfigVisible(true, item) }}
                >
                  编辑
                </Button>,
                <Popconfirm
                  key="delete"
                  title="确定删除此图表？"
                  onConfirm={(e) => { e?.stopPropagation(); removeChart(item.id) }}
                  onCancel={(e) => e?.stopPropagation()}
                >
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={(e) => e.stopPropagation()}
                  >
                    删除
                  </Button>
                </Popconfirm>
              ]}
            >
              <List.Item.Meta
                title={item.title || `图表 (${item.type})`}
                description={
                  <Space direction="vertical" size={2}>
                    <Text type="secondary">类型: {item.type}</Text>
                    {item.datasetName && <Text type="secondary">数据集: {item.datasetName}</Text>}
                    {item.xAxisField && <Text type="secondary">X轴: {item.xAxisField}</Text>}
                    {item.yAxisFields && item.yAxisFields.length > 0 && (
                      <Text type="secondary">Y轴: {item.yAxisFields.join(', ')}</Text>
                    )}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      )}

      <Divider>条件格式</Divider>

      <div style={{ marginBottom: 12 }}>
        <Button icon={<PlusOutlined />} onClick={() => setConditionalFormatVisible(true)} block>
          新建条件格式
        </Button>
      </div>

      {conditionalFormats.length === 0 ? (
        <Empty description={<Text type="secondary">暂无条件格式</Text>} />
      ) : (
        <List
          bordered
          size="small"
          dataSource={conditionalFormats}
          renderItem={(item: ConditionalFormatRule) => (
            <List.Item
              actions={[
                <Button
                  key="delete"
                  type="text"
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                >
                  删除
                </Button>
              ]}
            >
              <List.Item.Meta
                title={item.type}
                description={<Text type="secondary">范围: {item.range}</Text>}
              />
            </List.Item>
          )}
        />
      )}
    </div>
  )

  const writebackTab = (
    <div style={{ padding: 12 }}>
      <div style={{ marginBottom: 12 }}>
        <Button
          type="primary"
          icon={<SettingOutlined />}
          onClick={() => setWritebackConfigVisible(true)}
          block
        >
          配置回写规则
        </Button>
      </div>

      {writebackConfigs.length > 0 && (
        <Card size="small" title="已配置的回写规则" style={{ marginBottom: 12 }}>
          <List
            size="small"
            dataSource={writebackConfigs}
            renderItem={(config) => (
              <List.Item
                actions={[
                  <Button
                    key="edit"
                    type="text"
                    size="small"
                    icon={<EditOutlined />}
                    onClick={(e) => { e.stopPropagation(); setWritebackConfigVisible(true, config) }}
                  >
                    编辑
                  </Button>,
                  <Popconfirm
                    key="delete"
                    title="确定删除此回写规则？"
                    onConfirm={(e) => { e?.stopPropagation() }}
                    onCancel={(e) => e?.stopPropagation()}
                  >
                    <Button
                      type="text"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={(e) => e.stopPropagation()}
                    >
                      删除
                    </Button>
                  </Popconfirm>
                ]}
              >
                <List.Item.Meta
                  title={config.tableName}
                  description={
                    <Space direction="vertical" size={2}>
                      <Text type="secondary">主键: {config.primaryKeyField}</Text>
                      <Text type="secondary">字段数: {config.fields?.length || 0}</Text>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        </Card>
      )}

      <Divider>单元格回写配置</Divider>

      {selectedCell ? (
        <Form form={writebackForm} layout="vertical">
          <Card size="small" title="单元格信息" style={{ marginBottom: 12 }}>
            <Space direction="vertical" style={{ width: '100%' }} size={12}>
              <div>
                <Text type="secondary">单元格位置</Text>
                <div>
                  <Text strong>
                    {cellPositionToRef(selectedCell.row, selectedCell.col)}
                  </Text>
                </div>
              </div>
            </Space>
          </Card>

          <Card size="small" title="回写设置" style={{ marginBottom: 12 }}>
            <Space direction="vertical" style={{ width: '100%' }} size={12}>
              <Form.Item name="editable" valuePropName="checked" style={{ marginBottom: 0 }}>
                <Switch checkedChildren="可编辑" unCheckedChildren="只读" />
              </Form.Item>

              <Form.Item noStyle shouldUpdate={(prev, cur) => prev.editable !== cur.editable}>
                {({ getFieldValue }) => {
                  const editable = getFieldValue('editable')
                  if (!editable) return null

                  return (
                    <Space direction="vertical" style={{ width: '100%' }} size={12}>
                      <Form.Item
                        label="字段名"
                        name="fieldName"
                        rules={[{ required: true, message: '请输入字段名' }]}
                        style={{ marginBottom: 0 }}
                      >
                        <Input placeholder="请输入对应的数据表字段名" />
                      </Form.Item>

                      <Form.Item
                        label="字段类型"
                        name="fieldType"
                        rules={[{ required: true, message: '请选择字段类型' }]}
                        style={{ marginBottom: 0 }}
                      >
                        <Select>
                          <Select.Option value="STRING">字符串</Select.Option>
                          <Select.Option value="NUMBER">数值</Select.Option>
                          <Select.Option value="DATE">日期</Select.Option>
                          <Select.Option value="DATETIME">日期时间</Select.Option>
                          <Select.Option value="BOOLEAN">布尔值</Select.Option>
                        </Select>
                      </Form.Item>

                      <Form.Item name="required" valuePropName="checked" style={{ marginBottom: 0 }}>
                        <Switch checkedChildren="必填" unCheckedChildren="可选" />
                      </Form.Item>

                      <Form.Item
                        label="校验规则(正则)"
                        name="validationRule"
                        style={{ marginBottom: 0 }}
                      >
                        <Input placeholder="例如: ^\\d+$ 表示只能输入数字" />
                      </Form.Item>

                      <Form.Item
                        label="校验失败提示"
                        name="validationMessage"
                        style={{ marginBottom: 0 }}
                      >
                        <Input placeholder="例如: 只能输入数字" />
                      </Form.Item>
                    </Space>
                  )
                }}
              </Form.Item>
            </Space>
          </Card>

          <Button type="primary" icon={<SaveOutlined />} onClick={handleWritebackSave} block>
            保存单元格配置
          </Button>

          {cellWritebackConfig?.editable && (
            <>
              <Divider>当前配置</Divider>
              <Card size="small" type="inner">
                <Space direction="vertical" size={4}>
                  <Text type="secondary">字段名: {cellWritebackConfig.fieldName}</Text>
                  <Text type="secondary">类型: {cellWritebackConfig.fieldType}</Text>
                  <Text type="secondary">必填: {cellWritebackConfig.required ? '是' : '否'}</Text>
                  {cellWritebackConfig.validationRule && (
                    <Text type="secondary">校验: {cellWritebackConfig.validationRule}</Text>
                  )}
                </Space>
              </Card>
            </>
          )}
        </Form>
      ) : (
        <Empty description={<Text type="secondary">请选择单元格</Text>} />
      )}
    </div>
  )

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '12px 12px 0', borderBottom: '1px solid #f0f0f0' }}>
        <Text strong style={{ fontSize: 14 }}>属性面板</Text>
      </div>
      <div style={{ flex: 1, overflow: 'auto' }}>
        <Tabs
          defaultActiveKey="cell"
          size="small"
          items={[
            { key: 'cell', label: '单元格', children: cellPropertiesTab },
            { key: 'style', label: '样式', children: styleTab },
            { key: 'binding', label: '数据绑定', children: dataBindingTab },
            { key: 'chart', label: '图表配置', children: chartTab },
            { key: 'writeback', label: '回写配置', children: writebackTab }
          ]}
        />
      </div>
    </div>
  )
}

export default PropertyPanel
