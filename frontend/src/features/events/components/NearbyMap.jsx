import { useEffect, useRef, useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMap, CircleMarker, ZoomControl } from 'react-leaflet'
import { Link } from 'react-router-dom'
import L from 'leaflet'
import { Crosshair } from 'lucide-react'
import { formatEventDate } from '../../../utils/formatters'
import EventStatusBadge from '../../../components/EventStatusBadge'
import MapSearch from './MapSearch'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

const CATEGORY_COLORS = {
  SPORTS:  '#22C55E',
  MUSIC:   '#8B5CF6',
  ARTS:    '#EC4899',
  FOOD:    '#F59E0B',
  TECH:    '#3B82F6',
  OUTDOOR: '#10B981',
  SOCIAL:  '#6C63FF',
  OTHER:   '#6B7280',
}

function createCategoryIcon(category, status) {
  const color = status === 'FULL' ? '#F59E0B' : (CATEGORY_COLORS[category] || '#6C63FF')
  return L.divIcon({
    className: '',
    html: `<div style="
      width:32px;height:32px;
      background:${color};
      border-radius:50% 50% 50% 0;
      transform:rotate(-45deg);
      box-shadow:0 3px 10px rgba(0,0,0,0.3);
      border:2.5px solid white;
    "></div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -36],
  })
}

function RecenterMap({ center }) {
  const map = useMap()
  const prev = useRef(null)
  useEffect(() => {
    const [lat, lng] = center
    if (!prev.current ||
        Math.abs(prev.current[0] - lat) > 0.001 ||
        Math.abs(prev.current[1] - lng) > 0.001) {
      map.setView(center, map.getZoom(), { animate: true })
      prev.current = center
    }
  }, [center, map])
  return null
}

function LocateButton({ center }) {
  const map = useMap()
  return (
    <button
      title="Go to my location"
      onClick={() => map.flyTo(center, 14, { duration: 0.8 })}
      style={{
        position: 'absolute',
        top: 10,
        right: 10,
        zIndex: 1000,
        width: 36,
        height: 36,
        background: 'var(--bg-primary, #fff)',
        border: '2px solid rgba(0,0,0,0.2)',
        borderRadius: 6,
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        boxShadow: '0 2px 6px rgba(0,0,0,0.2)',
        color: '#3B82F6',
        transition: 'background 0.15s',
      }}
      onMouseEnter={(e) => e.currentTarget.style.background = '#f0f0f0'}
      onMouseLeave={(e) => e.currentTarget.style.background = 'var(--bg-primary, #fff)'}
    >
      <Crosshair size={18} />
    </button>
  )
}

export default function NearbyMap({ location, events = [] }) {
  const [scrollMsg, setScrollMsg] = useState(false)

  if (!location) return null
  const center = [location.lat, location.lng]

  return (
    <div style={{ position: 'relative', lineHeight: 0 }}>
      <MapContainer
        center={center}
        zoom={12}
        style={{ height: 'clamp(240px, 38vw, 420px)', width: '100%' }}
        scrollWheelZoom={true}
        zoomControl={false}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <ZoomControl position="bottomright" />
        <RecenterMap center={center} />
        <MapSearch />
        <LocateButton center={center} />

        {/* User location */}
        <CircleMarker
          center={center}
          radius={7}
          pathOptions={{ color: '#fff', weight: 2.5, fillColor: '#3B82F6', fillOpacity: 1 }}
        />
        <CircleMarker
          center={center}
          radius={16}
          pathOptions={{ color: '#3B82F6', weight: 1.5, fillColor: '#3B82F6', fillOpacity: 0.12 }}
        />

        {events.map((event) => {
          const lat = event.latitude ?? event.lat
          const lng = event.longitude ?? event.lng
          if (lat == null || lng == null) return null
          return (
            <Marker
              key={event.id}
              position={[lat, lng]}
              icon={createCategoryIcon(event.category, event.status)}
            >
              <Popup>
                <div className="map-popup">
                  <div className="map-popup-title">{event.title}</div>
                  <div className="map-popup-meta">
                    {formatEventDate(event.eventDate)}<br />
                    {event.venueName}
                  </div>
                  <div style={{ marginBottom: 8 }}>
                    <EventStatusBadge status={event.status} />
                  </div>
                  <Link to={`/events/${event.id}`} className="map-popup-link">
                    View Event →
                  </Link>
                </div>
              </Popup>
            </Marker>
          )
        })}
      </MapContainer>

      {/* Legend */}
      <div style={{
        position: 'absolute', bottom: 10, left: 10, zIndex: 1000,
        background: 'rgba(17,24,39,0.80)', backdropFilter: 'blur(6px)',
        borderRadius: 8, padding: '5px 10px',
        display: 'flex', alignItems: 'center', gap: 6,
        fontSize: '0.72rem', color: '#fff', pointerEvents: 'none',
      }}>
        <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#3B82F6', display: 'inline-block', border: '1.5px solid white' }} />
        You
        <span style={{ margin: '0 4px', opacity: 0.35 }}>|</span>
        <span style={{ width: 10, height: 10, borderRadius: '50% 50% 50% 0', transform: 'rotate(-45deg)', background: '#6C63FF', display: 'inline-block' }} />
        Events
      </div>
    </div>
  )
}
