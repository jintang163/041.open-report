import React, { useState, useRef, useCallback } from 'react'
import { Input, List, Avatar, Button, Space, Popconfirm, Typography, Empty, Divider, Tag } from 'antd'
import {
  LikeOutlined,
  LikeFilled,
  DeleteOutlined,
  ReplyOutlined,
  UserOutlined
} from '@ant-design/icons'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import type { ReportComment } from '@/types'
import MentionInput from '../MentionInput'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { Text, Paragraph } = Typography

interface CommentItemProps {
  comment: ReportComment
  currentUserId?: number
  onReply: (comment: ReportComment) => void
  onLike: (commentId: number) => void
  onDelete: (commentId: number) => void
}

const CommentItem: React.FC<CommentItemProps> = ({
  comment,
  currentUserId,
  onReply,
  onLike,
  onDelete
}) => {
  const isOwner = comment.createBy === currentUserId

  const renderContent = (content: string) => {
    const parts = content.split(/(@\[[^\]]+\]\(\d+\))/g)
    return parts.map((part, index) => {
      const mentionMatch = part.match(/@\[([^\]]+)\]\((\d+)\)/)
      if (mentionMatch) {
        return (
          <Text key={index} style={{ color: '#1677ff', cursor: 'pointer' }}>
            @{mentionMatch[1]}
          </Text>
        )
      }
      return <span key={index}>{part}</span>
    })
  }

  return (
    <div style={{ padding: '8px 0' }}>
      <div style={{ display: 'flex', gap: 8 }}>
        <Avatar
          size={28}
          src={comment.createByAvatar}
          icon={!comment.createByAvatar && <UserOutlined />}
          style={{ flexShrink: 0 }}
        />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 2 }}>
            <Text strong style={{ fontSize: 13 }}>
              {comment.createByName || '未知用户'}
            </Text>
            {comment.replyToUserName && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                回复 <Text style={{ color: '#1677ff' }}>@{comment.replyToUserName}</Text>
              </Text>
            )}
            <Text type="secondary" style={{ fontSize: 11, marginLeft: 'auto' }}>
              {dayjs(comment.createTime).fromNow()}
            </Text>
          </div>
          <div style={{ fontSize: 13, lineHeight: 1.6, wordBreak: 'break-word' }}>
            {renderContent(comment.content)}
          </div>
          <Space size={12} style={{ marginTop: 4 }}>
            <Button
              type="text"
              size="small"
              icon={comment.liked ? <LikeFilled style={{ color: '#1677ff' }} /> : <LikeOutlined />}
              onClick={() => onLike(comment.id!)}
              style={{ padding: '0 4px', fontSize: 12 }}
            >
              {comment.likeCount || 0}
            </Button>
            <Button
              type="text"
              size="small"
              icon={<ReplyOutlined />}
              onClick={() => onReply(comment)}
              style={{ padding: '0 4px', fontSize: 12 }}
            >
              回复
            </Button>
            {isOwner && (
              <Popconfirm
                title="确定删除此评论？"
                onConfirm={() => onDelete(comment.id!)}
                okText="确定"
                cancelText="取消"
              >
                <Button
                  type="text"
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                  style={{ padding: '0 4px', fontSize: 12 }}
                />
              </Popconfirm>
            )}
          </Space>
        </div>
      </div>
    </div>
  )
}

interface CommentPanelProps {
  comments: ReportComment[]
  loading?: boolean
  currentUserId?: number
  title?: string
  selectedCellRef?: string | null
  selectedChartId?: string | null
  snapshotVersion?: number | null
  onAddComment: (content: string, mentionUserIds: string) => void
  onAddReply: (parentId: number, content: string, replyToUserId: number, replyToUserName: string, mentionUserIds: string) => void
  onLike: (commentId: number) => void
  onDelete: (commentId: number) => void
}

