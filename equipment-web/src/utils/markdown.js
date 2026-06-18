const TABLE_BLOCK_PREFIX = '<div class="table-responsive">'

export function renderMarkdown(text) {
  if (!text) return ''

  const escapedText = escapeHtml(text)
  const parsedLines = parseTables(escapedText)
  const blocks = buildBlocks(parsedLines)

  return blocks.join('')
}

export function sanitizeReportMarkdown(text, options = {}) {
  if (!text) return ''

  const normalizedText = String(text)
    .replace(/\r\n/g, '\n')
    .replace(/\u200b/g, '')

  const lines = normalizedText.split('\n')
  const cleanedLines = []
  const normalizedTitle = normalizeCompareText(options.reportTitle)
  const normalizedTime = normalizeCompareText(options.generatedTime)

  for (let i = 0; i < lines.length; i += 1) {
    const rawLine = lines[i]
    const line = rawLine.replace(/\s+$/g, '')
    const trimmed = line.trim()

    if (shouldDropNoiseLine(trimmed, normalizedTitle, normalizedTime)) {
      continue
    }

    const workflowLines = transformWorkflowBlock(lines, i)
    if (workflowLines) {
      cleanedLines.push(...workflowLines.lines)
      i = workflowLines.nextIndex
      continue
    }

    cleanedLines.push(line)
  }

  return collapseBlankLines(cleanedLines)
}

function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function parseTables(text) {
  const lines = text.split('\n')
  const parsedLines = []
  let tableBuffer = []

  const flushTableBuffer = () => {
    if (!tableBuffer.length) {
      return
    }
    parsedLines.push(buildTableHtmlFromLines(tableBuffer))
    tableBuffer = []
  }

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i]
    const trimmed = line.trim()

    if (isTableCandidate(trimmed)) {
      tableBuffer.push(line)
      continue
    }

    flushTableBuffer()
    parsedLines.push(line)
  }

  flushTableBuffer()

  return parsedLines
}

function buildBlocks(lines) {
  const blocks = []
  let paragraphLines = []
  let listType = ''
  let listItems = []

  const flushParagraph = () => {
    if (!paragraphLines.length) {
      return
    }
    const content = paragraphLines.map(line => renderInlineMarkdown(line.trim())).join('<br/>')
    blocks.push(`<p>${content}</p>`)
    paragraphLines = []
  }

  const flushList = () => {
    if (!listItems.length || !listType) {
      return
    }
    const itemsHtml = listItems.map(item => buildListItemHtml(item)).join('')
    blocks.push(`<${listType}>${itemsHtml}</${listType}>`)
    listItems = []
    listType = ''
  }

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i]
    const trimmed = line.trim()

    if (line.startsWith(TABLE_BLOCK_PREFIX)) {
      flushParagraph()
      flushList()
      blocks.push(line)
      continue
    }

    if (!trimmed) {
      flushParagraph()
      flushList()
      continue
    }

    const headingLevel = getHeadingLevel(trimmed)
    if (headingLevel > 0) {
      flushParagraph()
      flushList()
      const content = renderInlineMarkdown(trimmed.replace(/^#{1,6}\s+/, ''))
      blocks.push(`<h${headingLevel}>${content}</h${headingLevel}>`)
      continue
    }

    if (/^\s*[-*_]{3,}\s*$/.test(trimmed)) {
      flushParagraph()
      flushList()
      blocks.push('<hr/>')
      continue
    }

    if (/^\s*[-*]\s+/.test(line)) {
      flushParagraph()
      if (listType !== 'ul') {
        flushList()
        listType = 'ul'
      }
      listItems.push(parseListItem(line.replace(/^\s*[-*]\s+/, '').trim()))
      continue
    }

    if (/^\s*\d+\.\s+/.test(line)) {
      flushParagraph()
      if (listType !== 'ol') {
        flushList()
        listType = 'ol'
      }
      listItems.push(parseListItem(line.replace(/^\s*\d+\.\s+/, '').trim()))
      continue
    }

    if (listType && listItems.length) {
      const lastItem = listItems[listItems.length - 1]
      lastItem.lines.push(trimmed)
      continue
    }

    flushList()
    paragraphLines.push(line)
  }

  flushParagraph()
  flushList()

  return blocks
}

function getHeadingLevel(line) {
  if (/^######\s+/.test(line)) {
    return 6
  }
  if (/^#####\s+/.test(line)) {
    return 5
  }
  if (/^####\s+/.test(line)) {
    return 4
  }
  if (/^###\s+/.test(line)) {
    return 3
  }
  if (/^##\s+/.test(line)) {
    return 2
  }
  if (/^#\s+/.test(line)) {
    return 1
  }
  return 0
}

function buildTableHtmlFromLines(lines) {
  const rows = []
  let caption = ''

  lines.forEach(line => {
    const trimmed = line.trim()
    if (isTableSeparatorRow(trimmed)) {
      return
    }

    const cells = parseTableRow(trimmed)
    if (!cells.length) {
      return
    }
    rows.push(cells)
  })

  if (!rows.length) {
    return lines.join('\n')
  }

  if (rows.length > 1 && rows[0].length === 1 && rows.slice(1).some(row => row.length > 1)) {
    caption = rows.shift()[0]
  }

  const columnCount = rows.reduce((max, row) => Math.max(max, row.length), 0)
  const normalizedRows = rows.map(row => padTableRow(row, columnCount))
  const useHeader = normalizedRows.length > 1 && normalizedRows.every(row => row.length === columnCount) && columnCount > 1 && !caption

  let html = '<div class="table-responsive">'
  if (caption) {
    html += `<div class="table-caption">${renderInlineMarkdown(caption)}</div>`
  }
  html += '<table class="markdown-table">'

  if (useHeader) {
    const header = normalizedRows.shift()
    html += '<thead><tr>'
    header.forEach(cell => {
      html += `<th>${renderInlineMarkdown(cell)}</th>`
    })
    html += '</tr></thead>'
  }

  html += '<tbody>'
  normalizedRows.forEach(row => {
    html += '<tr>'
    row.forEach(cell => {
      html += `<td>${renderInlineMarkdown(cell)}</td>`
    })
    html += '</tr>'
  })
  html += '</tbody></table></div>'

  return html
}

