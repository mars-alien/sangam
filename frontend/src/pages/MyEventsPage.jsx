import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { PlusCircle, Calendar } from 'lucide-react'
import { eventsApi } from '../features/events/api'
import EventCard from '../features/events/components/EventCard'
import LoadingSpinner from '../components/LoadingSpinner'
import EmptyState from '../components/EmptyState'
import useAuthStore from '../store/authStore'

export default function MyEventsPage() {
  const [tab, setTab] = useState('created')
  const [page, setPage] = useState(0)
  const { user } = useAuthStore()

  const { data, isLoading } = useQuery({
    queryKey: ['myEvents', page],
    queryFn: () => eventsApi.getMyEvents({ page }),
  })

  const pageData = data?.data
  const events = pageData?.content ?? []
  const totalPages = pageData?.totalPages ?? 0

  const createdEvents = events.filter((e) => e.creator?.id === user?.id)
  const joinedEvents  = events.filter((e) => e.creator?.id !== user?.id)
  const shown = tab === 'created' ? createdEvents : joinedEvents

  return (
    <div className="page">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 700 }}>My Events</h1>
        <Link to="/events/create" className="btn btn-primary btn-sm">
          <PlusCircle size={15} /> Create Event
        </Link>
      </div>

      <div className="tabs">
        <button
          className={`tab ${tab === 'created' ? 'active' : ''}`}
          onClick={() => { setTab('created'); setPage(0) }}
        >
          Events I Created ({createdEvents.length})
        </button>
        <button
          className={`tab ${tab === 'joined' ? 'active' : ''}`}
          onClick={() => { setTab('joined'); setPage(0) }}
        >
          Events I Joined ({joinedEvents.length})
        </button>
      </div>

      {isLoading ? (
        <LoadingSpinner />
      ) : shown.length === 0 ? (
        <EmptyState
          icon={Calendar}
          title={tab === 'created' ? "You haven't created any events yet" : "You haven't joined any events yet"}
          description={tab === 'created' ? 'Share an activity with people around you.' : 'Discover events near you and request to join.'}
          action={
            tab === 'created' ? (
              <Link to="/events/create" className="btn btn-primary btn-sm">
                <PlusCircle size={14} /> Create your first event
              </Link>
            ) : (
              <Link to="/" className="btn btn-outline btn-sm">Browse events</Link>
            )
          }
        />
      ) : (
        <>
          <div className="events-grid">
            {shown.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>
          {totalPages > 1 && (
            <div className="pagination">
              <button className="page-btn" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>← Prev</button>
              <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{page + 1} / {totalPages}</span>
              <button className="page-btn" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next →</button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
