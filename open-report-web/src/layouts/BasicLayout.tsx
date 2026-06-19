import { useState, useEffect, useMemo } from 'react'
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
  BellOutlined,
  LinkOutlined,
  FundScreenOutlined,
  ThunderboltOutlined,
  LockOutlined,
  EyeOutlined,
  FunctionOutlined
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useUserStore } from '@/store/user'
import type { MenuItem } from '@/types'

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
  BellOutlined: <BellOutlined />,
  LinkOutlined: <LinkOutlined />,
  FundScreenOutlined: <FundScreenOutlined />,
  ThunderboltOutlined: <ThunderboltOutlined />,
  SettingOutlined: <SettingOutlined />,
  LockOutlined: <LockOutlined />,
  EyeOutlined: <EyeOutlined />,
  FunctionOutlined: <FunctionOutlined />
}

const staticMenuConfig: Record<string, { icon: string; label: string }> = {
  '/dashboard': { icon: 'DashboardOutlined', label: '仪表盘' },
  '/datasource': { icon: 'DatabaseOutlined', label: '数据源管理' },
  '/dataset': { icon: 'TableOutlined', label: '数据集管理' },
  '/ai-report': { icon: 'ThunderboltOutlined', label: 'AI智能报表' },
  '/pivot-designer': { icon: 'FundScreenOutlined', label: '交叉报表' },
  '/report': { icon: 'FileTextOutlined', label: '报表管理' },
  '/function': { icon: 'FunctionOutlined', label: '函数仓库' },
  '/schedule': { icon: 'ScheduleOutlined', label: '调度管理' },
  '/subscription': { icon: 'BellOutlined', label: '订阅通知' },
  '/screen': { icon: 'FundScreenOutlined', label: '可视化大屏' },
  '/embed/demo': { icon: 'LinkOutlined', label: '嵌入式集成' },
  '/system/user': { icon: 'UserOutlined', label: '用户管理' },
  '/system/role': { icon: 'TeamOutlined', label: '角色管理' },
  '/system/menu': { icon: 'MenuOutlined', label: '菜单管理' },
  '/system/row-security': { icon: 'SafetyCertificateOutlined', label: '行级安全' },
  '/system/field-permission': { icon: 'LockOutlined', label: '字段权限' }
}

const systemSubMenus = ['/system/user', '/system/role', '/system/menu', '/system/row-security', '/system/field-permission']

function buildMenuItemsFromPermissions(menus: MenuItem[], permissions: string[]) {
  if (permissions.includes('*')) {
    return buildFullMenu()
  }

  const permittedPaths = new Set<string>()
  const addPermittedPaths = (menuList: MenuItem[]) => {
    for (const menu of menuList) {
      if (menu.path) {
        permittedPaths.add(menu.path)
      }
      if (menu.children) {
        addPermittedPaths(menu.children)
      }
    }
  }
  addPermittedPaths(menus)

  return buildMenuFromPaths(permittedPaths)
}

function buildMenuFromPaths(paths: Set<string>) {
  const items: any[] = []

  const topLevelPaths = ['/dashboard', '/datasource', '/dataset', '/ai-report', '/pivot-designer', '/report', '/function', '/schedule', '/subscription', '/screen', '/embed/demo']
  for (const path of topLevelPaths) {
    if (paths.has(path) && staticMenuConfig[path]) {
      const config = staticMenuConfig[path]
      items.push({
        key: path,
        icon: iconMap[config.icon],
        label: config.label
      })
    }
  }

  const systemChildren: any[] = []
  for (const path of systemSubMenus) {
    if (paths.has(path) && staticMenuConfig[path]) {
      const config = staticMenuConfig[path]
      systemChildren.push({
        key: path,
        icon: iconMap[config.icon],
        label: config.label
      })
    }
  }

  if (systemChildren.length > 0) {
    items.push({
      key: 'system',
      icon: <SettingOutlined />,
      label: '系统管理',
      children: systemChildren
    })
  }

  return items
}

function buildFullMenu() {
  return [
    { key: '/dashboard', icon: iconMap['DashboardOutlined'], label: '仪表盘' },
    { key: '/datasource', icon: iconMap['DatabaseOutlined'], label: '数据源管理' },
    { key: '/dataset', icon: iconMap['TableOutlined'], label: '数据集管理' },
    { key: '/ai-report', icon: iconMap['ThunderboltOutlined'], label: 'AI智能报表' },
    { key: '/pivot-designer', icon: iconMap['FundScreenOutlined'], label: '交叉报表' },
    { key: '/report', icon: iconMap['FileTextOutlined'], label: '报表管理' },
    { key: '/function', icon: iconMap['FunctionOutlined'], label: '函数仓库' },
    { key: '/schedule', icon: iconMap['ScheduleOutlined'], label: '调度管理' },
    { key: '/subscription', icon: iconMap['BellOutlined'], label: '订阅通知' },
    { key: '/screen', icon: iconMap['FundScreenOutlined'], label: '可视化大屏' },
    { key: '/embed/demo', icon: iconMap['LinkOutlined'], label: '嵌入式集成' },
    {
      key: 'system',
      icon: <SettingOutlined />,
      label: '系统管理',
      children: [
        { key: '/system/user', icon: iconMap['UserOutlined'], label: '用户管理' },
        { key: '/system/role', icon: iconMap['TeamOutlined'], label: '角色管理' },
        { key: '/system/menu', icon: iconMap['MenuOutlined'], label: '菜单管理' },
        { key: '/system/row-security', icon: iconMap['SafetyCertificateOutlined'], label: '行级安全' },
        { key: '/system/field-permission', icon: iconMap['LockOutlined'], label: '字段权限' }
      ]
    }
  ]
}

const BasicLayout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { userInfo, logout, permissions, menus } = useUserStore()
  const {
    token: { colorBgContainer, borderRadiusLG }
  } = theme.useToken()

  const [selectedKeys, setSelectedKeys] = useState<string[]>([])
  const [openKeys, setOpenKeys] = useState<string[]>([])

  const menuItems = useMemo(() => {
    return buildMenuItemsFromPermissions(menus || [], permissions || [])
  }, [menus, permissions])

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
      'ai-report': 'AI智能报表',
      'pivot-designer': '交叉报表',
      report: '报表管理',
      schedule: '调度管理',
      subscription: '订阅通知',
      screen: '可视化大屏',
      designer: '设计器',
      viewer: '预览',
      system: '系统管理',
      user: '用户管理',
      role: '角色管理',
      menu: '菜单管理',
      embed: '嵌入式集成',
      demo: '集成示例',
      'row-security': '行级安全',
      'field-permission': '字段权限'
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
