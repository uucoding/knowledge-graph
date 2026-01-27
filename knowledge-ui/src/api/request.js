import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 创建axios实例
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

/**
 * 请求拦截器
 */
request.interceptors.request.use(
  config => {
    // 可以在这里添加token等认证信息
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器
 */
request.interceptors.response.use(
  response => {
    const res = response.data

    // 根据后端返回的code判断请求是否成功
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res
  },
  error => {
    console.error('响应错误:', error)
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
