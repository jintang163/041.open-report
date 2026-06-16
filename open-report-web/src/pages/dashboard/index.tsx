import { useState, useEffect } from 'react'
import { Row, Col, Card, Statistic, Table, Tag, Space } from 'antd'
import {
  DatabaseOutlined,
  TableOutlined,
  FileTextOutlined,
  UserOutlined,
  ArrowUpOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'

interface DashboardStat {
  title: string
  value: number
  icon: any
  color: string
  prefix?: string
  suffix?: string
}

const Dashboard = () => {
  const [loading, setLoading] = useState(false)

  const stats: DashboardStat[] = [
    { title: '数据源数量', value: 12, icon: <DatabaseOutlined />, color: '#3f8600' },
    { title: '数据集数量', value: 48, icon: <TableOutlined />, color: '#1890ff' },
    { title: '报表数量', value: 86, icon: <FileTextOutlined />, color: '#722ed1' },
    { title: '用户数量', value: 35, icon: <UserOutlined />, color: '#cf1322' }
  ]

  const recentReports = [
    {
      key: '1',
      name: '销售月报',
      type: '表格',
      createBy: '张三',
      createTime: '2024-01-15 10:30:00',
      status: 1
    },
    {
      key: '2',
      name: '财务分析报表',
      type: '图表',
      createBy: '李四',
      createTime: '2024-01-14 15:20:00',
      status: 1
    },
    {
      key: '3',
      name: '库存统计',
      type: '混合',
      createBy: '王五',
      createTime: '2024-01-13 09:15:00',
      status: 0
    },
    {
      key: '4',
      name: '客户分析',
      type: '图表',
      createBy: '赵六',
      createTime: '2024-01-12 14:45:00',
      status: 1
    },
    {
      key: '5',
      name: '员工绩效',
      type: '表格',
      createBy: '孙七',
      createTime: '2024-01-11 16:00:00',
      status: 1
    }
  ]

  const columns = [
    {
      title: '报表名称',
      dataIndex: 'name',
      key: 'name'
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      render: (text: string) => (
        <Tag color="blue">{text}</Tag>
      )
    },
    {
      title: '创建人',
      dataIndex: 'createBy',
      key: 'createBy'
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={status === 1 ? 'green' : 'red'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      )
    }
  ]

  const lineChartOption = {
    title: {
      text: '报表访问趋势',
      left: 'center',
      textStyle: { fontSize: 14 }
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['访问次数', '执行次数'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '访问次数',
        type: 'line',
        smooth: true,
        data: [820, 932, 901, 934, 1290, 1330, 1320],
        areaStyle: { opacity: 0.3 }
      },
      {
        name: '执行次数',
        type: 'line',
        smooth: true,
        data: [120, 132, 101, 134, 90, 230, 210],
        areaStyle: { opacity: 0.3 }
      }
    ]
  }

  const pieChartOption = {
    title: {
      text: '数据源类型分布',
      left: 'center',
      textStyle: { fontSize: 14 }
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'horizontal',
      bottom: 0
    },
    series: [
      {
        name: '数据源',
        type: 'pie',
        radius: ['40%', '65%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: 4, name: 'MySQL' },
          { value: 3, name: 'Oracle' },
          { value: 2, name: 'PostgreSQL' },
          { value: 2, name: 'SQL Server' },
          { value: 1, name: 'DM' }
        ]
      }
    ]
  }

  useEffect(() => {
    setLoading(true)
    const timer = setTimeout(() => setLoading(false), 300)
    return () => clearTimeout(timer)
  }, [])

  return (
    <Space direction="vertical" size="large" style={{ width: '100%' }}>
      <Row gutter={[16, 16]}>
        {stats.map((stat, index) => (
          <Col xs={24} sm={12} lg={6} key={index}>
            <Card loading={loading}>
              <Statistic
                title={stat.title}
                value={stat.value}
                valueStyle={{ color: stat.color }}
                prefix={stat.icon}
                suffix={<ArrowUpOutlined style={{ fontSize: 12, color: '#3f8600' }} />}
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={16}>
          <Card loading={loading}>
            <ReactECharts option={lineChartOption} style={{ height: 350 }} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card loading={loading}>
            <ReactECharts option={pieChartOption} style={{ height: 350 }} />
          </Card>
        </Col>
      </Row>

      <Card title="最近报表" loading={loading}>
        <Table
          columns={columns}
          dataSource={recentReports}
          pagination={false}
          size="middle"
        />
      </Card>
    </Space>
  )
}

export default Dashboard
