export function renderMarkdown(text) {
  if (!text) return ''

  // 1. 进行基础的 HTML 转义，防止 HTML 注入
  let escapedText = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // 2. 表格语法解析
  const lines = escapedText.split('\n')
  let inTable = false
  let tableHeader = []
  let tableRows = []
  let parsedLines = []

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    const trimmed = line.trim()
    
    // 匹配符合表格特性的行，以 | 开头并以 | 结尾
    if (trimmed.startsWith('|') && trimmed.endsWith('|')) {
      const cells = trimmed.split('|').map(c => c.trim()).slice(1, -1)
      
      if (!inTable) {
        inTable = true
        tableHeader = cells
      } else if (cells.every(c => /^:?-+:?$/.test(c))) {
        // 这是表格的分隔行（如 | :--- | :--- |），忽略
        continue
      } else {
        tableRows.push(cells)
      }
    } else {
      if (inTable) {
        parsedLines.push(buildTableHtml(tableHeader, tableRows))
        inTable = false
        tableHeader = []
        tableRows = []
      }
      parsedLines.push(line)
    }
  }
  
  if (inTable) {
    parsedLines.push(buildTableHtml(tableHeader, tableRows))
  }

  let html = parsedLines.join('\n')

  // 3. 其它 Markdown 语法渲染
  html = html
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/^\s*[-*_]{3,}\s*$/gim, '<hr/>') // 渲染 ---, ***, ___ 为分割线
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/^\s*[-*]\s+(.*$)/gim, '<li>$1</li>') // 支持 - 开头或 * 开头的列表项
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br/>')

  // 合并连续的 <li> 为一个 <ul> 标签包裹
  html = html.replace(/(<li>.*?<\/li>)+/g, '<ul>$&</ul>')

  // 4. 精细清洗：移除表格与列表等块级元素内部因 \n 转义意外引入的多余 <br/>，保持排版工整
  html = html
    .replace(/(<table[^>]*>[\s\S]*?<\/table>)/gi, function(match) {
      return match.replace(/<br\s*\/?>/gi, '')
    })
    .replace(/<\/ul><br\s*\/?>/gi, '</ul>')
    .replace(/<\/hr><br\s*\/?>/gi, '<hr/>')
    .replace(/<hr\s*\/?><br\s*\/?>/gi, '<hr/>')
    .replace(/<br\s*\/?><li>/gi, '<li>')
    .replace(/<\/li><br\s*\/?>/gi, '</li>')

  return html
}

function buildTableHtml(headers, rows) {
  let html = '<div class="table-responsive"><table class="markdown-table">'
  html += '<thead><tr>'
  headers.forEach(h => {
    const renderedH = renderInlineMarkdown(h)
    html += `<th>${renderedH}</th>`
  })
  html += '</tr></thead>'
  html += '<tbody>'
  rows.forEach(row => {
    html += '<tr>'
    row.forEach(cell => {
      const renderedCell = renderInlineMarkdown(cell)
      html += `<td>${renderedCell}</td>`
    })
    html += '</tr>'
  })
  html += '</tbody></table></div>'
  return html
}

function renderInlineMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
}
