import axios from 'axios'

// ─── Axios Instance ───────────────────────────────────────────────────────────
const api = axios.create({
  baseURL: 'http://localhost:8089',
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

let isRedirectingToLogin = false

// ─── Response Interceptor: Handle 401 & 403 (token expired / invalid) ───────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {

    const status = error.response?.status

    console.log("API ERROR STATUS:", status)
    console.log("API ERROR URL:", error.config?.url)
    console.log("API ERROR DATA:", error.response?.data)

    // Token expired or invalid
    if ((status === 401 || status === 403) && window.location.pathname !== "/login") {
      if (!isRedirectingToLogin) {
        isRedirectingToLogin = true
        console.warn("Authentication expired/invalid. Redirecting to login...")

        localStorage.removeItem("token")
        localStorage.removeItem("activeChatSessionId")

        window.location.href = "/login"
      }
    }

    return Promise.reject(error)
  }
)

export default api
