import api from '../../api/axios'
import { EVENTS } from '../../api/endpoints'

export const eventsApi = {
  getNearbyEvents: ({ lat, lng, radiusKm, category, page = 0, size = 12 }) =>
    api
      .get(EVENTS.LIST, { params: { lat, lng, radiusKm, category: category || undefined, page, size } })
      .then((r) => r.data),

  searchEvents: ({ q, page = 0, size = 12 }) =>
    api.get(EVENTS.SEARCH, { params: { q, page, size } }).then((r) => r.data),

  getEvent: (id) => api.get(EVENTS.DETAIL(id)).then((r) => r.data),

  createEvent: (data) => api.post(EVENTS.CREATE, data).then((r) => r.data),

  updateEvent: (id, data) => api.put(EVENTS.UPDATE(id), data).then((r) => r.data),

  deleteEvent: (id) => api.delete(EVENTS.DELETE(id)).then((r) => r.data),

  getMyEvents: ({ page = 0, size = 12 } = {}) =>
    api.get(EVENTS.MY, { params: { page, size } }).then((r) => r.data),
}
