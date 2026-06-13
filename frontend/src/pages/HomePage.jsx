import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Search, MapPin, SlidersHorizontal } from 'lucide-react'
import { eventsApi } from '../features/events/api'
import EventCard from '../features/events/components/EventCard'
import NearbyMap from '../features/events/components/NearbyMap'
import LoadingSpinner from '../components/LoadingSpinner'
import EmptyState from '../components/EmptyState'
import useGeolocation from '../hooks/useGeolocation'
import useDebounce from '../hooks/useDebounce'

const CATEGORIES = [
  { value: '',        label: 'All categories' },
  { value: 'SPORTS',  label: 'Sports' },
  { value: 'MUSIC',   label: 'Music' },
  { value: 'ARTS',    label: 'Arts' },
  { value: 'FOOD',    label: 'Food' },
  { value: 'TECH',    label: 'Tech' },
  { value: 'OUTDOOR', label: 'Outdoor' },
  { value: 'SOCIAL',  label: 'Social' },
  { value: 'OTHER',   label: 'Other' },
]
const RADII = [5, 10, 25, 50, 100, 250, 500, 1000, 2000, 4000]

export default function HomePage() {
  const { location, loading: geoLoading, permissionDenied } = useGeolocation()
  const [radiusKm, setRadiusKm] = useState(10)
  const [category, setCategory] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const debouncedSearch = useDebounce(search, 400)

  const isSearching = debouncedSearch.trim().length > 0

  const nearbyQuery = useQuery({
    queryKey: ['events', 'nearby', location, radiusKm, category, page],
    queryFn: () =>
      eventsApi.getNearbyEvents({ lat: location.lat, lng: location.lng, radiusKm, category, page }),
    enabled: !!location && !isSearching,
  })

  const searchQuery = useQuery({
    queryKey: ['events', 'search', debouncedSearch, page],
    queryFn: () => eventsApi.searchEvents({ q: debouncedSearch, page }),
    enabled: isSearching,
  })

  const activeQuery = isSearching ? searchQuery : nearbyQuery
  const pageData = activeQuery.data?.data
  const events = pageData?.content ?? []
  const totalPages = pageData?.totalPages ?? 0

  function handleSearchChange(e) { setSearch(e.target.value); setPage(0) }
  function handleRadiusChange(e) { setRadiusKm(Number(e.target.value)); setPage(0) }
  function handleCategoryChange(e) { setCategory(e.target.value); setPage(0) }

  return (
    <div className="home-root">
      {/* Full-bleed map */}
      <div className="home-map-wrap">
        {geoLoading ? (
          <div className="home-map-loading">
            <LoadingSpinner />
            <span>Getting your location…</span>
          </div>
        ) : (
          <NearbyMap location={location} events={events} />
        )}
      </div>

      {/* Events feed */}
      <div className="page">
        {permissionDenied && (
          <div className="location-banner">
            <MapPin size={14} />
            Location access denied — showing events near Bengaluru.
          </div>
        )}

        {/* Controls */}
        <div className="home-controls">
          <div className="search-input-wrap">
            <Search size={15} />
            <input
              type="search"
              placeholder="Search events…"
              value={search}
              onChange={handleSearchChange}
            />
          </div>

          {!isSearching && (
            <div className="home-filters">
              <div className="select-wrap">
                <SlidersHorizontal size={14} />
                <select value={radiusKm} onChange={handleRadiusChange}>
                  {RADII.map((r) => (
                    <option key={r} value={r}>{r >= 4000 ? 'All India (~4000 km)' : `${r} km`}</option>
                  ))}
                </select>
              </div>
              <div className="select-wrap">
                <select value={category} onChange={handleCategoryChange}>
                  {CATEGORIES.map((c) => (
                    <option key={c.value} value={c.value}>{c.label}</option>
                  ))}
                </select>
              </div>
            </div>
          )}
        </div>

        {/* Results */}
        {activeQuery.isLoading ? (
          <div className="spinner-center"><LoadingSpinner /></div>
        ) : events.length === 0 ? (
          <EmptyState
            title="No events found"
            description={
              isSearching
                ? `No results for "${debouncedSearch}"`
                : 'No events nearby. Try a larger radius or different category.'
            }
          />
        ) : (
          <>
            <p className="results-count">
              {pageData?.totalElements ?? events.length} event{(pageData?.totalElements ?? events.length) !== 1 ? 's' : ''} found
            </p>
            <div className="events-grid">
              {events.map((event) => (
                <EventCard key={event.id} event={event} />
              ))}
            </div>
            {totalPages > 1 && (
              <div className="pagination">
                <button className="page-btn" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                  ← Prev
                </button>
                <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                  {page + 1} / {totalPages}
                </span>
                <button className="page-btn" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                  Next →
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
