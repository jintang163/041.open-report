import { useState } from 'react'
import {
  Card,
  Tabs,
  Button,
  Input,
  Select,
  Form,
  Space,
  Row,
  Col,
  Tag,
  message,
  Typography,
  InputNumber,
  DatePicker,
  Switch,
  Divider,
  Collapse,
  Alert,
  Tooltip,
  Copyable
} from 'antd'
import {
  CodeOutlined,
  LinkOutlined,
  ApiOutlined,
  CopyOutlined,
  ExperimentOutlined,
  FileTextOutlined,
  CloudDownloadOutlined,
  EyeOutlined
} from '@ant-design/icons'
import type { ReportTemplate } from '@/types'

const { Title, Text, Paragraph, Code } = Typography

const mockReports: ReportTemplate[] = [
  { id: 1, name: '销售月报', code: 'sales_monthly', status: 2 },
  { id: 2, name: '财务分析报表', code: 'finance_analysis', status: 2 },
  { id: 3, name: '库存统计', code: 'inventory_stat', status: 2 },
  { id: 4, name: '客户分析', code: 'customer_analysis', status: 2 }
]

const embedMethods = [
  {
    key: 'iframe',
    label: (
      <Space>
        <CodeOutlined />
        iframe 嵌入
      </Space>
    )
  },
  {
    key: 'url',
    label: (
      <Space>
        <LinkOutlined />
        URL 链接
      </Space>
    )
  },
  {
    key: 'api',
    label: (
      <Space>
        <ApiOutlined />
        API 调用
      </Space>
    )
  },
  {
    key: 'preview',
    label: (
      <Space>
        <ExperimentOutlined />
        实时预览
      </Space>
    )
  }
]