function isTableCandidate(line) {
  if (!line) return false
  if (isTableSeparatorRow(line)) return true
  if (!line.includes('|')) return false
  const cells = parseTableRow(line)
  return cells.length > 0
}

function isTableSeparatorRow(line) {
  if (!line) return false
  const compact = line.replace(/\s+/g, '')
  return /^\|?[:=-]+(\|[:=-]+)+\|?$/.test(compact) || /^\|?[:=-]+\|?$/.test(compact)
}

function parseTableRow(line) {
  const trimmed = line.trim()
  if (!trimmed.includes('|')) {
    return []
  }

  const rawCells = trimmed.split('|')
  const cells = rawCells
    .map(cell => cell.trim())
    .filter((cell, index, arr) => {
      if (!cell && (index === 0 || index === arr.length - 1)) {
        return false
      }
      return true
    })

  return cells.filter(cell => !isTableSeparatorCell(cell))
}

function isTableSeparatorCell(cell) {
  return /^:?-+:?$/.test(cell) || /^=+$/.test(cell)
}

function padTableRow(row, columnCount) {
  const padded = row.slice()
  while (padded.length < columnCount) {
    padded.push('')
  }
  return padded
}

function parseListItem(text) {
  const taskMatch = text.match(/^\[( |x|X)\]\s+(.*)$/)
  if (taskMatch) {
    return {
      type: 'task',
      checked: taskMatch[1].toLowerCase() === 'x',
      lines: [taskMatch[2]]
    }
  }

  return {
    type: 'plain',
    checked: false,
    lines: [text]
  }
}

function buildListItemHtml(item) {
  const content = item.lines.map(line => renderInlineMarkdown(line)).join('<br/>')
  if (item.type !== 'task') {
    return `<li>${content}</li>`
  }

  return `<li class="task-list-item"><span class="task-checkbox${item.checked ? ' is-checked' : ''}"></span><span class="task-text">${content}</span></li>`
}

function renderInlineMarkdown(text) {
  if (!text) return ''
  const codeTokens = []
  let normalizedText = text.replace(/`([^`]+)`/g, (_, content) => {
    const token = `@@CODE_TOKEN_${codeTokens.length}@@`
    codeTokens.push(`<code>${content}</code>`)
    return token
  })

  normalizedText = normalizedText
    .replace(/(^|\s)`+/g, '$1')
    .replace(/`+(\s|$)/g, '$1')
    .replace(/`/g, '')

  normalizedText = normalizedText
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')

  codeTokens.forEach((tokenValue, index) => {
    normalizedText = normalizedText.replace(`@@CODE_TOKEN_${index}@@`, tokenValue)
  })

  return normalizedText
}

function shouldDropNoiseLine(trimmed, normalizedTitle, normalizedTime) {
  if (!trimmed) {
    return false
  }

  if (/^`+$/.test(trimmed) || /^`{3,}/.test(trimmed)) {
    return true
  }

  if (/^(localhost|https?:\/\/|about:blank)/i.test(trimmed)) {
    return true
  }

  const normalizedLine = normalizeCompareText(trimmed)
  if (normalizedTitle && normalizedLine === normalizedTitle) {
    return true
  }

  if (normalizedTime && (normalizedLine === normalizedTime || normalizedLine.indexOf(normalizedTime) !== -1)) {
    return true
  }

  if (/^(生成时间|创建时间|导出时间)\s*[:：]/.test(trimmed)) {
    return true
  }

  return false
}

function transformWorkflowBlock(lines, index) {
  const currentLine = (lines[index] || '').trim()
  const nextLine = (lines[index + 1] || '').trim()
  const stageMatches = currentLine.match(/\[[^\]]+\]/g)

  if (!stageMatches || stageMatches.length < 2 || currentLine.indexOf('->') === -1 || !nextLine) {
    return null
  }

  const items = nextLine
    .split(/\s{2,}/)
    .map(item => item.trim())
    .filter(Boolean)

  if (items.length !== stageMatches.length) {
    return null
  }

  // Normalize common AI-generated priority chains into stable bullet items.
  return {
    lines: stageMatches.map((stage, stageIndex) => `- **${stage}** ${items[stageIndex]}`),
    nextIndex: index + 1
  }
}

function collapseBlankLines(lines) {
  const normalizedLines = []
  let previousBlank = false

  lines.forEach(line => {
    const isBlank = !line.trim()
    if (isBlank) {
      if (!previousBlank) {
        normalizedLines.push('')
      }
      previousBlank = true
      return
    }

    normalizedLines.push(line)
    previousBlank = false
  })

  return normalizedLines.join('\n').trim()
}

function normalizeCompareText(text) {
  return String(text || '')
    .replace(/\s+/g, '')
    .replace(/[：:]/g, '')
    .trim()
}
