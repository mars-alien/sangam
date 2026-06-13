import Badge from './Badge'

const STATUS_MAP = {
  OPEN: { color: 'green', label: 'Open' },
  FULL: { color: 'orange', label: 'Full' },
  ONGOING: { color: 'blue', label: 'Ongoing' },
  COMPLETED: { color: 'gray', label: 'Completed' },
  CANCELLED: { color: 'red', label: 'Cancelled' },
  DRAFT: { color: 'purple', label: 'Draft' },
}

export default function EventStatusBadge({ status }) {
  const { color, label } = STATUS_MAP[status] || { color: 'gray', label: status }
  return <Badge color={color}>{label}</Badge>
}
