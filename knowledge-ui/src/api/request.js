import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

function friendlyMessage(error) {
  const status = error?.response?.status
  if (status === 404) return '接口尚未接入（404）'
  if (status === 401 || status === 403) return '没有权限访问该资源'
  if (status >= 500) return `本地服务出错（${status}）`
  if (status) return `本地服务返回异常（${status}）`
  if (error?.code === 'ECONNABORTED') return '请求超时，请检查本地服务'
  return '无法连接本地后端，请确认服务已启动'
}

function notify(message) {
  ElMessage({
    message,
    type: 'error',
    grouping: true,
    duration: 4000,
    showClose: true
  })
}

function rejectWithMessage(message, source) {
  const error = new Error(message)
  error.friendlyMessage = message
  if (source) error.response = source.response
  return Promise.reject(error)
}

request.interceptors.request.use(
  config => config,
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => {
    const res = response.data
    const failedByCode = res && typeof res === 'object' && 'code' in res && Number(res.code) !== 200

    if (failedByCode) {
      const message = res.message || '请求未成功'
      if (response.config?.method?.toLowerCase() !== 'get') notify(message)
      return rejectWithMessage(message, response)
    }

    return res
  },
  error => {
    const message = friendlyMessage(error)
    error.friendlyMessage = message
    if (error.config?.method?.toLowerCase() !== 'get') notify(message)
    return Promise.reject(error)
  }
)

export default request
