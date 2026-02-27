import { useState ,useEffect } from 'react'
import api from '../api'
import './AddTransactionForm.css'

const DEFAULT_FORM = {
  amount: '',
  type: 'EXPENSE',
  description: '',
  date: new Date().toISOString().split('T')[0],
  categoryId: ''
}

export default function AddTransactionForm({ onSuccess,
  onCancel,
  editingTransaction }) {
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(DEFAULT_FORM)
  useEffect(() => {
  if (editingTransaction) {
    setForm({
      amount: editingTransaction.amount,
      type: editingTransaction.type,
      description: editingTransaction.description,
      date: editingTransaction.date,
      categoryId: editingTransaction.categoryId
    })
  }
}, [editingTransaction])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  

  useEffect(() => {
  api.get('/categories')
    .then(res => setCategories(res.data))
    .catch(() => console.log("Failed to load categories"))
}, [])

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value })
    setError('')
  }

  async function handleSubmit(e) {
  e.preventDefault()

  if (!form.amount || !form.description || !form.date || !form.categoryId) {
    setError('Please fill in all required fields.')
    return
  }

  if (isNaN(Number(form.amount)) || Number(form.amount) <= 0) {
    setError('Amount must be a positive number.')
    return
  }

  setLoading(true)

  try {
    // ✅ IF EDITING → UPDATE API
    if (editingTransaction) {
      await api.put(`/transactions/${editingTransaction.id}`, {
        amount: Number(form.amount),
        type: form.type,
        description: form.description,
        date: form.date,
      })
    } else {
      // ✅ CREATE NEW
      await api.post('/transactions', {
        amount: Number(form.amount),
        type: form.type,
        description: form.description,
        date: form.date,
      }, {
        params: { categoryId: form.categoryId }
      })
    }

    setForm(DEFAULT_FORM)
    onSuccess()

  } catch (err) {
    const message =
      err.response?.data?.message ||
      err.response?.data?.error ||
      'Failed to save transaction.'
    setError(message)
  } finally {
    setLoading(false)
  }
}

  return (
    <div className="atf-card">
      <div className="atf-header">
       <h3 className="atf-title">
  {editingTransaction ? 'Update Transaction' : 'New Transaction'}
</h3>
        <button className="atf-close" onClick={onCancel} aria-label="Close">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <form className="atf-form" onSubmit={handleSubmit} noValidate>
        <div className="atf-grid">

          {/* Type toggle */}
          <div className="atf-field atf-field-full">
            <label className="atf-label">Type</label>
            <div className="atf-toggle">
              {['EXPENSE', 'INCOME'].map((t) => (
                <button
                  key={t}
                  type="button"
                  className={`atf-toggle-btn atf-toggle-${t.toLowerCase()} ${form.type === t ? 'active' : ''}`}
                  onClick={() => setForm({ ...form, type: t })}
                >
                  {t === 'INCOME' ? '↑ Income' : '↓ Expense'}
                </button>
              ))}
            </div>
          </div>

          {/* Amount */}
          <div className="atf-field">
            <label className="atf-label" htmlFor="amount">Amount *</label>
            <input
              id="amount"
              name="amount"
              type="number"
              min="0.01"
              step="0.01"
              className="atf-input"
              placeholder="0.00"
              value={form.amount}
              onChange={handleChange}
            />
          </div>

          {/* Date */}
          <div className="atf-field">
            <label className="atf-label" htmlFor="date">Date *</label>
            <input
              id="date"
              name="date"
              type="date"
              className="atf-input"
              value={form.date}
              onChange={handleChange}
            />
          </div>

          {/* Description */}
          <div className="atf-field">
            <label className="atf-label" htmlFor="description">Description *</label>
            <input
              id="description"
              name="description"
              type="text"
              className="atf-input"
              placeholder="e.g. Groceries"
              value={form.description}
              onChange={handleChange}
            />
          </div>

          {/* Category */}
<div className="atf-field">
  <label className="atf-label" htmlFor="categoryId">Category *</label>

  <select
    id="categoryId"
    name="categoryId"
    className="atf-input"
    value={form.categoryId}
    onChange={handleChange}
  >
    <option value="">Select Category</option>

    {categories.map((c) => (
      <option key={c.id} value={c.id}>
        {c.name}
      </option>
    ))}
  </select>
</div>

        </div>

        {error && (
          <div className="atf-error" role="alert">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            {error}
          </div>
        )}

        <div className="atf-actions">
          <button type="button" className="atf-btn-cancel" onClick={onCancel}>
            Cancel
          </button>
<button type="submit" className="atf-btn-submit" disabled={loading}>
  {loading
    ? <span className="atf-spinner" />
    : editingTransaction ? 'Update Transaction' : 'Add Transaction'}
</button>
        </div>
      </form>
    </div>
  )
}
