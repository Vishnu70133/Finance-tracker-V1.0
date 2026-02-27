import { useEffect, useState } from 'react'
import api from '../api'
import { useNavigate } from 'react-router-dom'
import './Profile.css'
import Navbar from '../components/Navbar'



export default function Profile() {
  const navigate = useNavigate()

  const [form, setForm] = useState({
    name: '',
    password: ''
  })

  useEffect(() => {
    api.get('/users/me').then(res => {
      setForm({
        name: res.data.name,
        password: ''
      })
    })
  }, [])

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  async function handleUpdate(e) {
    e.preventDefault()

    await api.put('/users/me', form)

    alert('Profile updated!')
  }

  async function handleDelete() {
    if (!confirm('Delete your account permanently?')) return

    await api.delete('/users/me')

    localStorage.removeItem('token')
    navigate('/login')
  }
  const userEmail = (() => {
  try {
    const token = localStorage.getItem('token')
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.sub || payload.email || 'User'
  } catch {
    return 'User'
  }
})()

function handleLogout() {
  localStorage.removeItem('token')
  navigate('/login')
}

  return (
    <>
      <Navbar userEmail={userEmail} onLogout={handleLogout} />
  <div className="profile-root">
    
    <div className="profile-card">
      <div className="profile-header">
        <h2 className="profile-title">My Profile</h2>

        <button className="profile-back-btn" onClick={() => navigate('/dashboard')}>
            ← Back
        </button>
        </div>

      <form className="profile-form" onSubmit={handleUpdate}>
        <input
          className="profile-input"
          name="name"
          value={form.name}
          onChange={handleChange}
          placeholder="Name"
        />

        <input
          className="profile-input"
          name="password"
          type="password"
          value={form.password}
          onChange={handleChange}
          placeholder="New Password (optional)"
        />

        <button className="profile-update-btn" type="submit">
          Update Profile
        </button>
      </form>

      <hr className="profile-divider" />

      <button
        className="profile-delete-btn"
        onClick={handleDelete}
      >
        Delete Account
      </button>

    </div>

  </div>
  </>
)
}