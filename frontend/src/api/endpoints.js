export const AUTH = {
  LOGIN: '/api/v1/auth/login',
  REGISTER: '/api/v1/auth/register',
  REFRESH: '/api/v1/auth/refresh',
  LOGOUT: '/api/v1/auth/logout',
}

export const EVENTS = {
  LIST: '/api/v1/events',
  SEARCH: '/api/v1/events/search',
  MY: '/api/v1/events/my',
  DETAIL: (id) => `/api/v1/events/${id}`,
  CREATE: '/api/v1/events',
  UPDATE: (id) => `/api/v1/events/${id}`,
  DELETE: (id) => `/api/v1/events/${id}`,
}

export const GROUPS = {
  JOIN: (eventId) => `/api/v1/events/${eventId}/join`,
  JOIN_REQUESTS: (eventId) => `/api/v1/events/${eventId}/join-requests`,
  PROCESS_REQUEST: (eventId, requestId) => `/api/v1/events/${eventId}/join-requests/${requestId}`,
  MEMBERS: (eventId) => `/api/v1/events/${eventId}/members`,
  LEAVE: (eventId) => `/api/v1/events/${eventId}/members/me`,
}
