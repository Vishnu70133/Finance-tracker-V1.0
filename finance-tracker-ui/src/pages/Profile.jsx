import { useEffect, useState } from 'react'
import api from '../api'
import { useNavigate } from 'react-router-dom'
import './Profile.css'
import Navbar from '../components/Navbar'
import { useToast } from '../context/ToastContext'
import ConfirmModal from '../components/ConfirmModal'

export default function Profile() {
  const navigate = useNavigate()

  const [isEditMode, setIsEditMode] = useState(false)
  const [profile, setProfile] = useState({
    name: '',
    email: ''
  })
  const [form, setForm] = useState({
    name: '',
    password: ''
  })

  // Custom confirmation modal and toast states
  const [deleteModalOpen, setDeleteModalOpen] = useState(false)
  const [deleteLoading, setDeleteLoading] = useState(false)
  const { showToast } = useToast()

  useEffect(() => {
    api.get('/users/me').then(res => {
      setProfile({
        name: res.data.name,
        email: res.data.email
      })
      setForm({
        name: res.data.name,
        password: ''
      })
    })
  }, [])

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  function handleCancel() {
    setForm({
      name: profile.name,
      password: ''
    })
    setIsEditMode(false)
  }

  async function handleUpdate(e) {
    e.preventDefault()
    try {
      const res = await api.put('/users/me', form)
      setProfile({
        name: res.data.name,
        email: res.data.email
      })
      setForm({
        name: res.data.name,
        password: ''
      })
      setIsEditMode(false)
      showToast('Profile updated successfully!', 'success')
    } catch (err) {
      console.error(err)
      showToast('Failed to update profile.', 'error')
    }
  }

  function handleDelete() {
    setDeleteModalOpen(true)
  }

  async function handleConfirmDelete() {
    if (deleteLoading) return
    setDeleteLoading(true)
    try {
      await api.delete('/users/me')
      showToast('Account deleted successfully.', 'success')
      localStorage.removeItem('token')
      localStorage.removeItem('activeChatSessionId')
      navigate('/login')
    } catch (err) {
      console.error(err)
      showToast('Failed to delete account.', 'error')
    } finally {
      setDeleteLoading(false)
      setDeleteModalOpen(false)
    }
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
            <h2 className="profile-title">{isEditMode ? 'Edit Profile' : 'My Profile'}</h2>
            <button className="profile-back-btn" onClick={() => navigate('/dashboard')}>
              ← Back
            </button>
          </div>

          {!isEditMode ? (
            // View Mode
            <div className="profile-details">
              <div className="profile-field">
                <label>Name</label>
                <div className="profile-value">{profile.name}</div>
              </div>

              <div className="profile-field">
                <label>Email</label>
                <div className="profile-value">{profile.email}</div>
              </div>

              <div className="profile-field">
                <label>Password</label>
                <div className="profile-value">••••••••••</div>
              </div>

              <button className="profile-edit-btn" onClick={() => setIsEditMode(true)}>
                Edit Profile
              </button>
            </div>
          ) : (
            // Edit Mode
            <form className="profile-form" onSubmit={handleUpdate}>
              <div className="profile-field">
                <label>Name</label>
                <input
                  className="profile-input"
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  placeholder="Name"
                  required
                />
              </div>

              <div className="profile-field">
                <label>Email</label>
                <input
                  className="profile-input read-only-input"
                  name="email"
                  value={profile.email}
                  readOnly
                  disabled
                />
              </div>

              <div className="profile-field">
                <label>New Password</label>
                <input
                  className="profile-input"
                  name="password"
                  type="password"
                  value={form.password}
                  onChange={handleChange}
                  placeholder="New Password (optional)"
                />
              </div>

              <div className="profile-actions">
                <button className="profile-cancel-btn" type="button" onClick={handleCancel}>
                  Cancel
                </button>
                <button className="profile-save-btn" type="submit">
                  Save Changes
                </button>
              </div>
            </form>
          )}

          <hr className="profile-divider" />

          <button className="profile-delete-btn" onClick={handleDelete}>
            Delete Account
          </button>
        </div>
      </div>
      
      <ConfirmModal
        isOpen={deleteModalOpen}
        title="Delete Account?"
        message="Are you sure you want to permanently delete your account? This action cannot be undone."
        confirmText="Delete Account"
        cancelText="Cancel"
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteModalOpen(false)}
        loading={deleteLoading}
      />
    </>
  )
}