const CommentPanel: React.FC<CommentPanelProps> = ({
  comments,
  loading = false,
  currentUserId,
  title,
  selectedCellRef,
  selectedChartId,
  snapshotVersion,
  onAddComment,
  onAddReply,
  onLike,
  onDelete
}) => {
  const [replyTo, setReplyTo] = useState<ReportComment | null>(null)
  const [newComment, setNewComment] = useState('')
  const [mentionUserIds, setMentionUserIds] = useState<string>('')
  const [replyContent, setReplyContent] = useState('')
  const [replyMentionUserIds, setReplyMentionUserIds] = useState<string>('')

  const handleAddComment = useCallback(() => {
    if (!newComment.trim()) return
    onAddComment(newComment.trim(), mentionUserIds)
    setNewComment('')
    setMentionUserIds('')
  }, [newComment, mentionUserIds, onAddComment])

  const handleAddReply = useCallback(() => {
    if (!replyTo || !replyContent.trim()) return
    onAddReply(
      replyTo.id!,
      replyContent.trim(),
      replyTo.createBy!,
      replyTo.createByName || '',
      replyMentionUserIds
    )
    setReplyTo(null)
    setReplyContent('')
    setReplyMentionUserIds('')
  }, [replyTo, replyContent, replyMentionUserIds, onAddReply])

  const handleReply = useCallback((comment: ReportComment) => {
    setReplyTo(comment)
    setReplyContent('')
    setReplyMentionUserIds('')
  }, [])

  const handleCancelReply = useCallback(() => {
    setReplyTo(null)
    setReplyContent('')
    setReplyMentionUserIds('')
  }, [])

  const renderCommentWithReplies = (comment: ReportComment) => (
    <div key={comment.id}>
      <CommentItem
        comment={comment}
        currentUserId={currentUserId}
        onReply={handleReply}
        onLike={onLike}
        onDelete={onDelete}
      />
      {comment.replies && comment.replies.length > 0 && (
        <div style={{ marginLeft: 36, borderLeft: '2px solid #f0f0f0', paddingLeft: 12 }}>
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              currentUserId={currentUserId}
              onReply={handleReply}
              onLike={onLike}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </div>
  )

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {title && (
        <div style={{ padding: '12px 16px', borderBottom: '1px solid #f0f0f0' }}>
          <Text strong>{title}</Text>
        </div>
      )}

      <div style={{ flex: 1, overflow: 'auto', padding: '0 16px' }}>
        {comments.length === 0 && !loading ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="暂无评论"
            style={{ marginTop: 40 }}
          />
        ) : (
          <div style={{ padding: '8px 0' }}>
            {comments.map(renderCommentWithReplies)}
          </div>
        )}
      </div>

      <div style={{ borderTop: '1px solid #f0f0f0', padding: 12 }}>
        {(selectedCellRef || selectedChartId || snapshotVersion) && !replyTo && (
          <div
            style={{
              background: '#f6f8fa',
              padding: '6px 10px',
              borderRadius: 6,
              marginBottom: 8,
              fontSize: 12,
              color: '#666'
            }}
          >
            {selectedCellRef && <span>评论将绑定到 <Tag color="blue">单元格 {selectedCellRef}</Tag></span>}
            {selectedChartId && <span>评论将绑定到 <Tag color="blue">图表 {selectedChartId}</Tag></span>}
            {snapshotVersion && <span>（版本 v{snapshotVersion}）</span>}
          </div>
        )}
        {replyTo && (
          <div
            style={{
              background: '#f6f8fa',
              padding: '6px 10px',
              borderRadius: 6,
              marginBottom: 8,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}
          >
            <Text type="secondary" style={{ fontSize: 12 }}>
              回复 <Text strong>{replyTo.createByName}</Text>
            </Text>
            <Button type="text" size="small" onClick={handleCancelReply}>
              取消
            </Button>
          </div>
        )}

        {replyTo ? (
          <Space.Compact style={{ width: '100%' }}>
            <MentionInput
              value={replyContent}
              onChange={(val, mentions) => {
                setReplyContent(val)
                setReplyMentionUserIds(mentions)
              }}
              placeholder={`回复 ${replyTo.createByName}...`}
              onPressEnter={handleAddReply}
            />
            <Button type="primary" onClick={handleAddReply}>
              发送
            </Button>
          </Space.Compact>
        ) : (
          <Space.Compact style={{ width: '100%' }}>
            <MentionInput
              value={newComment}
              onChange={(val, mentions) => {
                setNewComment(val)
                setMentionUserIds(mentions)
              }}
              placeholder="添加评论，输入 @ 提及同事..."
              onPressEnter={handleAddComment}
            />
            <Button type="primary" onClick={handleAddComment}>
              发送
            </Button>
          </Space.Compact>
        )}
      </div>
    </div>
  )
}

export default CommentPanel