const EmbedDemo = () => {
  const [activeTab, setActiveTab] = useState('iframe')
  const [selectedReport, setSelectedReport] = useState<number>(1)
  const [embedToken, setEmbedToken] = useState('demo-token-xxxxxxxx')
  const [showToolbar, setShowToolbar] = useState(true)
  const [showHeader, setShowHeader] = useState(true)
  const [iframeWidth, setIframeWidth] = useState('100%')
  const [iframeHeight, setIframeHeight] = useState(600)
  const [theme, setTheme] = useState<'light' | 'dark'>('light')
  const [lang, setLang] = useState<'zh-CN' | 'en-US'>('zh-CN')
  const [outputFormat, setOutputFormat] = useState<'html' | 'pdf' | 'excel'>('html')

  const baseUrl = `${window.location.origin}/report/viewer`

  const currentReport = mockReports.find(r => r.id === selectedReport)

  const getViewerUrl = (withToken = true) => {
    const params = new URLSearchParams()
    params.set('id', String(selectedReport))
    if (withToken) params.set('token', embedToken)
    if (!showToolbar) params.set('toolbar', '0')
    if (!showHeader) params.set('header', '0')
    if (theme !== 'light') params.set('theme', theme)
    if (lang !== 'zh-CN') params.set('lang', lang)
    return `${baseUrl}?${params.toString()}`
  }

  const getApiUrl = () => {
    return `${window.location.origin}/api/report/execute/${selectedReport}`
  }

  const getExportUrl = () => {
    const params = new URLSearchParams()
    params.set('token', embedToken)
    return `${window.location.origin}/api/report/export/${selectedReport}/${outputFormat}?${params.toString()}`
  }

  const handleCopy = (text: string, label = '内容') => {
    navigator.clipboard.writeText(text)
    message.success(`${label}已复制到剪贴板`)
  }

  const iframeCode = `<iframe
  src="${getViewerUrl()}"
  width="${iframeWidth}"
  height="${iframeHeight}px"
  frameborder="0"
  allowfullscreen
  style="border: 1px solid #e8e8e8; border-radius: 4px;">
</iframe>`

  const urlLinkCode = `<a href="${getViewerUrl()}" target="_blank">
  查看 ${currentReport?.name || '报表'}
</a>`

  const jsFetchCode = `// 使用 Fetch API 调用报表数据
const token = '${embedToken}';
const reportId = ${selectedReport};

fetch('${getApiUrl()}', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    // 报表参数
    // param1: 'value1',
    // param2: 'value2'
  })
})
  .then(response => response.json())
  .then(data => {
    console.log('报表数据:', data);
    // 处理返回的数据
    // data.html - 渲染后的 HTML
    // data.data - 原始数据
    // data.charts - 图表配置
  })
  .catch(error => console.error('请求失败:', error));`

  const axiosCode = `// 使用 Axios 调用报表数据
import axios from 'axios';

const response = await axios.post('${getApiUrl()}', {
  // 报表参数
  // param1: 'value1'
}, {
  headers: {
    'Authorization': 'Bearer ${embedToken}'
  }
});

console.log(response.data);`

  const jqueryCode = `// 使用 jQuery 调用报表数据
$.ajax({
  url: '${getApiUrl()}',
  type: 'POST',
  headers: {
    'Authorization': 'Bearer ${embedToken}'
  },
  contentType: 'application/json',
  data: JSON.stringify({
    // 报表参数
  }),
  success: function(data) {
    console.log(data);
  },
  error: function(xhr, status, error) {
    console.error(error);
  }
});`

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card size="small">
        <Space direction="vertical" size="small" style={{ width: '100%' }}>
          <Title level={4} style={{ margin: 0 }}>
            <FileTextOutlined style={{ marginRight: 8 }} />
            报表嵌入式集成示例
          </Title>
          <Text type="secondary">
            提供多种方式将报表嵌入到您的业务系统中，支持 iframe 嵌入、URL 链接、RESTful API 调用等集成方式
          </Text>
        </Space>
      </Card>

      <Card size="small" title="配置选项">
        <Row gutter={[24, 16]}>
          <Col xs={24} md={12} lg={8}>
            <Form.Item label="选择报表" style={{ marginBottom: 0 }}>
              <Select
                value={selectedReport}
                onChange={setSelectedReport}
                options={mockReports.map(r => ({ label: r.name, value: r.id }))}
                style={{ width: '100%' }}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12} lg={8}>
            <Form.Item label="访问 Token" style={{ marginBottom: 0 }}>
              <Input
                value={embedToken}
                onChange={(e) => setEmbedToken(e.target.value)}
                addonAfter={
                  <Tooltip title="复制 Token">
                    <CopyOutlined onClick={() => handleCopy(embedToken, 'Token')} />
                  </Tooltip>
                }
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12} lg={8}>
            <Form.Item label="主题" style={{ marginBottom: 0 }}>
              <Select
                value={theme}
                onChange={setTheme}
                style={{ width: '100%' }}
                options={[
                  { label: '浅色主题', value: 'light' },
                  { label: '深色主题', value: 'dark' }
                ]}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12} lg={8}>
            <Form.Item label="语言" style={{ marginBottom: 0 }}>
              <Select
                value={lang}
                onChange={setLang}
                style={{ width: '100%' }}
                options={[
                  { label: '简体中文', value: 'zh-CN' },
                  { label: 'English', value: 'en-US' }
                ]}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12} lg={8}>
            <Form.Item label="显示工具栏" style={{ marginBottom: 0 }}>
              <Switch checked={showToolbar} onChange={setShowToolbar} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12} lg={8}>
            <Form.Item label="显示页头" style={{ marginBottom: 0 }}>
              <Switch checked={showHeader} onChange={setShowHeader} />
            </Form.Item>
          </Col>
        </Row>
      </Card>

      <Card size="small" styles={{ body: { padding: 0 } }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={embedMethods}
          style={{ padding: '0 24px' }}
        />
        <Divider style={{ margin: 0 }} />

        {activeTab === 'iframe' && (
          <Space direction="vertical" size="middle" style={{ width: '100%', padding: 24 }}>
            <Alert
              type="info"
              showIcon
              message="iframe 嵌入方式"
              description="最常用的集成方式，将报表直接嵌入到您系统的页面中，支持自适应高度、全屏展示等功能。"
            />
            <Row gutter={16}>
              <Col xs={24} md={12}>
                <Form.Item label="宽度">
                  <Input value={iframeWidth} onChange={(e) => setIframeWidth(e.target.value)} />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item label="高度 (px)">
                  <InputNumber
                    value={iframeHeight}
                    onChange={(v) => setIframeHeight(v || 600)}
                    min={200}
                    max={2000}
                    style={{ width: '100%' }}
                  />
                </Form.Item>
              </Col>
            </Row>
            <Card
              size="small"
              title={
                <Space>
                  <CodeOutlined />
                  嵌入代码
                  <Button
                    size="small"
                    icon={<CopyOutlined />}
                    onClick={() => handleCopy(iframeCode, '嵌入代码')}
                  >
                    复制代码
                  </Button>
                </Space>
              }
            >
              <pre
                style={{
                  background: '#1e1e1e',
                  color: '#d4d4d4',
                  padding: 16,
                  borderRadius: 4,
                  overflowX: 'auto',
                  margin: 0,
                  fontSize: 13,
                  lineHeight: 1.6
                }}
              >
                <code>{iframeCode}</code>
              </pre>
            </Card>
            <Card size="small" title="嵌入效果预览">
              <div
                style={{
                  border: '1px dashed #d9d9d9',
                  borderRadius: 4,
                  padding: 16,
                  minHeight: iframeHeight,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: '#fafafa'
                }}
              >
                <div style={{ textAlign: 'center', color: '#888' }}>
                  <FileTextOutlined style={{ fontSize: 48, marginBottom: 12 }} />
                  <p><strong>{currentReport?.name}</strong> 预览区域</p>
                  <p style={{ fontSize: 12 }}>此处将显示 iframe 嵌入的报表内容</p>
                  <p style={{ fontSize: 12, color: '#aaa' }}>尺寸: {iframeWidth} × {iframeHeight}px</p>
                </div>
              </div>
            </Card>
          </Space>
        )}

        {activeTab === 'url' && (
          <Space direction="vertical" size="middle" style={{ width: '100%', padding: 24 }}>
            <Alert
              type="info"
              showIcon
              message="URL 链接方式"
              description="通过直接访问 URL 链接打开报表，支持在新窗口、当前窗口或弹窗中打开。"
            />
            <Card size="small" title="生成的 URL">
              <Space.Compact style={{ width: '100%' }}>
                <Input value={getViewerUrl()} readOnly />
                <Button icon={<CopyOutlined />} onClick={() => handleCopy(getViewerUrl(), 'URL')}>
                  复制
                </Button>
                <Button type="primary" icon={<EyeOutlined />} onClick={() => window.open(getViewerUrl(), '_blank')}>
                  打开
                </Button>
              </Space.Compact>
            </Card>
            <Card
              size="small"
              title={
                <Space>
                  <CodeOutlined />
                  HTML 链接代码
                  <Button
                    size="small"
                    icon={<CopyOutlined />}
                    onClick={() => handleCopy(urlLinkCode, '链接代码')}
                  >
                    复制代码
                  </Button>
                </Space>
              }
            >
              <pre
                style={{
                  background: '#1e1e1e',
                  color: '#d4d4d4',
                  padding: 16,
                  borderRadius: 4,
                  overflowX: 'auto',
                  margin: 0,
                  fontSize: 13,
                  lineHeight: 1.6
                }}
              >
                <code>{urlLinkCode}</code>
              </pre>
            </Card>
            <Card size="small" title="URL 参数说明">
              <TableLikeParams />
            </Card>
          </Space>
        )}

        {activeTab === 'api' && (
          <Space direction="vertical" size="middle" style={{ width: '100%', padding: 24 }}>
            <Alert
              type="info"
              showIcon
              message="API 调用方式"
              description="通过 RESTful API 获取报表数据，可自定义前端渲染，灵活度最高，适合深度集成场景。"
            />
            <Row gutter={16}>
              <Col xs={24} md={12}>
                <Form.Item label="导出格式" style={{ marginBottom: 0 }}>
                  <Select
                    value={outputFormat}
                    onChange={setOutputFormat}
                    style={{ width: '100%' }}
                    options={[
                      { label: 'HTML', value: 'html' },
                      { label: 'PDF', value: 'pdf' },
                      { label: 'Excel', value: 'excel' }
                    ]}
                  />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item label="导出链接" style={{ marginBottom: 0 }}>
                  <Space.Compact style={{ width: '100%' }}>
                    <Input value={getExportUrl()} readOnly />
                    <Button icon={<CopyOutlined />} onClick={() => handleCopy(getExportUrl(), '导出链接')}>
                      复制
                    </Button>
                  </Space.Compact>
                </Form.Item>
              </Col>
            </Row>
            <Collapse
              items={[
                {
                  key: '1',
                  label: <Space><ApiOutlined />Fetch API (原生 JavaScript)</Space>,
                  children: (
                    <>
                      <div style={{ marginBottom: 8, textAlign: 'right' }}>
                        <Button size="small" icon={<CopyOutlined />} onClick={() => handleCopy(jsFetchCode, 'Fetch 代码')}>
                          复制代码
                        </Button>
                      </div>
                      <pre style={codeStyle}>
                        <code>{jsFetchCode}</code>
                      </pre>
                    </>
                  )
                },
                {
                  key: '2',
                  label: <Space><ApiOutlined />Axios</Space>,
                  children: (
                    <>
                      <div style={{ marginBottom: 8, textAlign: 'right' }}>
                        <Button size="small" icon={<CopyOutlined />} onClick={() => handleCopy(axiosCode, 'Axios 代码')}>
                          复制代码
                        </Button>
                      </div>
                      <pre style={codeStyle}>
                        <code>{axiosCode}</code>
                      </pre>
                    </>
                  )
                },
                {
                  key: '3',
                  label: <Space><ApiOutlined />jQuery AJAX</Space>,
                  children: (
                    <>
                      <div style={{ marginBottom: 8, textAlign: 'right' }}>
                        <Button size="small" icon={<CopyOutlined />} onClick={() => handleCopy(jqueryCode, 'jQuery 代码')}>
                          复制代码
                        </Button>
                      </div>
                      <pre style={codeStyle}>
                        <code>{jqueryCode}</code>
                      </pre>
                    </>
                  )
                }
              ]}
              defaultActiveKey={['1']}
            />
            <Card size="small" title="API 接口说明">
              <Space direction="vertical" style={{ width: '100%' }}>
                <Paragraph style={{ marginBottom: 8 }}>
                  <Text strong>执行报表接口：</Text>
                  <Code>POST /api/report/execute/{'{id}'}</Code>
                </Paragraph>
                <Paragraph style={{ marginBottom: 8 }}>
                  <Text strong>导出 Excel：</Text>
                  <Code>POST /api/report/export/{'{id}'}/excel</Code>
                </Paragraph>
                <Paragraph style={{ marginBottom: 8 }}>
                  <Text strong>导出 PDF：</Text>
                  <Code>POST /api/report/export/{'{id}'}/pdf</Code>
                </Paragraph>
                <Paragraph style={{ marginBottom: 0 }}>
                  <Text strong>获取报表参数：</Text>
                  <Code>GET /api/report/parameters/{'{id}'}</Code>
                </Paragraph>
              </Space>
            </Card>
          </Space>
        )}

        {activeTab === 'preview' && (
          <Space direction="vertical" size="middle" style={{ width: '100%', padding: 24 }}>
            <Alert
              type="success"
              showIcon
              message="实时预览"
              description="配置参数后，实时预览报表展示效果。"
            />
            <Card size="small" title="报表参数设置">
              <Row gutter={16}>
                <Col xs={24} md={8}>
                  <Form.Item label="开始日期">
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item label="结束日期">
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item label="部门">
                    <Select
                      placeholder="请选择部门"
                      allowClear
                      style={{ width: '100%' }}
                      options={[
                        { label: '销售部', value: 'sales' },
                        { label: '财务部', value: 'finance' },
                        { label: '技术部', value: 'tech' }
                      ]}
                    />
                  </Form.Item>
                </Col>
              </Row>
              <Space>
                <Button type="primary">查询</Button>
                <Button>重置</Button>
                <Button icon={<CloudDownloadOutlined />}>导出 Excel</Button>
                <Button icon={<CloudDownloadOutlined />}>导出 PDF</Button>
              </Space>
            </Card>
            <Card size="small" title={`报表预览 - ${currentReport?.name}`}>
              <div
                style={{
                  minHeight: 400,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  borderRadius: 4,
                  color: '#fff'
                }}
              >
                <div style={{ textAlign: 'center' }}>
                  <FileTextOutlined style={{ fontSize: 64, marginBottom: 16, opacity: 0.8 }} />
                  <Title level={3} style={{ color: '#fff', margin: 0 }}>
                    {currentReport?.name}
                  </Title>
                  <p style={{ marginTop: 8, opacity: 0.8 }}>报表预览区域 - {theme === 'dark' ? '深色' : '浅色'}主题</p>
                  <Space style={{ marginTop: 16 }}>
                    <Tag color="blue">ID: {selectedReport}</Tag>
                    <Tag color="green">{lang === 'zh-CN' ? '中文' : 'English'}</Tag>
                    {showToolbar && <Tag color="orange">显示工具栏</Tag>}
                    {showHeader && <Tag color="purple">显示页头</Tag>}
                  </Space>
                </div>
              </div>
            </Card>
          </Space>
        )}
      </Card>
    </Space>
  )
}

