const { request } = require('../utils/request')

module.exports = {
  page: (params) => request({ url: '/api/wx/notifications', data: params }),
  unreadCount: () => request({ url: '/api/wx/notifications/unread-count' }),
  markRead: (id) => request({ url: `/api/wx/notifications/${id}/read`, method: 'POST' }),
  markAllRead: () => request({ url: '/api/wx/notifications/read-all', method: 'POST' }),
}
