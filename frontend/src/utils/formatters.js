import { format, formatDistanceToNow, isToday, isTomorrow } from 'date-fns'

export function formatEventDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isToday(date)) return `Today at ${format(date, 'h:mm a')}`
  if (isTomorrow(date)) return `Tomorrow at ${format(date, 'h:mm a')}`
  return format(date, 'EEE, MMM d · h:mm a')
}

export function formatRelativeDate(dateStr) {
  if (!dateStr) return ''
  return formatDistanceToNow(new Date(dateStr), { addSuffix: true })
}

export function formatDistance(km) {
  if (km == null) return null
  if (km < 1) return `${Math.round(km * 1000)} m away`
  return `${km.toFixed(1)} km away`
}

export function formatSpotsLeft(current, max) {
  const taken = current ?? 0
  const left = max - taken
  if (left <= 0) return 'Full'
  if (left === 1) return '1 spot left'
  return `${left} spots left`
}

export function categoryLabel(cat) {
  const map = {
    SPORTS: 'Sports',
    MUSIC: 'Music',
    ARTS: 'Arts',
    FOOD: 'Food',
    TECH: 'Tech',
    OUTDOOR: 'Outdoor',
    SOCIAL: 'Social',
    OTHER: 'Other',
  }
  return map[cat] || cat
}
