import { useState } from 'react'
import { Form, Input, Button, Checkbox, message, Card } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate, Navigate } from 'react-router-dom'
import { useUserStore } from '@/store/user'
import { login } from '@/api/user'
import { LoginParams } from '@/types'

const Login = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { token, setToken, setUserInfo, setPermissions, setMenus } = useUserStore()

  if (token) {
    return <Navigate to="/dashboard" replace />
  }

  const onFinish = async (values: LoginParams) => {
    setLoading(true)
    try {
      const result = await login(values)
      setToken(result.token)
      setUserInfo({
        id: result.userId,
        username: result.username,
        nickname: result.nickname,
        avatar: result.avatar,
        deptId: result.deptId,
        roles: result.roles,
        permissions: result.permissions,
        menus: result.menus
      })
      setPermissions(result.permissions || [])
      setMenus(result.menus || [])
      message.success('登录成功')
      navigate('/dashboard')
    } catch (error: any) {
      message.error(error.message || '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        width: '100%',
        height: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
      }}
    >
      <Card
        style={{
          width: 400,
          boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
        }}
        bordered={false}
      >
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <h1 style={{ margin: 0, fontSize: 24, fontWeight: 600 }}>Open Report</h1>
          <p style={{ color: '#8c8c8c', marginTop: 8 }}>报表平台管理系统</p>
        </div>
        <Form
          name="login"
          initialValues={{ remember: true, username: 'admin', password: 'admin123' }}
          onFinish={onFinish}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>

          <Form.Item name="remember" valuePropName="checked">
            <Checkbox>记住我</Checkbox>
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              style={{ width: '100%' }}
            >
              登 录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Login
