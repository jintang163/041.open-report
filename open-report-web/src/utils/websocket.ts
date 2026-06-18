import { storage } from './storage'

export interface WebSocketMessage {
  type: string
  topic?: string
  payload?: any
  timestamp?: number
  messageId?: string
}

export interface WebSocketClientOptions {
  url?: string
  onConnect?: () => void
  onDisconnect?: () => void
  onError?: (error: Event) => void
  onMessage?: (message: WebSocketMessage) => void
  heartbeatInterval?: number
  reconnectInterval?: number
}

export class WebSocketClient {
  private socket: WebSocket | null = null
  private url: string
  private options: WebSocketClientOptions
  private heartbeatTimer: number | null = null
  private reconnectTimer: number | null = null
  private isManualClose = false
  private listeners: Map<string, Set<(message: WebSocketMessage) => void>> = new Map()
  private topics: Set<string> = new Set()

  constructor(options: WebSocketClientOptions = {}) {
    const apiBase = (import.meta.env.VITE_APP_API_BASE_URL as string) || ''
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    let baseHost = ''
    if (apiBase && apiBase.startsWith('http')) {
      try {
        const url = new URL(apiBase)
        baseHost = url.host
      } catch {
        baseHost = window.location.host
      }
    } else {
      baseHost = window.location.host
    }
    this.url = options.url || `${wsProtocol}//${baseHost}${apiBase.startsWith('/') ? apiBase : ''}/ws/report`
    this.options = {
      heartbeatInterval: 30000,
      reconnectInterval: 3000,
      ...options
    }
  }

  connect() {
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return
    }
    this.isManualClose = false

    let urlWithToken = this.url
    const token = storage.getToken()
    if (token) {
      const separator = this.url.includes('?') ? '&' : '?'
      urlWithToken = `${this.url}${separator}token=${encodeURIComponent(token)}`
    }

    try {
      this.socket = new WebSocket(urlWithToken)
    } catch (error) {
      console.error('[WebSocket] 创建连接失败:', error)
      this.scheduleReconnect()
      return
    }

    this.socket.onopen = () => {
      console.log('[WebSocket] 连接已建立')
      this.clearReconnect()
      this.startHeartbeat()
      this.topics.forEach((topic) => this.sendSubscribe(topic))
      this.options.onConnect?.()
    }

    this.socket.onclose = (event) => {
      console.log('[WebSocket] 连接已关闭:', event.code, event.reason)
      this.stopHeartbeat()
      this.options.onDisconnect?.()
      if (!this.isManualClose) {
        this.scheduleReconnect()
      }
    }

    this.socket.onerror = (error) => {
      console.error('[WebSocket] 连接错误:', error)
      this.options.onError?.(error)
    }

    this.socket.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data)
        this.options.onMessage?.(message)
        this.dispatch(message)
      } catch (error) {
        console.error('[WebSocket] 消息解析失败:', error)
      }
    }
  }

  disconnect() {
    this.isManualClose = true
    this.clearReconnect()
    this.stopHeartbeat()
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
  }

  send(message: WebSocketMessage) {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message))
    }
  }

  sendSubscribe(topic: string) {
    this.topics.add(topic)
    this.send({ type: 'SUBSCRIBE', topic })
  }

  sendUnsubscribe(topic: string) {
    this.topics.delete(topic)
    this.send({ type: 'UNSUBSCRIBE', topic })
  }

  subscribe(topic: string, handler: (message: WebSocketMessage) => void) {
    if (!this.listeners.has(topic)) {
      this.listeners.set(topic, new Set())
    }
    this.listeners.get(topic)!.add(handler)
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.sendSubscribe(topic)
    }
    return () => this.unsubscribe(topic, handler)
  }

  unsubscribe(topic: string, handler: (message: WebSocketMessage) => void) {
    this.listeners.get(topic)?.delete(handler)
  }

  private dispatch(message: WebSocketMessage) {
    const topic = message.topic || 'ALL'
    this.listeners.get(topic)?.forEach((handler) => {
      try {
        handler(message)
      } catch (error) {
        console.error('[WebSocket] 消息处理异常:', error)
      }
    })
    this.listeners.get('ALL')?.forEach((handler) => {
      try {
        handler(message)
      } catch (error) {
        console.error('[WebSocket] 消息处理异常:', error)
      }
    })
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    const interval = this.options.heartbeatInterval || 30000
    this.heartbeatTimer = window.setInterval(() => {
      if (this.socket?.readyState === WebSocket.OPEN) {
        this.send({ type: 'HEARTBEAT' })
      }
    }, interval)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer !== null) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  private scheduleReconnect() {
    this.clearReconnect()
    const interval = this.options.reconnectInterval || 3000
    this.reconnectTimer = window.setTimeout(() => {
      console.log('[WebSocket] 尝试重连...')
      this.connect()
    }, interval)
  }

  private clearReconnect() {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  get isConnected() {
    return this.socket?.readyState === WebSocket.OPEN
  }
}

export const globalWebSocket = new WebSocketClient()
