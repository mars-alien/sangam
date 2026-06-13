import api from '../../api/axios'
import { AUTH } from '../../api/endpoints'

export const authApi = {
  login: (data) => api.post(AUTH.LOGIN, data).then((r) => r.data),
  register: (data) => api.post(AUTH.REGISTER, data).then((r) => r.data),
  logout: (refreshToken) => api.post(AUTH.LOGOUT, { refreshToken }).then((r) => r.data),
}
