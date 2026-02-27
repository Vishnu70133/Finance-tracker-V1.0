import { useNavigate } from 'react-router-dom'

export default function Navbar({ userEmail, onLogout }) {
  const navigate = useNavigate()

  return (
    <nav className="dash-nav">
      {/* BRAND */}
      <div
        className="dash-nav-brand"
        onClick={() => navigate('/dashboard')}
        style={{ cursor: 'pointer' }}
      >
        <div className="dash-logo">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
            <polyline points="22 7 13.5 15.5 8.5 10.5 2 17" />
            <polyline points="16 7 22 7 22 13" />
          </svg>
        </div>
        <span className="dash-brand-text">Fintrack</span>
      </div>

      {/* RIGHT SIDE */}
      <div className="dash-nav-right">
        <span className="dash-user-pill">{userEmail}</span>

        <button
          className="dash-analytics-btn"
          onClick={() => navigate('/analytics')}
        >
          Analytics
        </button>

        <button
          className="dash-profile-btn"
          onClick={() => navigate('/profile')}
        >
          Profile
        </button>

        <button className="dash-logout-btn" onClick={onLogout}>
          Logout
        </button>
      </div>
    </nav>
  )
}