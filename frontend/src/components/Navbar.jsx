import { NavLink, useNavigate } from 'react-router-dom'
import { MapPin, PlusCircle, LayoutList, LogOut, LogIn, UserCircle } from 'lucide-react'
import useAuthStore from '../store/authStore'
import { authApi } from '../features/auth/api'

export default function Navbar() {
  const { isAuthenticated, user, refreshToken, logout } = useAuthStore()
  const navigate = useNavigate()

  async function handleLogout() {
    try { await authApi.logout(refreshToken) } catch { /* ignore */ }
    logout()
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <NavLink to="/" className="navbar-logo">
          <MapPin size={20} />
          <span className="navbar-logo-text">Sangam</span>
        </NavLink>

        <div className="navbar-links">
          <NavLink
            to="/"
            end
            className={({ isActive }) => `navbar-link${isActive ? ' active' : ''}`}
          >
            <span className="nav-link-icon"><LayoutList size={16} /></span>
            <span className="nav-link-text">Discover</span>
          </NavLink>
          {isAuthenticated && (
            <NavLink
              to="/my-events"
              className={({ isActive }) => `navbar-link${isActive ? ' active' : ''}`}
            >
              <span className="nav-link-icon"><UserCircle size={16} /></span>
              <span className="nav-link-text">My Events</span>
            </NavLink>
          )}
        </div>

        <div className="navbar-actions">
          {isAuthenticated ? (
            <>
              <NavLink to="/events/create" className="btn btn-primary btn-sm">
                <PlusCircle size={15} />
                <span className="nav-action-text">Create</span>
              </NavLink>
              <span className="navbar-username">{user?.username}</span>
              <button className="btn btn-ghost btn-sm navbar-logout" onClick={handleLogout} title="Log out">
                <LogOut size={16} />
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className="btn btn-ghost btn-sm">
                <LogIn size={15} />
                <span className="nav-action-text">Log in</span>
              </NavLink>
              <NavLink to="/register" className="btn btn-primary btn-sm">
                Sign up
              </NavLink>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
