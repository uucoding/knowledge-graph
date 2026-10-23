export const GRAPH_COLORS = [
  '#165DFF',
  '#00B42A',
  '#FF7D00',
  '#F53F3F',
  '#14C9C9',
  '#F7BA1E',
  '#722ED1',
  '#F5319D',
  '#9FDB1D'
]

export const DOCUMENT_STATUS = {
  0: { tone: 'info', label: '待处理' },
  1: { tone: 'warning', label: '处理中' },
  2: { tone: 'success', label: '已完成' },
  3: { tone: 'danger', label: '处理失败' }
}

export const OCR_STATUS = DOCUMENT_STATUS

export function statusMeta(status) {
  return DOCUMENT_STATUS[status] || DOCUMENT_STATUS[0]
}

export function formatScore(score) {
  return Number.isFinite(Number(score)) ? `${(Number(score) * 100).toFixed(0)}%` : '—'
}