const codeStyle: React.CSSProperties = {
  background: '#1e1e1e',
  color: '#d4d4d4',
  padding: 16,
  borderRadius: 4,
  overflowX: 'auto',
  margin: 0,
  fontSize: 13,
  lineHeight: 1.6
}

const TableLikeParams = () => {
  const params = [
    { name: 'id', required: true, desc: '报表 ID' },
    { name: 'token', required: true, desc: '访问令牌 (Embed Token)' },
    { name: 'toolbar', required: false, desc: '是否显示工具栏，1=显示，0=隐藏，默认 1' },
    { name: 'header', required: false, desc: '是否显示页头，1=显示，0=隐藏，默认 1' },
    { name: 'theme', required: false, desc: '主题，可选值: light / dark，默认 light' },
    { name: 'lang', required: false, desc: '语言，可选值: zh-CN / en-US，默认 zh-CN' }
  ]
  return (
    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
      <thead>
        <tr style={{ background: '#fafafa' }}>
          <th style={thStyle}>参数名</th>
          <th style={thStyle}>必填</th>
          <th style={thStyle}>说明</th>
        </tr>
      </thead>
      <tbody>
        {params.map(p => (
          <tr key={p.name}>
            <td style={tdStyle}><code>{p.name}</code></td>
            <td style={tdStyle}>{p.required ? <Tag color="red">是</Tag> : <Tag>否</Tag>}</td>
            <td style={tdStyle}>{p.desc}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

const thStyle: React.CSSProperties = {
  border: '1px solid #f0f0f0',
  padding: '8px 12px',
  textAlign: 'left',
  fontSize: 13
}

const tdStyle: React.CSSProperties = {
  border: '1px solid #f0f0f0',
  padding: '8px 12px',
  fontSize: 13
}

export default EmbedDemo
