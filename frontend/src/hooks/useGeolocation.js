import { useState, useEffect } from 'react'

const DEFAULT_LOCATION = { lat: 12.9716, lng: 77.5946 } // Bengaluru

export default function useGeolocation() {
  const [location, setLocation] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [permissionDenied, setPermissionDenied] = useState(false)

  useEffect(() => {
    if (!navigator.geolocation) {
      setError('Geolocation not supported')
      setLocation(DEFAULT_LOCATION)
      setLoading(false)
      return
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLocation({ lat: pos.coords.latitude, lng: pos.coords.longitude })
        setLoading(false)
      },
      (err) => {
        if (err.code === err.PERMISSION_DENIED) {
          setPermissionDenied(true)
        }
        setError(err.message)
        setLocation(DEFAULT_LOCATION)
        setLoading(false)
      },
      { timeout: 8000, maximumAge: 300000 }
    )
  }, [])

  return { location, error, loading, permissionDenied }
}
