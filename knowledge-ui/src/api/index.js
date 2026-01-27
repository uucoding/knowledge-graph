import request from './request'

/**
 * 聊天相关API
 */
export const chatApi = {
  // 创建会话
  createSession: () => request.post('/chat/session'),

  // 获取会话列表
  listSessions: () => request.get('/chat/session/list'),

  // 删除会话
  deleteSession: (id) => request.delete(`/chat/session/${id}`),

  // 更新会话标题
  updateSessionTitle: (id, title) => request.put(`/chat/session/${id}/title`, null, { params: { title } }),

  // 发送消息（同步）
  sendMessage: (sessionId, data) => request.post(`/chat/${sessionId}/send`, data),

  // 发送消息（流式SSE）
  sendMessageStream: (sessionId, data, onMessage, onError, onComplete) => {
    const controller = new AbortController()

    fetch(`/api/chat/${sessionId}/send/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
      signal: controller.signal
    }).then(async response => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // 解析SSE数据
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const jsonStr = line.slice(5).trim()
            if (jsonStr) {
              try {
                const data = JSON.parse(jsonStr)
                onMessage && onMessage(data)
                if (data.type === 'done') {
                  onComplete && onComplete(data)
                }
              } catch (e) {
                console.warn('解析SSE数据失败:', e, jsonStr)
              }
            }
          }
        }
      }
    }).catch(error => {
      if (error.name !== 'AbortError') {
        onError && onError(error)
      }
    })

    // 返回取消函数
    return () => controller.abort()
  },

  // 获取消息历史
  getMessages: (sessionId) => request.get(`/chat/${sessionId}/messages`),

  // 上传附件
  uploadAttachment: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/chat/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

/**
 * 图谱相关API
 */
export const graphApi = {
  // 获取图谱数据
  getGraph: (limit = 100) => request.get('/graph', { params: { limit } }),

  // 获取子图
  getSubGraph: (nodeId, depth = 2) => request.get(`/graph/subgraph/${nodeId}`, { params: { depth } }),

  // 搜索图谱
  searchGraph: (keyword, limit = 50) => request.get('/graph/search', { params: { keyword, limit } }),

  // 获取路径图
  getPathGraph: (startNodeId, endNodeId) => request.get('/graph/path', { params: { startNodeId, endNodeId } })
}

/**
 * 节点相关API
 */
export const nodeApi = {
  // 获取节点列表
  list: (params) => request.get('/node/list', { params }),

  // 获取节点详情
  detail: (id) => request.get(`/node/${id}`),

  // 创建节点
  create: (data) => request.post('/node', data),

  // 更新节点
  update: (id, data) => request.put(`/node/${id}`, data),

  // 删除节点
  delete: (id) => request.delete(`/node/${id}`),

  // 搜索节点
  search: (name) => request.get('/node/search', { params: { name } }),

  // 获取节点类型列表
  getTypes: () => request.get('/node/types'),

  // 获取节点统计
  statistics: () => request.get('/node/statistics'),

  // 获取相邻节点
  getNeighbors: (id) => request.get(`/node/${id}/neighbors`),

  // 查找最短路径
  findPath: (startId, endId) => request.get('/node/path', { params: { startId, endId } })
}

/**
 * 关系相关API
 */
export const relationApi = {
  // 获取关系列表
  list: (params) => request.get('/relation/list', { params }),

  // 获取关系详情
  detail: (id) => request.get(`/relation/${id}`),

  // 创建关系
  create: (data) => request.post('/relation', data),

  // 更新关系
  update: (id, data) => request.put(`/relation/${id}`, data),

  // 删除关系
  delete: (id) => request.delete(`/relation/${id}`),

  // 获取节点的关系
  getByNodeId: (nodeId) => request.get(`/relation/node/${nodeId}`),

  // 获取关系类型列表
  getTypes: () => request.get('/relation/types'),

  // 检查关系是否存在
  exists: (sourceNodeId, targetNodeId, relationType) =>
    request.get('/relation/exists', { params: { sourceNodeId, targetNodeId, relationType } })
}

/**
 * 文档相关API
 */
export const documentApi = {
  // 上传文档
  upload: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/document/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 获取文档列表
  list: (params) => request.get('/document/list', { params }),

  // 获取文档详情
  detail: (id) => request.get(`/document/${id}`),

  // 删除文档
  delete: (id) => request.delete(`/document/${id}`),

  // 重新解析
  reparse: (id) => request.post(`/document/${id}/reparse`),

  // 生成摘要
  generateSummary: (id) => request.post(`/document/${id}/summary`)
}

/**
 * OCR相关API
 */
export const ocrApi = {
  // 上传图片OCR
  upload: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/ocr/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 获取OCR记录列表
  list: (params) => request.get('/ocr/list', { params }),

  // 获取记录详情
  detail: (id) => request.get(`/ocr/${id}`),

  // 删除记录
  delete: (id) => request.delete(`/ocr/${id}`),

  // 重新识别
  reRecognize: (id) => request.post(`/ocr/${id}/rerecognize`)
}

/**
 * AI相关API
 */
export const aiApi = {
  // AI对话
  chat: (prompt) => request.post('/ai/chat', prompt, {
    headers: { 'Content-Type': 'text/plain' }
  }),

  // 知识抽取
  extract: (data) => request.post('/ai/extract', data),

  // 实体抽取
  extractEntities: (text) => request.post('/ai/extract/entities', text, {
    headers: { 'Content-Type': 'text/plain' }
  }),

  // 生成摘要
  generateSummary: (text) => request.post('/ai/summary', text, {
    headers: { 'Content-Type': 'text/plain' }
  }),

  // 语义搜索
  semanticSearch: (query, topK = 10, type) =>
    request.get('/ai/search', { params: { query, topK, type } })
}
