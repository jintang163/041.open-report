import { Navigate, Outlet } from 'react-router-dom'
import BasicLayout from '@/layouts/BasicLayout'
import Login from '@/pages/login'
import Dashboard from '@/pages/dashboard'
import Designer from '@/pages/designer'
import DashboardList from '@/pages/dashboard-list'
import DashboardDesigner from '@/pages/dashboard-designer'
import DashboardViewer from '@/pages/dashboard-viewer'
import AiReportGenerator from '@/pages/ai-report'
import PivotDesigner from '@/pages/pivot-designer'
import UserManagement from '@/pages/system/user'
import RoleManagement from '@/pages/system/role'
import MenuManagement from '@/pages/system/menu'
import RowSecurityManagement from '@/pages/system/row-security'
import FieldPermissionManagement from '@/pages/system/field-permission'
import DatasourceManagement from '@/pages/datasource'
import DatasetManagement from '@/pages/dataset'
import ReportManagement from '@/pages/report'
import ApprovalManagement from '@/pages/approval'
import ScheduleManagement from '@/pages/schedule'
import SubscriptionManagement from '@/pages/subscription'
import EmbedDemo from '@/pages/embed/demo'
import PrintPage from '@/pages/print'
import PreviewPage from '@/pages/preview'
import FunctionManagement from '@/pages/function'
import H5ReportPage from '@/pages/h5-report'
import { useUserStore } from '@/store/user'
import { ComponentType } from 'react'

const RequireAuth = () => {
  const token = useUserStore((state) => state.token)
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}

const withLoading = (Component: ComponentType) => {
  return () => <Component />
}

const routes = [
  {
    path: '/login',
    element: <Login />,
    meta: { title: '登录' }
  },
  {
    path: '/print',
    element: <PrintPage />,
    meta: { title: '打印' }
  },
  {
    path: '/embed/viewer',
    element: <div style={{ padding: 0, minHeight: '100vh' }}>嵌入式报表查看</div>,
    meta: { title: '报表查看' }
  },
  {
    path: '/h5/report/:id',
    element: <H5ReportPage />,
    meta: { title: '移动端报表' }
  },
  {
    path: '/',
    element: <RequireAuth />,
    children: [
      {
        element: <BasicLayout />,
        children: [
          {
            index: true,
            element: <Navigate to="/dashboard" replace />
          },
          {
            path: 'dashboard',
            element: withLoading(Dashboard)(),
            meta: { title: '仪表盘', icon: 'DashboardOutlined' }
          },
          {
            path: 'datasource',
            element: withLoading(DatasourceManagement)(),
            meta: { title: '数据源管理', icon: 'DatabaseOutlined' }
          },
          {
            path: 'dataset',
            element: withLoading(DatasetManagement)(),
            meta: { title: '数据集管理', icon: 'TableOutlined' }
          },
          {
            path: 'ai-report',
            element: withLoading(AiReportGenerator)(),
            meta: { title: 'AI智能报表', icon: 'ThunderboltOutlined' }
          },
          {
            path: 'pivot-designer',
            element: withLoading(PivotDesigner)(),
            meta: { title: '交叉报表', icon: 'FundScreenOutlined' }
          },
          {
            path: 'report',
            element: withLoading(ReportManagement)(),
            meta: { title: '报表管理', icon: 'FileTextOutlined' }
          },
          {
            path: 'function',
            element: withLoading(FunctionManagement)(),
            meta: { title: '函数仓库', icon: 'FunctionOutlined' }
          },
          {
            path: 'approval',
            element: withLoading(ApprovalManagement)(),
            meta: { title: '审批管理', icon: 'AuditOutlined' }
          },
          {
            path: 'designer/:id?',
            element: <Designer />,
            meta: { title: '报表设计器', icon: 'EditOutlined', hideInMenu: true }
          },
          {
            path: 'preview/:id',
            element: <PreviewPage />,
            meta: { title: '报表预览', icon: 'EyeOutlined', hideInMenu: true }
          },
          {
            path: 'report/viewer/:id',
            element: <div style={{ padding: 24 }}>报表查看</div>,
            meta: { title: '报表查看', icon: 'EyeOutlined', hideInMenu: true }
          },
          {
            path: 'schedule',
            element: withLoading(ScheduleManagement)(),
            meta: { title: '调度管理', icon: 'ScheduleOutlined' }
          },
          {
            path: 'subscription',
            element: withLoading(SubscriptionManagement)(),
            meta: { title: '订阅通知', icon: 'BellOutlined' }
          },
          {
            path: 'screen',
            element: withLoading(DashboardList)(),
            meta: { title: '可视化大屏', icon: 'DashboardOutlined' }
          },
          {
            path: 'dashboard/designer/:id?',
            element: <DashboardDesigner />,
            meta: { title: '大屏设计器', icon: 'EditOutlined', hideInMenu: true }
          },
          {
            path: 'dashboard/viewer/:id',
            element: <DashboardViewer />,
            meta: { title: '大屏预览', icon: 'EyeOutlined', hideInMenu: true }
          },
          {
            path: 'system/user',
            element: withLoading(UserManagement)(),
            meta: { title: '用户管理', icon: 'UserOutlined', parent: '系统管理' }
          },
          {
            path: 'system/role',
            element: withLoading(RoleManagement)(),
            meta: { title: '角色管理', icon: 'TeamOutlined', parent: '系统管理' }
          },
          {
            path: 'system/menu',
            element: withLoading(MenuManagement)(),
            meta: { title: '菜单管理', icon: 'MenuOutlined', parent: '系统管理' }
          },
          {
            path: 'system/row-security',
            element: withLoading(RowSecurityManagement)(),
            meta: { title: '行级安全', icon: 'SafetyCertificateOutlined', parent: '系统管理' }
          },
          {
            path: 'system/field-permission',
            element: withLoading(FieldPermissionManagement)(),
            meta: { title: '字段权限', icon: 'LockOutlined', parent: '系统管理' }
          },
          {
            path: 'embed/demo',
            element: withLoading(EmbedDemo)(),
            meta: { title: '嵌入式集成', icon: 'LinkOutlined' }
          }
        ]
      }
    ]
  },
  {
    path: '*',
    element: <div style={{ padding: 50, textAlign: 'center' }}>404 Not Found</div>
  }
]

export default routes
