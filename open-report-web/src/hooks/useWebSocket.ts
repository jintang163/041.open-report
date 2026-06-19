import { useEffect, useRef, useCallback, useState } from 'react'
import { globalWebSocket, WebSocketMessage, WebSocketClient } from '@/utils/websocket'

export function useWebSocket(
  topics?: string | string[],
  handler?: (message: WebSocketMessage) => void,
  client: WebSocketClient = globalWebSocket
) {
  const handlerRef = useRef(handler)
  handlerRef.current = handler

  const [isConnected, setIsConnected] = useState(client.isConnected)

  useEffect(() => {
    if (!client.isConnected) {
      client.connect()
    }

    const onConnect = () => setIsConnected(true)
    const onDisconnect = () => setIsConnected(false)

    const connectHandler = () => {
      client.subscribe('__connect__', onConnect)
    }

    const disconnectHandler = () => {
      client.subscribe('__disconnect__', onDisconnect)
    }

    setIsConnected(client.isConnected)

    const topicList = Array.isArray(topics) ? topics : topics ? [topics] : []
    const unsubscribers: (() => void)[] = []

    topicList.forEach((topic) => {
      const unsub = client.subscribe(topic, (msg) => {
        handlerRef.current?.(msg)
      })
      unsubscribers.push(unsub)
    })

    return () => {
      unsubscribers.forEach((u) => u())
    }
  }, [topics, client])

  const subscribe = useCallback(
    (topic: string, fn: (message: WebSocketMessage) => void) => {
      return client.subscribe(topic, fn)
    },
    [client]
  )

  return { isConnected, subscribe }
}

export function useReportWebSocket(
  templateId: number | string | undefined,
  onRefresh?: () => void
) {
  const [shouldRefresh, setShouldRefresh] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const [lastMessage, setLastMessage] = useState<WebSocketMessage | null>(null)

  const topics = templateId ? [`REPORT:${templateId}`, 'REPORT:LIST', `COMMENT:${templateId}`] : ['REPORT:LIST']

  const { isConnected } = useWebSocket(topics, (message) => {
    setLastMessage(message)
    if (
      message.type === 'REPORT_TEMPLATE_CHANGED' ||
      message.type === 'REPORT_DATA_CHANGED' ||
      message.type === 'REPORT_VERSION_CHANGED' ||
      message.type === 'REPORT_APPROVAL_CHANGED' ||
      message.type === 'REPORT_COMMENT_CHANGED' ||
      message.type === 'REPORT_COMMENT_MENTION'
    ) {
      setShouldRefresh(true)
      setRefreshKey((k) => k + 1)
      onRefresh?.()
    }
  })

  const acknowledgeRefresh = useCallback(() => setShouldRefresh(false), [])

  return { isConnected, shouldRefresh, acknowledgeRefresh, refreshKey, lastMessage }
}
