import { renderMarkdown, sanitizeReportMarkdown } from '@/utils/markdown'

export function formatReportTime(timeValue) {
  if (!timeValue) return ''

  if (Array.isArray(timeValue)) {
    const y = timeValue[0]
    const m = String(timeValue[1]).padStart(2, '0')
    const d = String(timeValue[2]).padStart(2, '0')
    const hh = String(timeValue[3]).padStart(2, '0')
    const mm = String(timeValue[4]).padStart(2, '0')
    return `${y}-${m}-${d} ${hh}:${mm}`
  }

  return String(timeValue).replace('T', ' ').substring(0, 16)
}

export function buildAiReportHtml(options) {
  const cleanedMarkdown = sanitizeReportMarkdown(options.content, {
    reportTitle: options.title,
    generatedTime: options.generatedTime
  })

  return renderMarkdown(cleanedMarkdown)
}

export function printAiReportFromFrame(printFrame, options) {
  if (!printFrame || !printFrame.contentWindow) {
    throw new Error('打印容器初始化失败，请刷新页面后重试')
  }

  const renderedHtml = buildAiReportHtml({
    content: options.content,
    title: options.title,
    generatedTime: options.generatedTime
  })
  const cleanedContent = buildPrintContentHtml({
    title: options.title,
    renderedHtml
  })
  const printDocument = buildPrintDocument({
    title: options.title,
    badge: options.badge,
    metaItems: options.metaItems,
    cleanedContent
  })
  const frameWindow = printFrame.contentWindow
  const frameDocument = frameWindow.document

  frameDocument.open()
  frameDocument.write(printDocument)
  frameDocument.close()

  setTimeout(() => {
    frameWindow.focus()
    frameWindow.print()
  }, 180)
}

function buildPrintContentHtml(options) {
  const parser = new window.DOMParser()
  const doc = parser.parseFromString(`<div>${options.renderedHtml || ''}</div>`, 'text/html')
  const root = doc.body.firstElementChild

  if (!root) {
    return ''
  }

  const firstHeading = root.querySelector('h1')
  if (firstHeading) {
    const headingText = normalizeText(firstHeading.textContent)
    const reportTitleText = normalizeText(options.title || '')
    if (headingText && reportTitleText && headingText.indexOf(reportTitleText) !== -1) {
      firstHeading.remove()
    }
  }

  root.querySelectorAll('p, li').forEach(node => {
    if (!normalizeText(node.textContent)) {
      node.remove()
    }
  })

  root.querySelectorAll('br').forEach(node => {
    if (!node.previousSibling && !node.nextSibling) {
      node.remove()
    }
  })

  return root.innerHTML
}

