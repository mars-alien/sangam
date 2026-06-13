import { useState } from 'react'
import { Link, useNavigate, Navigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { MapPin } from 'lucide-react'
import RegisterForm from '../features/auth/components/RegisterForm'
import { authApi } from '../features/auth/api'
import useAuthStore from '../store/authStore'

export default function RegisterPage() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const setAuth = useAuthStore((s) => s.setAuth)
  const navigate = useNavigate()
  const [apiError, setApiError] = useState(null)

  const mutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: (data) => {
      const { accessToken, refreshToken, user } = data.data
      setAuth({ user, accessToken, refreshToken })
      navigate('/', { replace: true })
    },
    onError: (err) => {
      setApiError(err.response?.data?.message || 'Registration failed. Please try again.')
    },
  })

  if (isAuthenticated) return <Navigate to="/" replace />

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 20 }}>
          <MapPin size={24} color="var(--primary)" />
          <span style={{ fontWeight: 700, fontSize: '1.1rem', color: 'var(--primary)' }}>Sangam</span>
        </div>
        <h1 className="auth-title">Create account</h1>
        <p className="auth-subtitle">Join Sangam and find people to share experiences with</p>
        <RegisterForm
          onSubmit={(data) => { setApiError(null); mutation.mutate(data) }}
          isLoading={mutation.isPending}
          apiError={apiError}
        />
        <div className="auth-footer">
          Already have an account?{' '}
          <Link to="/login">Log in</Link>
        </div>
      </div>
    </div>
  )
}
