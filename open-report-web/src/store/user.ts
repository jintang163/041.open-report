import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { User, MenuItem } from '@/types'
import { storage } from '@/utils/storage'

interface UserState {
  token: string | null
  userInfo: User | null
  permissions: string[]
  menus: MenuItem[]
  setToken: (token: string) => void
  setUserInfo: (user: User) => void
  setPermissions: (perms: string[]) => void
  setMenus: (menus: MenuItem[]) => void
  hasPermission: (perm: string) => boolean
  logout: () => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set, get) => ({
      token: storage.getToken(),
      userInfo: storage.getUserInfo<User>(),
      permissions: [],
      menus: [],

      setToken: (token: string) => {
        storage.setToken(token)
        set({ token })
      },

      setUserInfo: (user: User) => {
        storage.setUserInfo(user)
        set({ userInfo: user })
      },

      setPermissions: (perms: string[]) => {
        set({ permissions: perms })
      },

      setMenus: (menus: MenuItem[]) => {
        set({ menus })
      },

      hasPermission: (perm: string) => {
        const { permissions } = get()
        if (!permissions || permissions.length === 0) return false
        if (permissions.includes('*')) return true
        return permissions.includes(perm)
      },

      logout: () => {
        storage.clear()
        set({ token: null, userInfo: null, permissions: [], menus: [] })
      }
    }),
    {
      name: 'user-storage',
      partialize: (state) => ({
        token: state.token,
        userInfo: state.userInfo,
        permissions: state.permissions,
        menus: state.menus
      })
    }
  )
)
