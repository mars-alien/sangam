import { Clock } from 'lucide-react'

export default function WaitlistBadge({ position }) {
  return (
    <span className="badge badge-amber">
      <Clock size={12} />
      Waitlist {position ? `#${position}` : ''}
    </span>
  )
}
