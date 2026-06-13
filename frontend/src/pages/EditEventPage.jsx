import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { useState } from 'react'
import { ArrowLeft } from 'lucide-react'
import EventForm from '../features/events/components/EventForm'
import { eventsApi } from '../features/events/api'
import LoadingSpinner from '../components/LoadingSpinner'

export default function EditEventPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [apiError, setApiError] = useState(null)

  const { data, isLoading, error } = useQuery({
    queryKey: ['event', id],
    queryFn: () => eventsApi.getEvent(id),
  })

  const mutation = useMutation({
    mutationFn: (formData) => eventsApi.updateEvent(id, formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['event', id] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
      navigate(`/events/${id}`)
    },
    onError: (err) => {
      setApiError(err.response?.data?.message || 'Failed to update event.')
    },
  })

  if (isLoading) return <div className="page"><LoadingSpinner /></div>

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

  const pad = (n) => String(n).padStart(2, '0')
  const toDate = (iso) => {
    if (!iso) return ''
    const d = new Date(iso)
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }
  const toTime = (iso) => {
    if (!iso) return ''
    const d = new Date(iso)
    return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  }

  const defaultValues = {
    title:         event.title,
    description:   event.description,
    category:      event.category,
    venueName:     event.venueName,
    address:       event.address || '',
    city:          event.city,
    eventDate:     toDate(event.eventDate),
    eventTime:     toTime(event.eventDate),
    minCompanions: event.minCompanions,
    maxCompanions: event.maxCompanions,
    tags:          event.tags?.join(', ') || '',
    latitude:      event.latitude,
    longitude:     event.longitude,
  }

  return (
    <div className="page">
      <div style={{ marginBottom: 24 }}>
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate(`/events/${id}`)}
          style={{ marginBottom: 12 }}
        >
          <ArrowLeft size={15} /> Back to event
        </button>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 700 }}>Edit Event</h1>
        <p style={{ color: 'var(--text-secondary)', marginTop: 4 }}>Update your event details.</p>
      </div>

      {apiError && (
        <div className="api-error" style={{ marginBottom: 20, maxWidth: 720, margin: '0 auto 20px' }}>
          {apiError}
        </div>
      )}

      <EventForm
        onSubmit={(formData) => { setApiError(null); mutation.mutate(formData) }}
        isLoading={mutation.isPending}
        defaultValues={defaultValues}
        submitLabel="Save Changes"
      />
    </div>
  )
}
