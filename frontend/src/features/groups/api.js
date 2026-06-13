import api from '../../api/axios'
import { GROUPS } from '../../api/endpoints'

export const groupsApi = {
  sendJoinRequest: (eventId, data) =>
    api.post(GROUPS.JOIN(eventId), data).then((r) => r.data),

  processJoinRequest: (eventId, requestId, data) =>
    api.put(GROUPS.PROCESS_REQUEST(eventId, requestId), data).then((r) => r.data),

  cancelJoinRequest: (eventId, requestId) =>
    api.delete(GROUPS.PROCESS_REQUEST(eventId, requestId)).then((r) => r.data),

  getJoinRequests: (eventId, { status, page = 0, size = 20 } = {}) =>
    api
      .get(GROUPS.JOIN_REQUESTS(eventId), { params: { status: status || undefined, page, size } })
      .then((r) => r.data),

  getEventMembers: (eventId, { page = 0, size = 20 } = {}) =>
    api.get(GROUPS.MEMBERS(eventId), { params: { page, size } }).then((r) => r.data),

  leaveEvent: (eventId) =>
    api.delete(GROUPS.LEAVE(eventId)).then((r) => r.data),
}
