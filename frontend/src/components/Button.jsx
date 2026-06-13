export default function Button({
  children,
  variant = 'primary',
  size = '',
  full = false,
  loading = false,
  className = '',
  ...props
}) {
  const cls = [
    'btn',
    `btn-${variant}`,
    size && `btn-${size}`,
    full && 'btn-full',
    className,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <button className={cls} disabled={loading || props.disabled} {...props}>
      {loading && <span className="spinner spinner-sm" style={{ borderColor: 'rgba(255,255,255,0.3)', borderTopColor: 'white' }} />}
      {children}
    </button>
  )
}
