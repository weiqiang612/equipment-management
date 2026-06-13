/**
 * 极简自定义 Markdown 渲染工具方法
 * 仅用于本系统的 AI 输出摘要展示，防止引入体积过大的第三方 marked 依赖
 */
export function renderMarkdown(text) {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/^\s*-\s+(.*$)/gim, '<li>$1</li>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br/>')
  html = html.replace(/(<li>.*?<\/li>)+/g, '<ul>$&</ul>')
  return html
}
