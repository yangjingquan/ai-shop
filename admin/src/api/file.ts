import request from '@/utils/request'

export type UploadScope = 'admin' | 'merchant'

export const fileApi = {
  upload: (scope: UploadScope, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<unknown, { url: string }>(`/api/${scope}/file/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    })
  },
  uploadBatch: (scope: UploadScope, files: File[]) => {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))
    return request.post<unknown, string[]>(`/api/${scope}/file/upload/batch`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    })
  },
  remove: (scope: UploadScope, url: string) =>
    request.delete<unknown, void>(`/api/${scope}/file/delete`, { params: { url } }),
}
