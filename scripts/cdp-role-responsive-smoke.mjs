import { spawn } from 'node:child_process'
import { mkdirSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'

const baseUrl = process.env.SMOKE_BASE_URL || 'http://localhost:18080'
const edgePath = process.env.EDGE_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const outputDir = resolve(process.env.SMOKE_OUTPUT_DIR || join(tmpdir(), 'codex-role-responsive'))
const profileDir = join(tmpdir(), `codex-cdp-profile-${process.pid}`)
const debuggingPort = Number(process.env.CDP_PORT || 19223)

const accounts = {
  customer: { endpoint: '/api/auth/login', username: 'customer', route: '/profile' },
  waiter: { endpoint: '/api/auth/staff/login', username: 'waiter', route: '/waiter' },
  kitchen: { endpoint: '/api/auth/staff/login', username: 'kitchen', route: '/kitchen' },
  cashier: { endpoint: '/api/auth/staff/login', username: 'cashier', route: '/cashier' },
  manager: { endpoint: '/api/auth/staff/login', username: 'manager', route: '/admin/analytics' },
  admin: { endpoint: '/api/auth/staff/login', username: 'admin', route: '/admin/analytics' },
}

const protectedScreens = [
  { role: 'admin', route: '/reservation', name: 'public-reservation' },
  { role: 'admin', route: '/admin/analytics', name: 'admin-dashboard' },
  { role: 'manager', route: '/admin/analytics', name: 'manager-dashboard' },
  { role: 'admin', route: '/admin/ai-knowledge', name: 'ai-knowledge' },
  { role: 'admin', route: '/admin', name: 'products' },
  { role: 'kitchen', route: '/kitchen', name: 'kitchen' },
  { role: 'waiter', route: '/waiter', name: 'waiter-table-map' },
  { role: 'cashier', route: '/cashier', name: 'cashier' },
  { role: 'admin', route: '/admin/staff', name: 'staff-management' },
  { role: 'admin', route: '/admin/orders', name: 'orders' },
  { role: 'admin', route: '/admin/reservations', name: 'reservations' },
  { role: 'admin', route: '/admin/deposit-policies', name: 'deposit-policies' },
  { role: 'admin', route: '/admin/tables', name: 'tables' },
  { role: 'admin', route: '/admin/table-areas', name: 'table-areas' },
  { role: 'admin', route: '/admin/ingredients', name: 'ingredients' },
  { role: 'admin', route: '/admin/popular-items', name: 'popular-items' },
  { role: 'admin', route: '/admin/purchase-suggestions', name: 'purchase-suggestions' },
  { role: 'admin', route: '/admin/vouchers', name: 'vouchers' },
  { role: 'admin', route: '/admin/posts', name: 'posts' },
].filter(screen => !process.env.SMOKE_SCREEN_PATTERN
  || new RegExp(process.env.SMOKE_SCREEN_PATTERN, 'i').test(screen.name))

const viewports = {
  desktop: { width: 1366, height: 768 },
  tablet: { width: 768, height: 1024 },
  mobile: { width: 390, height: 844 },
}

const sleep = milliseconds => new Promise(resolvePromise => setTimeout(resolvePromise, milliseconds))

async function login(account) {
  const response = await fetch(`${baseUrl}${account.endpoint}`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ username: account.username, password: '123' }),
  })
  if (!response.ok) throw new Error(`Login ${account.username} failed with HTTP ${response.status}`)
  const body = await response.json()
  if (!body.token) throw new Error(`Login ${account.username} returned no token`)
  return body
}

async function waitForDebugger() {
  for (let attempt = 0; attempt < 80; attempt += 1) {
    try {
      const response = await fetch(`http://127.0.0.1:${debuggingPort}/json/list`)
      if (response.ok) {
        const pages = await response.json()
        const page = pages.find(candidate => candidate.type === 'page')
        if (page) return page.webSocketDebuggerUrl
      }
    } catch {
      // Browser is still starting.
    }
    await sleep(250)
  }
  throw new Error('Edge DevTools endpoint did not become ready')
}

function connectCdp(webSocketUrl) {
  const socket = new WebSocket(webSocketUrl)
  let nextId = 1
  const pending = new Map()
  const listeners = new Map()

  socket.addEventListener('message', event => {
    const message = JSON.parse(event.data)
    if (message.id) {
      const request = pending.get(message.id)
      if (!request) return
      pending.delete(message.id)
      if (message.error) request.reject(new Error(message.error.message))
      else request.resolve(message.result)
      return
    }
    for (const listener of listeners.get(message.method) || []) listener(message.params)
  })

  const opened = new Promise((resolvePromise, reject) => {
    socket.addEventListener('open', resolvePromise, { once: true })
    socket.addEventListener('error', () => reject(new Error('CDP WebSocket failed')), { once: true })
  })

  return {
    opened,
    on(method, listener) {
      const methodListeners = listeners.get(method) || []
      methodListeners.push(listener)
      listeners.set(method, methodListeners)
    },
    send(method, params = {}) {
      const id = nextId
      nextId += 1
      return new Promise((resolvePromise, reject) => {
        pending.set(id, { resolve: resolvePromise, reject })
        socket.send(JSON.stringify({ id, method, params }))
      })
    },
    close() {
      socket.close()
    },
  }
}

