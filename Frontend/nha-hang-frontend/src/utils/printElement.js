export function printElement(elementId, title = 'Mộc Vị Restaurant') {
  const source = document.getElementById(elementId)
  if (!source) throw new Error(`Không tìm thấy nội dung in: ${elementId}`)

  const frame = document.createElement('iframe')
  frame.setAttribute('title', 'Khung in hóa đơn')
  frame.style.position = 'fixed'
  frame.style.width = '0'
  frame.style.height = '0'
  frame.style.border = '0'
  document.body.appendChild(frame)

  const printDocument = frame.contentDocument
  const styles = [...document.querySelectorAll('link[rel="stylesheet"], style')]
    .map(node => node.outerHTML)
    .join('\n')
  printDocument.open()
  printDocument.write(`<!doctype html><html><head><meta charset="utf-8"><title></title>${styles}<style>@page{margin:12mm}body{margin:0;background:#fff}.hide-on-print{display:none!important}</style></head><body></body></html>`)
  printDocument.close()
  printDocument.title = title
  printDocument.body.appendChild(source.cloneNode(true))

  const cleanup = () => setTimeout(() => frame.remove(), 0)
  frame.contentWindow.addEventListener('afterprint', cleanup, { once: true })
  frame.contentWindow.focus()
  frame.contentWindow.print()
  setTimeout(cleanup, 30000)
}
