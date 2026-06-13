import api from '../../api/axios'

export const usersApi = {
  getProfile: () => api.get('/api/v1/users/me').then((r) => r.data),
  updateProfile: (data) => api.put('/api/v1/users/me', data).then((r) => r.data),
}
