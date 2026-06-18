import React from 'react'
import { usePermission } from '@/hooks/usePermission'

interface PermissionAbleProps {
  perm: string | string[]
  mode?: 'any' | 'all'
  fallback?: React.ReactNode
  children: React.ReactNode
}

const PermissionAble: React.FC<PermissionAbleProps> = ({
  perm,
  mode = 'any',
  fallback = null,
  children
}) => {
  const { hasPermission, hasAnyPermission, hasAllPermissions } = usePermission()

  const perms = Array.isArray(perm) ? perm : [perm]

  const hasAccess =
    mode === 'all'
      ? hasAllPermissions(perms)
      : perms.length === 1
        ? hasPermission(perms[0])
        : hasAnyPermission(perms)

  return hasAccess ? <>{children}</> : <>{fallback}</>
}

export default PermissionAble
