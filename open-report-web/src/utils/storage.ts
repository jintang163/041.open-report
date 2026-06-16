const TOKEN_KEY = 'open_report_token'
const USER_INFO_KEY = 'open_report_user_info'

export const storage = {
  getToken: (): string | null => {
    return localStorage.getItem(TOKEN_KEY)
  },

  setToken: (token: string): void => {
    localStorage.setItem(TOKEN_KEY, token)
  },

  removeToken: (): void => {
    localStorage.removeItem(TOKEN_KEY)
  },

  getUserInfo: <T = any>(): T | null => {
    const data = localStorage.getItem(USER_INFO_KEY)
    if (data) {
      try {
        return JSON.parse(data) as T
      } catch {
        return null
      }
    }
    return null
  },

  setUserInfo: (userInfo: any): void => {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  },

  removeUserInfo: (): void => {
    localStorage.removeItem(USER_INFO_KEY)
  },

  clear: (): void => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_INFO_KEY)
  },

  get: (key: string): string | null => {
    return localStorage.getItem(key)
  },

  set: (key: string, value: string): void => {
    localStorage.setItem(key, value)
  },

  remove: (key: string): void => {
    localStorage.removeItem(key)
  }
}
