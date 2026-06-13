import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import EventForm from '../features/events/components/EventForm'
import { eventsApi } from '../features/events/api'

export default function CreateEventPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState(null)

  const mutation = useMutation({
    mutationFn: eventsApi.createEvent,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['events'] })
      navigate(`/events/${data.data.id}`)
    },
    onError: (err) => {
      setApiError(err.response?.data?.message || 'Failed to create event.')
    },
  })

  return (
    <div className="page">
      <div style={{ marginBottom: 24 }}>
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate(-1)}
          style={{ marginBottom: 12 }}
        >
          <ArrowLeft size={15} /> Back
        </button>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 700 }}>Create Event</h1>
        <p style={{ color: 'var(--text-secondary)', marginTop: 4 }}>
          Plan something great and invite people to join you.
        </p>
      </div>

      {apiError && (
        <div className="api-error" style={{ marginBottom: 20, maxWidth: 720, margin: '0 auto 20px' }}>
          {apiError}
        </div>
      )}

      <EventForm
        onSubmit={(data) => { setApiError(null); mutation.mutate(data) }}
        isLoading={mutation.isPending}
      />
    </div>
  )
}
