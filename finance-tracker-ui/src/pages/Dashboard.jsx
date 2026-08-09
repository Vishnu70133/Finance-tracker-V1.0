import { useState, useEffect, useCallback } from 'react'
import CategoryPieChart from '../components/CategoryPieChart'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import TransactionTable from '../components/TransactionTable'
import AddTransactionForm from '../components/AddTransactionForm'
import './Dashboard.css'
import Navbar from '../components/Navbar'
import { useToast } from '../context/ToastContext'
import ConfirmModal from '../components/ConfirmModal'


export default function Dashboard() {
  const [editingTransaction, setEditingTransaction] = useState(null)
  const [categoryStats, setCategoryStats] = useState([])
  const [startDate, setStartDate] = useState('')
  
const [endDate, setEndDate] = useState('')
  const navigate = useNavigate()
  const [summary, setSummary] = useState({
  income: 0,
  expense: 0,
})
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [showForm, setShowForm] = useState(false)

  // Custom confirmation modal states
  const [deleteModalOpen, setDeleteModalOpen] = useState(false)
  const [transactionToDelete, setTransactionToDelete] = useState(null)
  const [deleteLoading, setDeleteLoading] = useState(false)
  const { showToast } = useToast()

  const pageSize = 5

  // Get user email from token payload (basic decode, no library needed)
  const userEmail = (() => {
    try {
      const token = localStorage.getItem('token')
      const payload = JSON.parse(atob(token.split('.')[1]))
      return payload.sub || payload.email || 'User'
    } catch {
      return 'User'
    }
  })()

  function handleDelete(id) {
    setTransactionToDelete(id)
    setDeleteModalOpen(true)
  }

  async function handleConfirmDelete() {
    if (!transactionToDelete || deleteLoading) return
    setDeleteLoading(true)
    try {
      await api.delete(`/transactions/${transactionToDelete}`)
      showToast('Transaction deleted successfully.', 'success')
      fetchTransactions(page)
      fetchSummary()
      fetchCategoryStats()
    } catch (err) {
      console.error(err)
      showToast('Failed to delete transaction.', 'error')
    } finally {
      setDeleteLoading(false)
      setDeleteModalOpen(false)
      setTransactionToDelete(null)
    }
  }
function handleEdit(transaction) {
  setEditingTransaction(transaction)
  setShowForm(true)
}
  
const fetchCategoryStats = useCallback(async () => {
  try {
    const res = await api.get('/transactions/my/category-summary')
    setCategoryStats(res.data)
  } catch (err) {
    console.error('Category stats failed', err)
  }
}, [])
  const fetchSummary = useCallback(async () => {
  try {
    const res = await api.get('/transactions/my/summary')
    setSummary(res.data)
  } catch (err) {
    console.error('Summary load failed', err)
  }
}, [])

  const fetchTransactions = useCallback(async (pageNum) => {
    setLoading(true)
    setError('')
    try {
      const res = await api.get(`/transactions/my/page?page=${pageNum}&size=${pageSize}`)
      const content = res.data?.data?.content ?? []
      setTransactions(content)
      // If fewer results than page size, no more pages
      setHasMore(content.length === pageSize)
    } catch (err) {
  console.error(err)
  setError('Failed to load transactions. Please try again.')
}
 finally {
      setLoading(false)
    }
  }, [])

useEffect(() => {
  fetchTransactions(page)
  fetchSummary()
  fetchCategoryStats()
}, [page, fetchTransactions, fetchSummary, fetchCategoryStats])

  function handleLogout() {
    localStorage.removeItem('token')
    navigate('/login')
  }

  function handlePrev() {
    if (page > 0) setPage((p) => p - 1)
  }

  function handleNext() {
    if (hasMore) setPage((p) => p + 1)
  }

  function handleTransactionAdded() {
  setShowForm(false)
  setPage(0)

  setEditingTransaction(null)
  fetchTransactions(0)
  fetchSummary()
  fetchCategoryStats()
}
async function handleDateFilter() {
  if (!startDate || !endDate) {
    setError('Please select start and end dates')
    return
  }

  try {
    setLoading(true)

    const res = await api.get(
  `/transactions/my/range?start=${startDate}&end=${endDate}`
)

    setTransactions(res.data)
    setHasMore(false) // disable pagination while filtered
  } catch (err) {
    console.error(err)
    setError('Failed to load filtered transactions')
  } finally {
    setLoading(false)
  }
}
  // Summary stats from current page
  const totalIncome = summary.totalIncome || 0
const totalExpense = summary.totalExpense || 0

  return (
    <div className="dash-root">
  <Navbar userEmail={userEmail} onLogout={handleLogout} />

      {/* ── Page Body ── */}
      <main className="dash-main">
        {/* Header */}
        <div className="dash-header animate-fade-up">
          <div>
            <h1 className="dash-title">Dashboard</h1>
            <p className="dash-subtitle">Track your income and expenses</p>
          </div>
          <button className="dash-add-btn" onClick={() => setShowForm((v) => !v)}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            {showForm ? 'Cancel' : 'Add Transaction'}
          </button>
        </div>

        {/* Date Filter */}
<div className="dash-filter">
  <input
    type="date"
    className="dash-date"
    value={startDate}
    onChange={(e) => setStartDate(e.target.value)}
  />

  <input
    type="date"
    className="dash-date"
    value={endDate}
    onChange={(e) => setEndDate(e.target.value)}
  />

  <button className="dash-filter-btn" onClick={handleDateFilter}>
    Apply Filter
  </button>

  <button
    className="dash-filter-clear"
    onClick={() => {
      setStartDate('')
      setEndDate('')
      fetchTransactions(0)
    }}
  >
    Reset
  </button>
</div>

        {/* Stats Cards */}
<div className="dash-stats animate-fade-up" style={{ animationDelay: '0.05s' }}>

  <div className="stat-card">
    <span className="stat-label">Total Income</span>
    <span className="stat-value stat-green">
      +₹{totalIncome.toLocaleString()}
    </span>
  </div>

  <div className="stat-card">
    <span className="stat-label">Total Expenses</span>
    <span className="stat-value stat-red">
      −₹{totalExpense.toLocaleString()}
    </span>
  </div>

  <div className="stat-card">
    <span className="stat-label">Net Balance</span>
    <span
      className={`stat-value ${
        totalIncome - totalExpense >= 0 ? 'stat-green' : 'stat-red'
      }`}
    >
      {totalIncome - totalExpense >= 0 ? '+' : '−'}₹
      {Math.abs(totalIncome - totalExpense).toLocaleString()}
    </span>
  </div>

  <div className="stat-card">
    <span className="stat-label">Transactions (Page)</span>
    <span className="stat-value">{transactions.length}</span>
  </div>

</div>
<div className="dash-chart">
  <h2 className="dash-section-title">Expenses by Category</h2>
  <CategoryPieChart data={categoryStats} />
</div>

        {/* Add Transaction Form */}
        {showForm && (
          <div className="dash-form-wrapper animate-fade-up">
            <AddTransactionForm
  onSuccess={handleTransactionAdded}
  onCancel={() => {
    setShowForm(false)
    setEditingTransaction(null)
  }}
  editingTransaction={editingTransaction}
/>
          </div>
        )}

        {/* Transaction Table */}
        <div className="dash-table-section animate-fade-up" style={{ animationDelay: '0.1s' }}>
          <div className="dash-section-header">
            <h2 className="dash-section-title">Recent Transactions</h2>
            <span className="dash-page-badge">Page {page + 1}</span>
          </div>

          {error && (
            <div className="dash-error">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
              {error}
            </div>
          )}

          <TransactionTable
            transactions={transactions}
            loading={loading}
            onEdit={handleEdit}
            onDelete={handleDelete}
          />

          {/* Pagination */}
          <div className="dash-pagination">
            <button
              className="page-btn"
              onClick={handlePrev}
              disabled={page === 0 || loading}
            >
              ← Prev
            </button>
            <span className="page-info">Page {page + 1}</span>
            <button
              className="page-btn"
              onClick={handleNext}
              disabled={!hasMore || loading}
            >
              Next →
            </button>
          </div>
        </div>
      </main>
      {/* ✅ AI Bubble */}
      <div
        className="ai-bubble"
        onClick={() => navigate("/ai")}
      >
        💬
      </div>
      
      <ConfirmModal
        isOpen={deleteModalOpen}
        title="Delete Transaction?"
        message="Are you sure you want to delete this transaction? This action cannot be undone."
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={handleConfirmDelete}
        onCancel={() => {
          setDeleteModalOpen(false)
          setTransactionToDelete(null)
        }}
        loading={deleteLoading}
      />
    </div>
    
  )
}
