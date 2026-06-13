import { useState, useCallback } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { MapContainer, TileLayer, Marker, ZoomControl, useMapEvents, useMap } from 'react-leaflet'
import MapSearch from './MapSearch'
import L from 'leaflet'
import { Crosshair } from 'lucide-react'
import Input from '../../../components/Input'
import Button from '../../../components/Button'
import { createEventSchema } from '../../../utils/validators'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

const CATEGORIES = ['SPORTS', 'MUSIC', 'ARTS', 'FOOD', 'TECH', 'OUTDOOR', 'SOCIAL', 'OTHER']

function MapPicker({ position, onPick }) {
  useMapEvents({
    click(e) {
      onPick({ lat: e.latlng.lat, lng: e.latlng.lng })
    },
  })
  return position ? <Marker position={[position.lat, position.lng]} /> : null
}

function LocateButton({ onLocate }) {
  const map = useMap()
  function handleClick() {
    if (!navigator.geolocation) return
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        const pos = { lat: coords.latitude, lng: coords.longitude }
        map.flyTo([pos.lat, pos.lng], 15, { duration: 0.8 })
        onLocate(pos)
      },
      () => {}
    )
  }
  return (
    <button
      type="button"
      title="Use my current location"
      onClick={handleClick}
      style={{
        position: 'absolute', top: 10, right: 10, zIndex: 1000,
        width: 36, height: 36,
        background: 'var(--bg-primary, #fff)',
        border: '2px solid rgba(0,0,0,0.2)',
        borderRadius: 6, cursor: 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        boxShadow: '0 2px 6px rgba(0,0,0,0.2)',
        color: '#3B82F6',
      }}
    >
      <Crosshair size={18} />
    </button>
  )
}

export default function EventForm({ onSubmit, isLoading, defaultValues, submitLabel = 'Create Event' }) {
  const [position, setPosition] = useState(
    defaultValues?.latitude
      ? { lat: defaultValues.latitude, lng: defaultValues.longitude }
      : null
  )

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(createEventSchema),
    defaultValues: defaultValues || {},
  })

  const timeValue = useWatch({ control, name: 'eventTime' })

  const handlePick = useCallback((pos) => setPosition(pos), [])

  function onFormSubmit(data) {
    if (!position) {
      alert('Please click on the map to set the event location.')
      return
    }
    const tags = data.tags
      ? data.tags.split(',').map((t) => t.trim()).filter(Boolean)
      : []
    const { eventTime, tags: _tags, ...rest } = data
    const eventDate = new Date(rest.eventDate + 'T' + eventTime).toISOString()
    onSubmit({
      ...rest,
      latitude: position.lat,
      longitude: position.lng,
      tags,
      minCompanions: Number(rest.minCompanions) || 1,
      maxCompanions: Number(rest.maxCompanions),
      eventDate,
    })
  }

  return (
    <form className="create-form" onSubmit={handleSubmit(onFormSubmit)}>
      <div>
        <p className="form-section-title">Event details</p>
      </div>

      <Input
        label="Title"
        placeholder="What's happening?"
        error={errors.title?.message}
        {...register('title')}
      />

      <div className="input-wrapper">
        <label className="input-label">Description</label>
        <textarea
          className={`input-field ${errors.description ? 'error' : ''}`}
          rows={4}
          placeholder="Tell people what to expect…"
          {...register('description')}
        />
        {errors.description && <span className="input-error">{errors.description.message}</span>}
      </div>

      <div className="form-row">
        <div className="input-wrapper">
          <label className="input-label">Category</label>
          <select className={`input-field ${errors.category ? 'error' : ''}`} {...register('category')}>
            <option value="">Select category</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase()}</option>
            ))}
          </select>
          {errors.category && <span className="input-error">{errors.category.message}</span>}
        </div>
        <Input
          label="Tags (comma-separated)"
          placeholder="hiking, weekend, casual"
          {...register('tags')}
        />
      </div>

      <div>
        <p className="form-section-title" style={{ marginBottom: 12 }}>Location</p>
        <div className="form-row">
          <Input
            label="Venue name"
            placeholder="Hyde Park"
            error={errors.venueName?.message}
            {...register('venueName')}
          />
          <Input
            label="City"
            placeholder="London"
            error={errors.city?.message}
            {...register('city')}
          />
        </div>
        <Input
          label="Address"
          placeholder="123 Main St"
          error={errors.address?.message}
          style={{ marginTop: 12 }}
          {...register('address')}
        />
      </div>

      <div>
        <p className="form-section-title" style={{ marginBottom: 8 }}>Pin location on map</p>
        <div className="map-picker-wrap" style={{ isolation: 'isolate' }}>
          <MapContainer
            center={position ? [position.lat, position.lng] : [12.9716, 77.5946]}
            zoom={position ? 14 : 11}
            style={{ height: '300px', width: '100%' }}
            scrollWheelZoom={true}
            zoomControl={false}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <ZoomControl position="bottomright" />
            <MapSearch onSelect={handlePick} />
            <LocateButton onLocate={handlePick} />
            <MapPicker position={position} onPick={handlePick} />
          </MapContainer>
        </div>
        <p className="map-picker-hint">Click on the map to pin the location · scroll to zoom</p>
        {position && (
          <p className="coords-display">
            📍 {position.lat.toFixed(5)}, {position.lng.toFixed(5)}
          </p>
        )}
      </div>

      <div>
        <p className="form-section-title" style={{ marginBottom: 12 }}>Schedule & capacity</p>
        <div className="form-row">
          <Input
            label="Date"
            type="date"
            error={errors.eventDate?.message}
            {...register('eventDate')}
          />
          <div className="input-wrapper">
            <label className="input-label">Time</label>
            <div style={{ position: 'relative' }}>
              <input
                type="time"
                className={`input-field${errors.eventTime ? ' error' : ''}`}
                style={{ color: timeValue ? undefined : 'transparent' }}
                {...register('eventTime')}
              />
              {!timeValue && (
                <span style={{
                  position: 'absolute', left: 12, top: '50%',
                  transform: 'translateY(-50%)',
                  color: 'var(--text-secondary)',
                  pointerEvents: 'none', fontSize: '0.9rem',
                }}>
                  e.g. 14:30
                </span>
              )}
            </div>
            {errors.eventTime && <span className="input-error">{errors.eventTime.message}</span>}
          </div>
        </div>
        <div className="form-row" style={{ marginTop: 0 }}>
          <Input
            label="Min companions"
            type="number"
            min={1}
            defaultValue={1}
            error={errors.minCompanions?.message}
            {...register('minCompanions')}
          />
          <Input
            label="Max companions"
            type="number"
            min={1}
            placeholder="10"
            error={errors.maxCompanions?.message}
            {...register('maxCompanions')}
          />
        </div>
      </div>

      <Button type="submit" variant="primary" size="lg" full loading={isLoading}>
        {isLoading ? 'Saving…' : submitLabel}
      </Button>
    </form>
  )
}
