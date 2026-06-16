import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { User } from '@/types'
import { storage } from '@/utils/storage'

interface UserState {
  token: string | null
  userInfo: User | null
  setToken: (token: string) => void
  setUserInfo: (user: User) => void
  logout: () => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set) => ({
      token: storage.getToken(),
      userInfo: storage.getUserInfo<User>(),

      setToken: (token: string) => {
        storage.setToken(token)
        set({ token })
      },

      setUserInfo: (user: User) => {
        storage.setUserInfo(user)
        set({ userInfo: user })
      },

      logout: () => {
        storage.clear()
        set({ token: null, userInfo: null })
      }
    }),
    {
      name: 'user-storage',
      partialize: (state) => ({
        token: state.token,
        userInfo: state.userInfo
      })
    }
  )
)
