import './TransactionTable.css'

// ─── Skeleton row ──────────────────────────────────────────────────────────────
function SkeletonRow() {
  return (
    <tr className="skeleton-row">
      {[100, 70, 55, 80, 65, 90].map((w, i) => (
        <td key={i} className="tx-td">
          <span className="skeleton-cell" style={{ width: `${w}%` }} />
        </td>
      ))}
    </tr>
  )
}

// ─── Amount formatter ──────────────────────────────────────────────────────────
function formatAmount(amount, type) {
  const formatted = `₹${Number(amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
  return type === 'INCOME' ? `+${formatted}` : `−${formatted}`
}

// ─── Date formatter ────────────────────────────────────────────────────────────
function formatDate(dateStr) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

// ─── TransactionTable ──────────────────────────────────────────────────────────
export default function TransactionTable({
  transactions,
  loading,
  onEdit,
  onDelete
}) {
  return (
    <div className="tx-table-wrapper">
      <table className="tx-table">
        <thead>
          <tr>
            <th className="tx-th">Date</th>
            <th className="tx-th">Description</th>
            <th className="tx-th">Category</th>
            <th className="tx-th">Type</th>
            <th className="tx-th tx-th-right">Amount</th>
            <th className="tx-th">User</th>
            <th className="tx-th">Actions</th>
          </tr>
        </thead>
        <tbody>
          {loading ? (
            // Show 5 skeleton rows while loading
            Array.from({ length: 5 }).map((_, i) => <SkeletonRow key={i} />)
          ) : transactions.length === 0 ? (
            <tr>
              <td colSpan={6} className="tx-empty">
                <div className="tx-empty-inner">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="2" y="7" width="20" height="14" rx="2" /><path d="M16 7V5a2 2 0 0 0-4 0v2" />
                    <line x1="12" y1="12" x2="12" y2="16" /><line x1="10" y1="14" x2="14" y2="14" />
                  </svg>
                  <span>No transactions found</span>
                </div>
              </td>
            </tr>
          ) : (
            transactions.map((tx) => (
              <tr key={tx.id} className="tx-row">
                <td className="tx-td tx-date">{formatDate(tx.date)}</td>
                <td className="tx-td tx-desc">{tx.description || '—'}</td>
                <td className="tx-td">
                  <span className="tx-category">{tx.categoryName || '—'}</span>
                </td>
                <td className="tx-td">
                  <span className={`tx-badge tx-badge-${tx.type?.toLowerCase()}`}>
                    {tx.type}
                  </span>
                </td>
                <td className={`tx-td tx-amount tx-amount-${tx.type?.toLowerCase()}`}>
                  {formatAmount(tx.amount, tx.type)}
                </td>
                <td className="tx-td tx-user">{tx.userName || '—'}</td>
                <td className="tt-actions">
                <button
                  className="tt-edit"
                  onClick={() => onEdit(tx)}
                >
                  Edit
                </button>

                <button
                  className="tt-delete"
                  onClick={() => onDelete(tx.id)}
                >
                  Delete
                </button>
              </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
