import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import api from '../api'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ResponsiveContainer
} from 'recharts'
import './Analytics.css'

export default function Analytics() {
    const navigate = useNavigate()

  const [data,setData] = useState([])

  useEffect(()=>{
    api.get('/transactions/my/monthly-chart')
       .then(res => setData(res.data))
  },[])
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
    <div className="analytics-root">

      <h2 className="analytics-title">
        Monthly Income vs Expense
      </h2>
              <button className="profile-back-btn" onClick={() => navigate('/dashboard')}>
            ← Back
        </button>

      <div className="analytics-card">

        <ResponsiveContainer width="100%" height={320}>
          <LineChart data={data}>

            <CartesianGrid strokeDasharray="3 3" opacity={0.2} />

            <XAxis dataKey="month"/>
            <YAxis/>

            <Tooltip/>

            {/* 🟢 INCOME LINE */}
            <Line
              type="monotone"
              dataKey="income"
              stroke="#00e5a0"
              strokeWidth={3}
              dot={{ r:4 }}
            />

            {/* 🔴 EXPENSE LINE */}
            <Line
              type="monotone"
              dataKey="expense"
              stroke="#ff5c6a"
              strokeWidth={3}
              dot={{ r:4 }}
            />

          </LineChart>
        </ResponsiveContainer>

      </div>
    </div>
    </>
  )
}