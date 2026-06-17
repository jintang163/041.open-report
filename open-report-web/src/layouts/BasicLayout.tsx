import { useState, useEffect } from 'react'
import { Layout, Menu, Avatar, Dropdown, Breadcrumb, theme } from 'antd'
import {
  DashboardOutlined,
  DatabaseOutlined,
  TableOutlined,
  FileTextOutlined,
  UserOutlined,
  TeamOutlined,
  MenuOutlined,
  SettingOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  SafetyCertificateOutlined,
  ScheduleOutlined,
  LinkOutlined,
  FundScreenOutlined
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useUserStore } from '@/store/user'

const { Header, Sider, Content } = Layout

const iconMap: Record<string, any> = {
  DashboardOutlined: <DashboardOutlined />,
  DatabaseOutlined: <DatabaseOutlined />,
  TableOutlined: <TableOutlined />,
  FileTextOutlined: <FileTextOutlined />,
  UserOutlined: <UserOutlined />,
  TeamOutlined: <TeamOutlined />,
  MenuOutlined: <MenuOutlined />,
  SafetyCertificateOutlined: <SafetyCertificateOutlined />,
  ScheduleOutlined: <ScheduleOutlined />,
  LinkOutlined: <LinkOutlined />,
  FundScreenOutlined: <FundScreenOutlined />
}

const menuItems = [
  {
    key: '/dashboard',
    icon: iconMap['DashboardOutlined'],
    label: '仪表盘'
  },
  {
    key: '/datasource',
    icon: iconMap['DatabaseOutlined'],
    label: '数据源管理'
  },
  {
    key: '/dataset',
    icon: iconMap['TableOutlined'],
    label: '数据集管理'
  },
  {
    key: '/report',
    icon: iconMap['FileTextOutlined'],
    label: '报表管理'
  },
  {
    key: '/schedule',
    icon: iconMap['ScheduleOutlined'],
    label: '调度管理'
  },
  {
    key: '/screen',
    icon: iconMap['FundScreenOutlined'],
    label: '可视化大屏'
  },
  {
    key: '/embed/demo',
    icon: iconMap['LinkOutlined'],
    label: '嵌入式集成'
  },
  {
    key: 'system',
    icon: <SettingOutlined />,
    label: '系统管理',
    children: [
      {
        key: '/system/user',
        icon: iconMap['UserOutlined'],
        label: '用户管理'
      },
      {
        key: '/system/role',
        icon: iconMap['TeamOutlined'],
        label: '角色管理'
      },
      {
        key: '/system/menu',
        icon: iconMap['MenuOutlined'],
        label: '菜单管理'
      }
    ]
  }
]

const BasicLayout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { userInfo, logout } = useUserStore()
  const {
    token: { colorBgContainer, borderRadiusLG }
  } = theme.useToken()

  const [selectedKeys, setSelectedKeys] = useState<string[]>([])
  const [openKeys, setOpenKeys] = useState<string[]>([])

  useEffect(() => {
    const pathname = location.pathname
    setSelectedKeys([pathname])
    if (pathname.startsWith('/system')) {
      setOpenKeys(['system'])
    }
  }, [location.pathname])

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心'
    },
    {
      type: 'divider' as const,
      key: 'divider'
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout
    }
  ]

  const breadcrumbItems = (() => {
    const pathSnippets = location.pathname.split('/').filter((i) => i)
    const breadcrumbMap: Record<string, string> = {
      dashboard: '仪表盘',
      datasource: '数据源管理',
      dataset: '数据集管理',
      report: '报表管理',
      schedule: '调度管理',
      screen: '可视化大屏',
      designer: '设计器',
      viewer: '预览',
      system: '系统管理',
      user: '用户管理',
      role: '角色管理',
      menu: '菜单管理',
      embed: '嵌入式集成',
      demo: '集成示例'
    }
    const items = [{ title: '首页' }]
    let url = ''
    for (const snippet of pathSnippets) {
      url += `/${snippet}`
      items.push({
        title: breadcrumbMap[snippet] || snippet
      })
    }
    return items
  })()

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        theme="dark"
        width={240}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: collapsed ? 14 : 18,
            fontWeight: 'bold',
            background: 'rgba(255, 255, 255, 0.04)'
          }}
        >
          {collapsed ? 'OR' : 'Open Report'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          openKeys={openKeys}
          onOpenChange={setOpenKeys}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 24px',
            background: colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,21,41,.08)'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <span
              onClick={() => setCollapsed(!collapsed)}
              style={{
                fontSize: 18,
                cursor: 'pointer',
                marginRight: 16
              }}
            >
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </span>
            <Breadcrumb items={breadcrumbItems} />
          </div>
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                cursor: 'pointer',
                gap: 8
              }}
            >
              <Avatar size="small" icon={<UserOutlined />} src={userInfo?.avatar} />
              <span>{userInfo?.nickname || userInfo?.username || '管理员'}</span>
            </div>
          </Dropdown>
        </Header>
        <Content
          style={{
            margin: '16px',
            padding: 24,
            minHeight: 280,
            background: colorBgContainer,
            borderRadius: borderRadiusLG
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default BasicLayout
