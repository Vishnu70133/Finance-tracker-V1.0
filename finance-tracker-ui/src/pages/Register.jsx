import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import './Login.css'   // reuse same styling if you want

export default function Register() {
  const navigate = useNavigate()

  const [form, setForm] = useState({
    name: '',
    email: '',
    password: ''
  })

  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
    setError('')
  }

  async function handleSubmit(e) {
    e.preventDefault()

    if (!form.name || !form.email || !form.password) {
      setError('Please fill in all fields.')
      return
    }

    setLoading(true)

    try {
      // ✅ REGISTER USER
      await api.post('/auth/register', form)

      // 👉 After register go to login
      navigate('/login', { state: { registered: true } })

    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Registration failed.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-root">
      <div className="login-card animate-fade-up">

        <h1 className="login-title">Create Account</h1>
        <p className="login-subtitle">Start tracking your finances</p>

        <form className="login-form" onSubmit={handleSubmit} noValidate>

          <div className="field-group">
            <label className="field-label">Name</label>
            <input
              name="name"
              type="text"
              className="field-input"
              placeholder="Your Name"
              value={form.name}
              onChange={handleChange}
            />
          </div>

          <div className="field-group">
            <label className="field-label">Email</label>
            <input
              name="email"
              type="email"
              className="field-input"
              placeholder="you@example.com"
              value={form.email}
              onChange={handleChange}
            />
          </div>

          <div className="field-group">
            <label className="field-label">Password</label>
            <input
              name="password"
              type="password"
              className="field-input"
              placeholder="••••••••"
              value={form.password}
              onChange={handleChange}
            />
          </div>

          {error && <div className="login-error">{error}</div>}

          <button type="submit" className="login-btn" disabled={loading}>
            {loading ? 'Creating...' : 'Register'}
          </button>

          <p style={{ marginTop: 10 }}>
            Already have an account?{' '}
            <span
              style={{ cursor: 'pointer', color: '#4dabf7' }}
              onClick={() => navigate('/login')}
            >
              Login
            </span>
          </p>

        </form>
      </div>
    </div>
  )
}