function buildPrintDocument(options) {
  const metaItems = (options.metaItems || [])
    .filter(item => item && item.value)
    .map(item => `<span>${escapePrintHtml(item.label)}：${escapePrintHtml(item.value)}</span>`)
    .join('')

  return `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>${escapePrintHtml(options.title)}</title>
  <style>
    @page { size: A4 portrait; margin: 15mm 14mm 16mm; }
    * { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; background: #ffffff; color: #111827; font-family: "Microsoft YaHei", "PingFang SC", sans-serif; }
    .print-report { width: 100%; }
    .print-header { border-bottom: 1px solid #dbe2ea; padding-bottom: 14px; margin-bottom: 18px; page-break-after: avoid; }
    .print-title { margin: 0 0 10px; font-size: 24px; line-height: 1.25; font-weight: 700; color: #0f172a; }
    .print-meta { display: flex; flex-wrap: wrap; gap: 10px 18px; align-items: center; font-size: 12px; color: #475569; }
    .print-badge { display: inline-block; padding: 3px 10px; border-radius: 999px; background: #eff6ff; color: #2563eb; font-weight: 600; }
    .print-content { font-size: 13.5px; line-height: 1.78; color: #1f2937; }
    .print-content h1 { margin: 0 0 18px; padding-bottom: 10px; border-bottom: 2px solid #e2e8f0; font-size: 21px; color: #0f172a; page-break-after: avoid; page-break-inside: avoid; }
    .print-content h2 { margin: 26px 0 12px; font-size: 18px; color: #111827; page-break-after: avoid; page-break-inside: avoid; }
    .print-content h3 { margin: 20px 0 10px; font-size: 16px; color: #1f2937; page-break-after: avoid; page-break-inside: avoid; }
    .print-content h4 { margin: 18px 0 10px; font-size: 15px; color: #1f2937; page-break-after: avoid; page-break-inside: avoid; }
    .print-content h5 { margin: 16px 0 8px; font-size: 14px; color: #334155; page-break-after: avoid; page-break-inside: avoid; }
    .print-content h6 { margin: 14px 0 8px; font-size: 13px; color: #475569; page-break-after: avoid; page-break-inside: avoid; }
    .print-content .table-caption {
      padding: 10px 12px;
      background: #f8fafc;
      border-bottom: 1px solid #e2e8f0;
      color: #0f172a;
      font-weight: 700;
      font-size: 13px;
    }
    .print-content p { margin: 0 0 14px; orphans: 3; widows: 3; }
    .print-content ul, .print-content ol { margin: 10px 0 16px; padding-left: 24px; page-break-inside: avoid; }
    .print-content li { margin-bottom: 8px; page-break-inside: avoid; }
    .print-content .task-list-item { list-style: none; display: flex; align-items: flex-start; gap: 10px; margin-left: -22px; }
    .print-content .task-checkbox { width: 14px; height: 14px; margin-top: 5px; border: 1.5px solid #cbd5e1; border-radius: 4px; background: #fff; flex: 0 0 auto; }
    .print-content .task-checkbox.is-checked { background: #2563eb; border-color: #2563eb; position: relative; }
    .print-content .task-checkbox.is-checked::after { content: ""; position: absolute; left: 3px; top: 0px; width: 4px; height: 8px; border: solid #fff; border-width: 0 2px 2px 0; transform: rotate(45deg); }
    .print-content .task-text { flex: 1; }
    .print-content strong { color: #111827; font-weight: 700; }
    .print-content code { padding: 1px 6px; border: 1px solid #e2e8f0; border-radius: 4px; background: #f8fafc; font-size: 12px; font-family: Consolas, monospace; }
    .print-content hr { height: 1px; border: none; background: #e2e8f0; margin: 22px 0; page-break-after: avoid; }
    .print-content .table-responsive { overflow: visible; margin: 18px 0; border: 1px solid #e2e8f0; border-radius: 8px; page-break-inside: avoid; }
    .print-content .table-caption {
      padding: 10px 12px;
      background: #f8fafc;
      border-bottom: 1px solid #e2e8f0;
      color: #0f172a;
      font-weight: 700;
      font-size: 13px;
    }
    .print-content .markdown-table { width: 100%; table-layout: fixed; border-collapse: collapse; font-size: 13px; }
    .print-content .markdown-table thead { display: table-header-group; }
    .print-content .markdown-table tr { page-break-inside: avoid; }
    .print-content .markdown-table th { background: #f8fafc; color: #334155; font-weight: 700; padding: 10px 12px; border-bottom: 2px solid #e2e8f0; text-align: left; }
    .print-content .markdown-table td { padding: 10px 12px; border-bottom: 1px solid #eef2f7; vertical-align: top; word-break: break-word; }
    .print-content .markdown-table tr:last-child td { border-bottom: none; }
    .print-content blockquote { margin: 16px 0; padding: 10px 14px; border-left: 3px solid #94a3b8; background: #f8fafc; color: #334155; page-break-inside: avoid; }
  </style>
</head>
<body>
  <article class="print-report">
    <header class="print-header">
      <h1 class="print-title">${escapePrintHtml(options.title)}</h1>
      <div class="print-meta">
        ${options.badge ? `<span class="print-badge">${escapePrintHtml(options.badge)}</span>` : ''}
        ${metaItems}
      </div>
    </header>
    <section class="print-content">${options.cleanedContent || ''}</section>
  </article>
</body>
</html>`
}

function normalizeText(text) {
  return String(text || '').replace(/\s+/g, '').trim()
}

function escapePrintHtml(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}
