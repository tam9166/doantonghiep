export function printElement(elementId, title = 'Mộc Vị Restaurant') {
  const source = document.getElementById(elementId)
  if (!source) throw new Error(`Không tìm thấy nội dung in: ${elementId}`)

  const frame = document.createElement('iframe')
  frame.setAttribute('title', 'Khung in hóa đơn')
  frame.setAttribute('aria-hidden', 'true')
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
  printDocument.write(`<!doctype html><html><head><meta charset="utf-8"><title></title>${styles}<style>
    @page{size:A4 portrait;margin:10mm}
    html,body{width:auto!important;min-height:0!important;height:auto!important;margin:0!important;background:#fff!important;overflow:visible!important}
    body>*{display:none!important}
    #print-root{display:block!important;position:static!important;width:100%!important;min-height:0!important;height:auto!important;overflow:visible!important}
    #print-root>*{position:static!important;inset:auto!important;width:100%!important;max-width:none!important;min-height:0!important;max-height:none!important;margin:0!important;padding:0!important;overflow:visible!important;box-shadow:none!important;border:0!important;border-radius:0!important;transform:none!important}
    #print-root .hide-on-print{display:none!important}
    #print-root .invoice-content{padding:10px!important}
    #print-root .invoice-brand{padding-bottom:8px!important;margin-bottom:10px!important}
    #print-root .invoice-brand h1{font-size:20px!important}
    #print-root .brand-address{font-size:10px!important;margin-top:5px!important}
    #print-root .invoice-meta{margin-bottom:10px!important}
    #print-root .invoice-meta p{margin:2px 0!important;font-size:11px!important}
    #print-root table{break-inside:auto!important}
    #print-root tr,#print-root .invoice-total,#print-root .invoice-footer{break-inside:avoid!important;page-break-inside:avoid!important}
    #print-root th{background:#4b2c20!important;color:#fff!important;-webkit-print-color-adjust:exact!important;print-color-adjust:exact!important}
    #print-root th,#print-root td{padding:5px 6px!important;font-size:11px!important}
    #print-root .invoice-thumb{width:28px!important;height:28px!important}
    #print-root .invoice-total{margin-top:8px!important}
    #print-root .invoice-footer{margin-top:10px!important;padding-top:7px!important}
  </style></head><body><main id="print-root"></main></body></html>`)
  printDocument.close()
  printDocument.title = title
  printDocument.getElementById('print-root').appendChild(source.cloneNode(true))

  const cleanup = () => setTimeout(() => frame.remove(), 0)
  frame.contentWindow.addEventListener('afterprint', cleanup, { once: true })
  frame.contentWindow.focus()
  frame.contentWindow.print()
  setTimeout(cleanup, 30000)
}
