import React from 'react'
import { Badge, Tooltip } from 'antd'
import { CommentOutlined } from '@ant-design/icons'

interface CommentBubbleProps {
  count: number
  onClick: () => void
  active?: boolean
}

const CommentBubble: React.FC<CommentBubbleProps> = ({ count, onClick, active }) => {
  if (count === 0) return null

  return (
    <Tooltip title={`${count} 条评论`}>
      <div
        onClick={(e) => {
          e.stopPropagation()
          onClick()
        }}
        style={{
          position: 'absolute',
          top: -8,
          right: -8,
          cursor: 'pointer',
          zIndex: 10,
          transition: 'transform 0.2s'
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.transform = 'scale(1.2)'
        }}
        onMouseLeave={(e) => {
          e.currentTarget.style.transform = 'scale(1)'
        }}
      >
        <Badge count={count} size="small" color={active ? '#1677ff' : '#8c8c8c'}>
          <div
            style={{
              width: 24,
              height: 24,
              borderRadius: '50%',
              background: active ? '#1677ff' : '#f0f0f0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 6px rgba(0,0,0,0.15)'
            }}
          >
            <CommentOutlined
              style={{
                fontSize: 12,
                color: active ? '#fff' : '#595959'
              }}
            />
          </div>
        </Badge>
      </div>
    </Tooltip>
  )
}

export default CommentBubble
