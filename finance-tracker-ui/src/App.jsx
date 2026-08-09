import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Register from './pages/Register'
import Profile from './pages/Profile'
import Analytics from './pages/Analytics'
import AiAssistantPage from "./pages/AiAssistantPage"
import { AIChatProvider } from './context/AIChatContext'
import AIChatLauncher from './components/AI/AIChatLauncher'
import api from './api'
import { ToastProvider } from './context/ToastContext'

// ─── Protected Route Guard ────────────────────────────────────────────────────
function ProtectedRoute({ children }) {
  const [isValidating, setIsValidating] = useState(true)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const token = localStorage.getItem('token')

  useEffect(() => {
    if (!token) {
      setIsAuthenticated(false)
      setIsValidating(false)
      return
    }

    api.get('/users/me')
      .then(() => {
        setIsAuthenticated(true)
        setIsValidating(false)
      })
      .catch((err) => {
        console.error("Token verification failed:", err)
        localStorage.removeItem('token')
        localStorage.removeItem('activeChatSessionId')
        setIsAuthenticated(false)
        setIsValidating(false)
      })
  }, [token])

  if (isValidating) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        background: 'radial-gradient(circle at top, #0b1220, #06080f 70%)',
        color: '#fff',
        fontFamily: 'sans-serif'
      }}>
        <div style={{ textAlign: 'center' }}>
          <div className="spinner" style={{
            border: '4px solid rgba(255,255,255,0.1)',
            width: '36px',
            height: '36px',
            borderRadius: '50%',
            borderLeftColor: '#00e5a0',
            animation: 'spin 1s linear infinite',
            margin: '0 auto 16px'
          }} />
          <style>{`
            @keyframes spin {
              0% { transform: rotate(0deg); }
              100% { transform: rotate(360deg); }
            }
          `}</style>
          Loading authentication...
        </div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return children
}

// ─── App ──────────────────────────────────────────────────────────────────────
export default function App() {
  return (
    <ToastProvider>
      <AIChatProvider>
        <Routes>
          <Route
            path="/ai"
            element={
              <ProtectedRoute>
                <AiAssistantPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/analytics"
            element={
              <ProtectedRoute>
                <Analytics />
              </ProtectedRoute>
            }
          />
          <Route path="/register" element={<Register />} />
          <Route path="/login" element={<Login />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <Profile />
              </ProtectedRoute>
            }
          />
          {/* Default redirect */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        <AIChatLauncher />
      </AIChatProvider>
    </ToastProvider>
  )
}
