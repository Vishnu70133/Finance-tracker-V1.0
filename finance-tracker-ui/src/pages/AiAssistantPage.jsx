import { useState, useRef, useEffect } from "react"
import { useAIChat } from "../context/AIChatContext"
import { useNavigate } from "react-router-dom"
import "./AiAssistantPage.css"
import Navbar from "../components/Navbar"
import { useToast } from "../context/ToastContext"
import ConfirmModal from "../components/ConfirmModal"

export default function AiAssistantPage() {
  const navigate = useNavigate()
  const {
    messages,
    sessions,
    currentSession,
    loading,
    profileName,
    loadMessages,
    createChat,
    sendMessage,
    deleteChat
  } = useAIChat()

  const [input, setInput] = useState("")
  const [sidebarOpen, setSidebarOpen] = useState(
    localStorage.getItem('sidebar_open') !== 'false'
  )
  const [mobileOpen, setMobileOpen] = useState(false)
  const chatEndRef = useRef(null)

  // Custom confirmation modal and toast states
  const [deleteModalOpen, setDeleteModalOpen] = useState(false)
  const [sessionToDelete, setSessionToDelete] = useState(null)
  const [deleteLoading, setDeleteLoading] = useState(false)
  const { showToast } = useToast()

  // Scroll to bottom on message updates
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  const toggleSidebar = () => {
    const newState = !sidebarOpen
    setSidebarOpen(newState)
    localStorage.setItem('sidebar_open', String(newState))
  }

  const handleSend = async (e) => {
    if (e) e.preventDefault()
    const text = input.trim()
    if (!text || loading) return
    setInput("")
    await sendMessage(text)
  }

  const handleKey = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleDeleteSession = (e, id) => {
    e.stopPropagation()
    setSessionToDelete(id)
    setDeleteModalOpen(true)
  }

  const handleConfirmDelete = async () => {
    if (!sessionToDelete || deleteLoading) return
    setDeleteLoading(true)
    try {
      await deleteChat(sessionToDelete)
      showToast('Conversation deleted successfully.', 'success')
    } catch (err) {
      console.error(err)
      showToast('Failed to delete conversation.', 'error')
    } finally {
      setDeleteLoading(false)
      setDeleteModalOpen(false)
      setSessionToDelete(null)
    }
  }

  // Parse and group sessions by Date relative to today
  const groupSessions = (sessionList) => {
    const today = []
    const yesterday = []
    const last7Days = []
    const older = []

    const now = new Date()
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const startOfYesterday = new Date(startOfToday)
    startOfYesterday.setDate(startOfYesterday.getDate() - 1)
    const startOf7DaysAgo = new Date(startOfToday)
    startOf7DaysAgo.setDate(startOf7DaysAgo.getDate() - 7)

    sessionList.forEach(s => {
      const dateStr = s.updatedAt || s.createdAt
      if (!dateStr) {
        older.push(s)
        return
      }
      const d = new Date(dateStr)
      if (d >= startOfToday) {
        today.push(s)
      } else if (d >= startOfYesterday) {
        yesterday.push(s)
      } else if (d >= startOf7DaysAgo) {
        last7Days.push(s)
      } else {
        older.push(s)
      }
    })

    return { today, yesterday, last7Days, older }
  }

  const grouped = groupSessions(sessions)
  const activeSessionTitle = sessions.find(s => s.id === currentSession)?.title || "New Chat"

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

  const suggestions = [
    "What is my name?",
    "How much did I spend this month?",
    "Analyze my spending",
    "What's my highest spending category?"
  ]

  const renderSessionItem = (s) => (
    <div
      key={s.id}
      className={`chat-item-card ${s.id === currentSession ? "active" : ""}`}
      onClick={() => {
        loadMessages(s.id)
        setMobileOpen(false) // Close mobile drawer
      }}
    >
      <span className="chat-item-icon">💬</span>
      <span className="chat-item-title-text">{s.title || "New Chat"}</span>
      <button
        className="chat-item-delete-btn"
        onClick={(e) => handleDeleteSession(e, s.id)}
        title="Delete conversation"
      >
        🗑
      </button>
    </div>
  )

  const renderSidebarContent = () => (
    <>
      <div className="sidebar-brand-section">
        <span className="sidebar-spark">✨</span>
        <span className="sidebar-brand-title">FinTrack AI</span>
      </div>

      <button className="sidebar-new-chat-btn" onClick={() => {
        createChat()
        setMobileOpen(false)
      }}>
        ＋ New Chat
      </button>

      <div className="sidebar-scroll-area">
        {grouped.today.length > 0 && (
          <div className="session-group">
            <h4 className="group-label">Today</h4>
            {grouped.today.map(renderSessionItem)}
          </div>
        )}

        {grouped.yesterday.length > 0 && (
          <div className="session-group">
            <h4 className="group-label">Yesterday</h4>
            {grouped.yesterday.map(renderSessionItem)}
          </div>
        )}

        {grouped.last7Days.length > 0 && (
          <div className="session-group">
            <h4 className="group-label">Previous 7 Days</h4>
            {grouped.last7Days.map(renderSessionItem)}
          </div>
        )}

        {grouped.older.length > 0 && (
          <div className="session-group">
            <h4 className="group-label">Older</h4>
            {grouped.older.map(renderSessionItem)}
          </div>
        )}
      </div>

      {/* User profile section */}
      <div className="sidebar-user-section" onClick={() => navigate('/profile')}>
        <div className="user-profile-avatar">👤</div>
        <div className="user-profile-info">
          <div className="user-profile-name">{profileName || "User"}</div>
          <div className="user-profile-link">Profile Details</div>
        </div>
      </div>
    </>
  )

  return (
    <>
      <Navbar userEmail={userEmail} onLogout={handleLogout} />

      <div className="ai-layout">
        {/* -------- DESKTOP SIDEBAR -------- */}
        <div className={`ai-desktop-sidebar ${sidebarOpen ? "expanded" : "collapsed"}`}>
          {sidebarOpen ? (
            renderSidebarContent()
          ) : (
            <div className="collapsed-sidebar-icons">
              <button className="collapsed-action-btn" onClick={() => createChat()} title="New Chat">
                ＋
              </button>
              <button className="collapsed-action-btn" onClick={toggleSidebar} title="Expand sidebar">
                👉
              </button>
              <button className="collapsed-action-btn" onClick={() => navigate('/profile')} title="Profile">
                👤
              </button>
            </div>
          )}
        </div>

        {/* -------- MOBILE DRAWER SIDEBAR -------- */}
        {mobileOpen && (
          <div className="ai-mobile-overlay" onClick={() => setMobileOpen(false)}>
            <div className="ai-mobile-drawer" onClick={(e) => e.stopPropagation()}>
              {renderSidebarContent()}
            </div>
          </div>
        )}

        {/* -------- MAIN CHAT PAGE AREA -------- */}
        <div className="ai-page">
          {/* Header */}
          <div className="ai-chat-header">
            <button className="header-toggle-btn" onClick={toggleSidebar} title="Toggle sidebar">
              ☰
            </button>
            <button className="header-mobile-toggle-btn" onClick={() => setMobileOpen(true)} title="Show menu">
              ☰
            </button>
            <div className="header-title-container">
              <span className="header-title-main">FinTrack AI</span>
              <span className="header-title-divider">/</span>
              <span className="header-title-sub">{activeSessionTitle}</span>
            </div>
          </div>

          {/* Messages Container */}
          <div className="ai-container">
            <div className="ai-chat">
              {messages.map((m, i) => (
                <div key={i} className={`ai-chat-msg-bubble-wrapper ${m.role === "user" ? "user" : "ai"}`}>
                  <div className={`ai-chat-msg-bubble ${m.role === "user" ? "user" : "ai"}`}>
                    {m.text}
                  </div>
                </div>
              ))}

              {messages.length === 1 && (
                <div className="ai-suggestions-container">
                  <h4 className="suggestions-prompt-label">Try asking:</h4>
                  <div className="ai-suggestions">
                    {suggestions.map((s, i) => (
                      <button key={i} onClick={() => sendMessage(s)} className="suggestion-btn">
                        {s}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {loading && (
                <div className="ai-chat-msg-bubble-wrapper ai">
                  <div className="ai-chat-msg-bubble ai thinking">
                    Thinking...
                  </div>
                </div>
              )}

              <div ref={chatEndRef}></div>
            </div>

            {/* Input Form */}
            <div className="ai-input-container">
              <form className="ai-input-box" onSubmit={handleSend}>
                <textarea
                  className="ai-input"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKey}
                  placeholder="Ask FinTrack Assistant..."
                  rows={1}
                />
                <button
                  className="ai-send-btn"
                  type="submit"
                  disabled={loading || !input.trim()}
                >
                  ➤
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
      
      <ConfirmModal
        isOpen={deleteModalOpen}
        title="Delete Conversation?"
        message="Are you sure you want to delete this conversation? This action cannot be undone."
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={handleConfirmDelete}
        onCancel={() => {
          setDeleteModalOpen(false)
          setSessionToDelete(null)
        }}
        loading={deleteLoading}
      />
    </>
  )
}