import { useCallback } from 'react'
import { useUserStore } from '@/store/user'

export function usePermission() {
  const permissions = useUserStore((state) => state.permissions)
  const hasPermission = useUserStore((state) => state.hasPermission)

  const hasAnyPermission = useCallback(
    (perms: string[]) => {
      if (!permissions || permissions.length === 0) return false
      if (permissions.includes('*')) return true
      return perms.some((p) => permissions.includes(p))
    },
    [permissions]
  )

  const hasAllPermissions = useCallback(
    (perms: string[]) => {
      if (!permissions || permissions.length === 0) return false
      if (permissions.includes('*')) return true
      return perms.every((p) => permissions.includes(p))
    },
    [permissions]
  )

  const isSuperAdmin = useCallback(() => {
    return permissions.includes('*')
  }, [permissions])

  return {
    permissions,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    isSuperAdmin
  }
}
