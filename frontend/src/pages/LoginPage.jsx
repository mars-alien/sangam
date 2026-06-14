import { useState } from 'react'
import { Link, useNavigate, useLocation, Navigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { MapPin } from 'lucide-react'
import LoginForm from '../features/auth/components/LoginForm'
import { authApi } from '../features/auth/api'
import useAuthStore from '../store/authStore'

export default function LoginPage() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const setAuth = useAuthStore((s) => s.setAuth)
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from?.pathname || '/'

  const [apiError, setApiError] = useState(null)

  const mutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      const { accessToken, refreshToken, user } = data.data
      setAuth({ user, accessToken, refreshToken })
      navigate(from, { replace: true })
    },
    onError: (err) => {
      if (!err.response) {
        setApiError('Cannot reach server — check your internet connection and try again.')
      } else {
        setApiError(err.response.data?.message || 'Login failed. Please try again.')
      }
    },
  })

  const sessionExpired = location.state?.sessionExpired

  if (isAuthenticated) return <Navigate to="/" replace />

  return (
    <div className="auth-page">
      <div className="auth-card">
        {sessionExpired && (
          <div className="session-expired-banner">
            Your session expired — please sign in again.
          </div>
        )}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 20 }}>
          <MapPin size={24} color="var(--primary)" />
          <span style={{ fontWeight: 700, fontSize: '1.1rem', color: 'var(--primary)' }}>Sangam</span>
        </div>
        <h1 className="auth-title">Welcome back</h1>
        <p className="auth-subtitle">Sign in to discover events around you</p>
        <LoginForm
          onSubmit={(data) => { setApiError(null); mutation.mutate(data) }}
          isLoading={mutation.isPending}
          apiError={apiError}
        />
        <div className="auth-footer">
          Don't have an account?{' '}
          <Link to="/register">Sign up</Link>
        </div>
      </div>
    </div>
  )
}
