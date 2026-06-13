import { useEffect } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import Navbar from './Navbar'

export default function Layout() {
  const navigate = useNavigate()

  useEffect(() => {
    const handleExpired = () => navigate('/login', { state: { sessionExpired: true } })
    window.addEventListener('session:expired', handleExpired)
    return () => window.removeEventListener('session:expired', handleExpired)
  }, [navigate])

  return (
    <>
      <Navbar />
      <main style={{ flex: 1, paddingTop: 60 }}>
        <Outlet />
      </main>
    </>
  )
}
