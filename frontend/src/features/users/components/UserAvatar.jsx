export default function UserAvatar({ user, size = '' }) {
  const initials = user?.displayName
    ? user.displayName[0].toUpperCase()
    : user?.username
    ? user.username[0].toUpperCase()
    : '?'

  if (user?.avatarUrl) {
    return (
      <img
        src={user.avatarUrl}
        alt={user.username}
        className={`avatar ${size ? `avatar-${size}` : ''}`}
        style={{ objectFit: 'cover' }}
      />
    )
  }

  return (
    <div className={`avatar ${size ? `avatar-${size}` : ''}`}>
      {initials}
    </div>
  )
}
