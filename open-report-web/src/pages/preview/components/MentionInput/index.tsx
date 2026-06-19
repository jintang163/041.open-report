import React, { useState, useRef, useEffect, useCallback } from 'react'
import { Input, List, Avatar, Popover } from 'antd'
import { UserOutlined } from '@ant-design/icons'
import type { User } from '@/types'
import { getUserList } from '@/api/user'

interface MentionInputProps {
  value: string
  onChange: (value: string, mentionUserIds: string) => void
  placeholder?: string
  onPressEnter?: () => void
  rows?: number
}

interface MentionPosition {
  start: number
  end: number
  query: string
}

const MentionInput: React.FC<MentionInputProps> = ({
  value,
  onChange,
  placeholder,
  onPressEnter,
  rows = 1
}) => {
  const [mentionPos, setMentionPos] = useState<MentionPosition | null>(null)
  const [users, setUsers] = useState<User[]>([])
  const [mentionUsers, setMentionUsers] = useState<Map<number, User>>(new Map())
  const [popoverVisible, setPopoverVisible] = useState(false)
  const inputRef = useRef<any>(null)

  const searchUsers = useCallback(async (query: string) => {
    if (!query) {
      try {
        const result = await getUserList({ pageNum: 1, pageSize: 20 })
        setUsers(result.list || [])
      } catch {
        setUsers([])
      }
      return
    }
    try {
      const result = await getUserList({ pageNum: 1, pageSize: 20, keyword: query })
      setUsers(result.list || [])
    } catch {
      setUsers([])
    }
  }, [])

  useEffect(() => {
    if (mentionPos) {
      searchUsers(mentionPos.query)
      setPopoverVisible(true)
    } else {
      setPopoverVisible(false)
    }
  }, [mentionPos, searchUsers])

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const newValue = e.target.value
      const cursorPos = e.target.selectionStart

      const lastAtIndex = newValue.lastIndexOf('@', cursorPos - 1)
      if (lastAtIndex !== -1) {
        const textAfterAt = newValue.substring(lastAtIndex + 1, cursorPos)
        if (!textAfterAt.includes(' ') && !textAfterAt.includes('\n')) {
          setMentionPos({
            start: lastAtIndex,
            end: cursorPos,
            query: textAfterAt
          })
        } else {
          setMentionPos(null)
        }
      } else {
        setMentionPos(null)
      }

      const mentionIds = Array.from(mentionUsers.keys()).join(',')
      onChange(newValue, mentionIds)
    },
    [onChange, mentionUsers]
  )

  const handleSelectUser = useCallback(
    (user: User) => {
      if (!mentionPos) return

      const mentionText = `@[${user.nickname || user.username}](${user.id})`
      const before = value.substring(0, mentionPos.start)
      const after = value.substring(mentionPos.end)
      const newValue = before + mentionText + ' ' + after

      const newMentionUsers = new Map(mentionUsers)
      newMentionUsers.set(user.id!, user)
      setMentionUsers(newMentionUsers)

      const mentionIds = Array.from(newMentionUsers.keys()).join(',')
      onChange(newValue, mentionIds)

      setMentionPos(null)
      setPopoverVisible(false)

      setTimeout(() => {
        if (inputRef.current) {
          inputRef.current.focus()
        }
      }, 0)
    },
    [mentionPos, value, onChange, mentionUsers]
  )

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        if (mentionPos && popoverVisible) {
          e.preventDefault()
          return
        }
        if (onPressEnter) {
          e.preventDefault()
          onPressEnter()
        }
      }
    },
    [mentionPos, popoverVisible, onPressEnter]
  )

  const userListContent = (
    <div style={{ width: 220, maxHeight: 240, overflow: 'auto' }}>
      <List
        size="small"
        dataSource={users}
        renderItem={(user) => (
          <List.Item
            style={{ padding: '4px 8px', cursor: 'pointer' }}
            onClick={() => handleSelectUser(user)}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = '#f5f5f5'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'transparent'
            }}
          >
            <List.Item.Meta
              avatar={<Avatar size={24} src={user.avatar} icon={!user.avatar && <UserOutlined />} />}
              title={
                <span style={{ fontSize: 13 }}>
                  {user.nickname || user.username}
                </span>
              }
              description={
                user.email ? (
                  <span style={{ fontSize: 11 }}>{user.email}</span>
                ) : undefined
              }
            />
          </List.Item>
        )}
        locale={{ emptyText: '未找到用户' }}
      />
    </div>
  )

  return (
    <Popover
      content={userListContent}
      open={popoverVisible}
      placement="topLeft"
      trigger={[]}
      overlayStyle={{ zIndex: 1050 }}
    >
      <Input.TextArea
        ref={inputRef}
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        autoSize={rows === 1 ? { minRows: 1, maxRows: 4 } : { minRows: rows, maxRows: 8 }}
        style={{ flex: 1 }}
      />
    </Popover>
  )
}

export default MentionInput
