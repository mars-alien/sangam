import { useQuery } from '@tanstack/react-query'
import { Crown } from 'lucide-react'
import UserAvatar from '../../users/components/UserAvatar'
import LoadingSpinner from '../../../components/LoadingSpinner'
import { groupsApi } from '../api'

export default function MembersList({ eventId }) {
  const { data, isLoading } = useQuery({
    queryKey: ['members', eventId],
    queryFn: () => groupsApi.getEventMembers(eventId),
  })

  if (isLoading) return <LoadingSpinner />

  const members = data?.data?.content ?? []

  if (members.length === 0) {
    return <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>No members yet.</p>
  }

  return (
    <div className="members-list">
      {members.map((m) => (
        <div key={m.id} className="member-row">
          <UserAvatar user={m.user} />
          <div style={{ flex: 1 }}>
            <div className="member-name">{m.user?.username}</div>
            <div className="member-role">{m.role}</div>
          </div>
          {m.role === 'CREATOR' && <Crown size={14} color="var(--warning)" />}
        </div>
      ))}
    </div>
  )
}
