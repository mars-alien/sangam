import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import Button from '../../../components/Button'
import WaitlistBadge from './WaitlistBadge'
import { groupsApi } from '../api'

function JoinModal({ eventId, onClose, onSuccess }) {
  const [message, setMessage] = useState('')
  const mutation = useMutation({
    mutationFn: () => groupsApi.sendJoinRequest(eventId, { message }),
    onSuccess: (data) => onSuccess(data),
  })

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <div className="modal-title">Request to Join</div>
        <div className="input-wrapper">
          <label className="input-label">Intro message (optional)</label>
          <textarea
            className="input-field"
            rows={3}
            placeholder="Tell the organiser a bit about yourself…"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
          />
        </div>
        {mutation.isError && (
          <p className="api-error" style={{ marginTop: 10 }}>
            {mutation.error?.response?.data?.message || 'Something went wrong'}
          </p>
        )}
        <div className="modal-actions">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button variant="primary" loading={mutation.isPending} onClick={() => mutation.mutate()}>
            Send Request
          </Button>
        </div>
      </div>
    </div>
  )
}

export default function JoinRequestButton({ event, currentUserStatus, joinRequestId, waitlistPosition, onUpdate }) {
  const [showModal, setShowModal] = useState(false)
  const queryClient = useQueryClient()

  const cancelMutation = useMutation({
    mutationFn: () => groupsApi.cancelJoinRequest(event.id, joinRequestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['event', event.id] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
      onUpdate?.()
    },
  })

  const leaveMutation = useMutation({
    mutationFn: () => groupsApi.leaveEvent(event.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['event', event.id] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
      onUpdate?.()
    },
  })

  const status = currentUserStatus

  if (event.status === 'CANCELLED' || event.status === 'COMPLETED') return null

  if (status === 'APPROVED') {
    return (
      <Button
        variant="outline-danger"
        full
        loading={leaveMutation.isPending}
        onClick={() => { if (confirm('Leave this event?')) leaveMutation.mutate() }}
      >
        Leave Event
      </Button>
    )
  }

  if (status === 'PENDING') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <Button variant="ghost" full disabled>Request Pending</Button>
        <button
          className="btn btn-ghost btn-sm"
          style={{ color: 'var(--danger)', justifyContent: 'center' }}
          disabled={cancelMutation.isPending}
          onClick={() => cancelMutation.mutate()}
        >
          Cancel Request
        </button>
      </div>
    )
  }

  if (status === 'WAITLISTED') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, alignItems: 'center' }}>
        <WaitlistBadge position={waitlistPosition} />
        <button
          className="btn btn-ghost btn-sm"
          style={{ color: 'var(--danger)' }}
          disabled={cancelMutation.isPending}
          onClick={() => cancelMutation.mutate()}
        >
          Leave Waitlist
        </button>
      </div>
    )
  }

  if (status === 'REJECTED') {
    return <Button variant="ghost" full disabled>Request Declined</Button>
  }

  return (
    <>
      <Button variant="primary" full onClick={() => setShowModal(true)}>
        Request to Join
      </Button>
      {showModal && (
        <JoinModal
          eventId={event.id}
          onClose={() => setShowModal(false)}
          onSuccess={() => {
            setShowModal(false)
            queryClient.invalidateQueries({ queryKey: ['event', event.id] })
            queryClient.invalidateQueries({ queryKey: ['events'] })
            onUpdate?.()
          }}
        />
      )}
    </>
  )
}
