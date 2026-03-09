import axios from 'axios'

// ─── Axios Instance ───────────────────────────────────────────────────────────
const api = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

// ─── Request Interceptor: Attach JWT Token ────────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ─── Response Interceptor: Handle 401 (token expired / invalid) ───────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {

    const status = error.response?.status

    console.log("API ERROR STATUS:", status)
    console.log("API ERROR URL:", error.config?.url)
    console.log("API ERROR DATA:", error.response?.data)

    // Token expired or invalid
    if (status === 401 || status === 403) {

      console.warn("Authentication expired. Redirecting to login...")

      localStorage.removeItem("token")

      window.location.href = "/login"
    }

    return Promise.reject(error)
  }
)

export default api
