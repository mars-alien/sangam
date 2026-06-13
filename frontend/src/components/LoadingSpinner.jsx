export default function LoadingSpinner({ size = '' }) {
  return (
    <div className="spinner-center">
      <div className={`spinner ${size ? `spinner-${size}` : ''}`} />
    </div>
  )
}