mkdirSync(outputDir, { recursive: true })
mkdirSync(profileDir, { recursive: true })

const browser = spawn(edgePath, [
  '--headless=new',
  '--disable-gpu',
  '--hide-scrollbars',
  '--no-first-run',
  '--disable-extensions',
  `--remote-debugging-port=${debuggingPort}`,
  `--user-data-dir=${profileDir}`,
  'about:blank',
], { stdio: 'ignore', windowsHide: true })

let cdp
try {
  const sessions = {}
  for (const [role, account] of Object.entries(accounts)) sessions[role] = await login(account)

  cdp = connectCdp(await waitForDebugger())
  await cdp.opened
  await cdp.send('Page.enable')
  await cdp.send('Runtime.enable')
  await cdp.send('Network.enable')

  let exceptions = []
  let failedRequests = []
  cdp.on('Runtime.exceptionThrown', event => exceptions.push(event.exceptionDetails?.text || 'Runtime exception'))
  cdp.on('Network.loadingFailed', event => {
    if (!event.canceled) failedRequests.push(event.errorText || 'Network request failed')
  })

  await cdp.send('Page.navigate', { url: baseUrl })
  await sleep(1200)

  const results = []
  for (const screen of protectedScreens) {
    const loginResponse = sessions[screen.role]
    const user = { ...loginResponse }
    delete user.token
    for (const [viewportName, viewport] of Object.entries(viewports)) {
      exceptions = []
      failedRequests = []
      await cdp.send('Emulation.setDeviceMetricsOverride', {
        width: viewport.width,
        height: viewport.height,
        deviceScaleFactor: 1,
        mobile: viewportName === 'mobile',
      })
      await cdp.send('Runtime.evaluate', {
        expression: `sessionStorage.clear(); sessionStorage.setItem('staff_token', ${JSON.stringify(loginResponse.token)}); sessionStorage.setItem('staff_user', ${JSON.stringify(JSON.stringify(user))}); sessionStorage.setItem('auth_context', 'staff');`,
      })
      await cdp.send('Page.navigate', { url: `${baseUrl}${screen.route}` })
      await sleep(2500)
      await cdp.send('Runtime.evaluate', { expression: 'scrollTo(0, 0)' })
      const evaluation = await cdp.send('Runtime.evaluate', {
        returnByValue: true,
        expression: `(() => ({
          path: location.pathname,
          title: document.title,
          readyState: document.readyState,
          textLength: (document.body?.innerText || '').trim().length,
          viewportWidth: innerWidth,
          documentWidth: Math.max(document.documentElement?.scrollWidth || 0, document.body?.scrollWidth || 0),
          viewportHeight: innerHeight,
          documentHeight: Math.max(document.documentElement?.scrollHeight || 0, document.body?.scrollHeight || 0),
          visibleButtons: [...document.querySelectorAll('button')].filter(button => {
            const rect = button.getBoundingClientRect();
            const style = getComputedStyle(button);
            return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
          }).length,
          offscreenButtons: [...document.querySelectorAll('button')].filter(button => {
            const rect = button.getBoundingClientRect();
            const style = getComputedStyle(button);
            const visible = rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
            return visible && (rect.right <= 0 || rect.left >= innerWidth);
          }).length,
          errorText: [...document.querySelectorAll('.error, .error-message, [role=alert]')].map(node => node.innerText).filter(Boolean).slice(0, 5),
        }))()`,
      })
      const metrics = evaluation.result.value
      const screenshot = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })
      const filename = `${screen.name}-${viewportName}.png`
      writeFileSync(join(outputDir, filename), Buffer.from(screenshot.data, 'base64'))
      results.push({
        screen: screen.name,
        role: screen.role,
        viewport: viewportName,
        expectedPath: screen.route,
        ...metrics,
        horizontalOverflow: metrics.documentWidth > metrics.viewportWidth + 2,
        exceptions,
        failedRequests,
        screenshot: filename,
      })
    }
  }

  const summary = {
    baseUrl,
    authenticatedRoles: Object.fromEntries(Object.entries(sessions).map(([role, value]) => [role, value.roles])),
    results,
  }
  writeFileSync(join(outputDir, 'summary.json'), JSON.stringify(summary, null, 2))
  console.log(JSON.stringify(summary, null, 2))
} finally {
  if (cdp) cdp.close()
  browser.kill('SIGTERM')
  await sleep(500)
  if (resolve(profileDir).startsWith(resolve(tmpdir()))) rmSync(profileDir, { recursive: true, force: true })
}
