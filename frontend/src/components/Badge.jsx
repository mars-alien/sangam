export default function Badge({ children, color = 'gray', className = '' }) {
  return (
    <span className={`badge badge-${color} ${className}`}>
      {children}
    </span>
  )
}
