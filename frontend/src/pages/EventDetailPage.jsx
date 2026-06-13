import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Calendar, MapPin, Users, ArrowLeft, Edit, Trash2, Check, X } from 'lucide-react'
import { eventsApi } from '../features/events/api'
import { groupsApi } from '../features/groups/api'
import EventStatusBadge from '../components/EventStatusBadge'
import EventMap from '../features/events/components/EventMap'
import MembersList from '../features/groups/components/MembersList'
import JoinRequestButton from '../features/groups/components/JoinRequestButton'
import UserAvatar from '../features/users/components/UserAvatar'
import LoadingSpinner from '../components/LoadingSpinner'
import Button from '../components/Button'
import useAuthStore from '../store/authStore'
import { formatEventDate, formatRelativeDate } from '../utils/formatters'

function JoinRequestsPanel({ eventId }) {
  const queryClient = useQueryClient()

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['joinRequests', eventId],
    queryFn: () => groupsApi.getJoinRequests(eventId, { status: 'PENDING' }),
  })

  const processMutation = useMutation({
    mutationFn: ({ requestId, action }) => groupsApi.processJoinRequest(eventId, requestId, { action }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['event', eventId] })
      queryClient.invalidateQueries({ queryKey: ['members', eventId] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
      refetch()
    },
  })

  const requests = data?.data?.content ?? []

  if (isLoading) return <LoadingSpinner />
  if (requests.length === 0) return (
    <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>No pending requests.</p>
  )

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {requests.map((jr) => (
        <div key={jr.id} className="jr-item">
          <UserAvatar user={jr.requester} />
          <div className="jr-info">
            <div className="jr-name">{jr.requester?.username}</div>
            {jr.message && <div className="jr-msg">"{jr.message}"</div>}
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 2 }}>
              {formatRelativeDate(jr.createdAt)}
            </div>
          </div>
          <div className="jr-actions">
            <button
              className="btn btn-sm"
              style={{ background: 'var(--success)', color: 'white', padding: '5px 10px' }}
              disabled={processMutation.isPending}
              onClick={() => processMutation.mutate({ requestId: jr.id, action: 'APPROVE' })}
              title="Approve"
            >
              <Check size={14} />
            </button>
            <button
              className="btn btn-sm"
              style={{ background: 'var(--danger)', color: 'white', padding: '5px 10px' }}
              disabled={processMutation.isPending}
              onClick={() => processMutation.mutate({ requestId: jr.id, action: 'REJECT' })}
              title="Reject"
            >
              <X size={14} />
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}

export default function EventDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { isAuthenticated, user } = useAuthStore()

  const { data, isLoading, error } = useQuery({
    queryKey: ['event', id],
    queryFn: () => eventsApi.getEvent(id),
  })

  const deleteMutation = useMutation({
    mutationFn: () => eventsApi.deleteEvent(id),
    onSuccess: () => navigate('/my-events'),
  })

  if (isLoading) return <LoadingSpinner />
  if (error || !data?.data) {
    return (
      <div className="page">
        <div className="empty-state">
          <h3>Event not found</h3>
          <Link to="/" className="btn btn-outline btn-sm">Back to Home</Link>
        </div>
      </div>
    )
  }

  const event = data.data
  const isCreator = event.isCreator
  const currentUserStatus = event.currentUserStatus || 'NONE'
  const joinRequestId = event.joinRequestId

  // Try to extract lat/lng from the event. Backend stores as JTS geometry,
  // response may include them at the top level or nested.
  const lat = event.latitude ?? event.location?.y ?? null
  const lng = event.longitude ?? event.location?.x ?? null

  function handleDelete() {
    if (confirm('Delete this event? This cannot be undone.')) {
      deleteMutation.mutate()
    }
  }

  return (
    <div className="page">
      <button
        className="btn btn-ghost btn-sm"
        onClick={() => navigate(-1)}
        style={{ marginBottom: 16 }}
      >
        <ArrowLeft size={15} /> Back
      </button>

      <div className="detail-layout">
        {/* Left column */}
        <div>
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 12 }}>
            <EventStatusBadge status={event.status} />
            {event.tags?.map((t) => (
              <span key={t} className="badge badge-purple">{t}</span>
            ))}
          </div>

          <h1 className="detail-title">{event.title}</h1>

          <div className="detail-meta">
            <div className="detail-meta-row">
              <Calendar size={16} color="var(--primary)" />
              {formatEventDate(event.eventDate)}
            </div>
            <div className="detail-meta-row">
              <MapPin size={16} color="var(--primary)" />
              {event.venueName}{event.city ? `, ${event.city}` : ''}
              {event.address && ` — ${event.address}`}
            </div>
            <div className="detail-meta-row">
              <Users size={16} color="var(--primary)" />
              {event.currentMemberCount} / {event.maxCompanions} joined
            </div>
          </div>

          {/* Description */}
          <div className="detail-section">
            <div className="detail-section-title">About this event</div>
            <p className="detail-description">{event.description}</p>
          </div>

          {/* Map */}
          {lat && lng && (
            <div className="detail-section">
              <div className="detail-section-title">Location</div>
              <EventMap lat={lat} lng={lng} />
            </div>
          )}

          {/* Members */}
          <div className="detail-section">
            <div className="detail-section-title">
              Members ({event.currentMemberCount})
            </div>
            <MembersList eventId={id} />
          </div>

          {/* Join requests (creator only) */}
          {isCreator && (
            <div className="detail-section">
              <div className="detail-section-title">Pending Join Requests</div>
              <JoinRequestsPanel eventId={id} />
            </div>
          )}
        </div>

        {/* Right sidebar */}
        <div>
          <div className="sidebar-card">
            {/* Creator */}
            <div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Organized by
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <UserAvatar user={event.creator} />
                <div>
                  <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{event.creator?.username}</div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                    Created {formatRelativeDate(event.createdAt)}
                  </div>
                </div>
              </div>
            </div>

            {/* Spots progress */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: 6 }}>
                <span style={{ color: 'var(--text-secondary)' }}>Spots filled</span>
                <span style={{ fontWeight: 600 }}>{event.currentMemberCount} / {event.maxCompanions}</span>
              </div>
              <div className="progress-bar">
                <div
                  className={`progress-bar-fill ${event.status === 'FULL' ? 'full' : ''}`}
                  style={{ width: `${Math.min(100, (event.currentMemberCount / event.maxCompanions) * 100)}%` }}
                />
              </div>
            </div>

            {/* Action button */}
            {isAuthenticated && !isCreator && (
              <JoinRequestButton
                event={event}
                currentUserStatus={currentUserStatus}
                joinRequestId={joinRequestId}
                waitlistPosition={event.waitlistPosition}
                onUpdate={() => queryClient.invalidateQueries({ queryKey: ['event', id] })}
              />
            )}

            {!isAuthenticated && (
              <Link to="/login" className="btn btn-primary btn-full">
                Log in to join
              </Link>
            )}

            {/* Creator actions */}
            {isCreator && (
              <div style={{ display: 'flex', gap: 8 }}>
                <Link to={`/events/${id}/edit`} className="btn btn-outline btn-sm" style={{ flex: 1, justifyContent: 'center' }}>
                  <Edit size={14} /> Edit
                </Link>
                <Button
                  variant="outline-danger"
                  size="sm"
                  style={{ flex: 1 }}
                  loading={deleteMutation.isPending}
                  onClick={handleDelete}
                >
                  <Trash2 size={14} /> Delete
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
