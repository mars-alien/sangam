import { Link } from 'react-router-dom'
import {
  Music, Trophy, Palette, UtensilsCrossed, Cpu, TreePine, Users, Tag,
  Calendar, MapPin, Navigation,
} from 'lucide-react'
import EventStatusBadge from '../../../components/EventStatusBadge'
import UserAvatar from '../../users/components/UserAvatar'
import { formatEventDate, formatDistance, formatSpotsLeft, categoryLabel } from '../../../utils/formatters'

const CATEGORY_ICONS = {
  SPORTS: Trophy,
  MUSIC: Music,
  ARTS: Palette,
  FOOD: UtensilsCrossed,
  TECH: Cpu,
  OUTDOOR: TreePine,
  SOCIAL: Users,
  OTHER: Tag,
}

export default function EventCard({ event }) {
  const Icon = CATEGORY_ICONS[event.category] || Tag
  const fillPct = event.maxCompanions
    ? Math.min(100, (event.currentMemberCount / event.maxCompanions) * 100)
    : 0
  const isFull = event.status === 'FULL'
  const dist = formatDistance(event.distanceKm)

  return (
    <Link to={`/events/${event.id}`} className="card event-card">
      <div className="event-card-header">
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
          <div className="category-icon">
            <Icon size={16} color="var(--primary)" />
          </div>
          <div>
            <div className="event-card-title">{event.title}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 2 }}>
              {categoryLabel(event.category)}
            </div>
          </div>
        </div>
        <EventStatusBadge status={event.status} />
      </div>

      <div className="event-card-meta">
        <div className="event-card-meta-row">
          <Calendar size={13} />
          {formatEventDate(event.eventDate)}
        </div>
        <div className="event-card-meta-row">
          <MapPin size={13} />
          {event.venueName}{event.city ? `, ${event.city}` : ''}
        </div>
        {dist && (
          <div className="event-card-meta-row">
            <Navigation size={13} />
            {dist}
          </div>
        )}
      </div>

      <div className="event-card-spots">
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
          <span>{event.currentMemberCount ?? 0} / {event.maxCompanions} joined</span>
          <span style={{ color: isFull ? 'var(--warning)' : 'var(--text-muted)' }}>
            {formatSpotsLeft(event.currentMemberCount, event.maxCompanions)}
          </span>
        </div>
        <div className="progress-bar">
          <div className={`progress-bar-fill ${isFull ? 'full' : ''}`} style={{ width: `${fillPct}%` }} />
        </div>
      </div>

      <div className="event-card-footer">
        <div className="creator-row">
          <UserAvatar user={event.creator} size="sm" />
          <span>{event.creator?.username}</span>
        </div>
        {event.tags?.length > 0 && (
          <div style={{ display: 'flex', gap: 4 }}>
            {event.tags.slice(0, 2).map((t) => (
              <span key={t} className="badge badge-purple" style={{ fontSize: '0.7rem', padding: '2px 6px' }}>
                {t}
              </span>
            ))}
          </div>
        )}
      </div>
    </Link>
  )
}
