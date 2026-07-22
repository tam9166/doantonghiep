const enabled = String(import.meta.env.VITE_CAPTCHA_ENABLED || 'false').toLowerCase() === 'true'
const provider = String(import.meta.env.VITE_CAPTCHA_PROVIDER || 'mock').toLowerCase()
const mockToken = import.meta.env.VITE_CAPTCHA_MOCK_TOKEN || ''
const siteKey = import.meta.env.VITE_CAPTCHA_SITE_KEY || ''

let turnstilePromise = null
let recaptchaPromise = null
const turnstileWidgets = new Map()

export function captchaActionForRequest(config) {
  const method = String(config.method || 'get').toUpperCase()
  const path = requestPath(config)

  if (method === 'POST' && /^\/api\/auth\/(login|staff\/login|signup)$/.test(path)) {
    return 'auth'
  }
  if (method === 'POST' && path === '/api/reservations') {
    return 'reservation-create'
  }
  if (path.startsWith('/api/chatbot')) {
    return 'chatbot'
  }
  if (method === 'POST' && path.startsWith('/api/reviews')) {
    return 'review-create'
  }
  if (method === 'POST' && path.startsWith('/api/applications')) {
    return 'application-upload'
  }
  return null
}

export async function executeCaptcha(action) {
  if (!enabled) {
    return ''
  }
  if (provider === 'mock') {
    return mockToken
  }
  if (!siteKey) {
    throw new Error('CAPTCHA site key is not configured')
  }
  if (provider === 'turnstile') {
    return executeTurnstile(action)
  }
  if (provider === 'recaptcha') {
    return executeRecaptcha(action)
  }
  throw new Error(`Unsupported CAPTCHA provider: ${provider}`)
}

function requestPath(config) {
  const origin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost'
  // Axios accepts relative base URLs such as "/". The URL constructor does
  // not, so first resolve the configured base against the current origin.
  const base = new URL(config.baseURL || origin, origin).href
  const url = new URL(config.url || '', base)
  return url.pathname
}

function loadScript(src, id) {
  const existing = document.getElementById(id)
  if (existing) {
    return Promise.resolve()
  }
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.id = id
    script.src = src
    script.async = true
    script.defer = true
    script.onload = resolve
    script.onerror = () => reject(new Error(`Could not load ${id}`))
    document.head.appendChild(script)
  })
}

async function executeTurnstile(action) {
  if (!turnstilePromise) {
    turnstilePromise = loadScript('https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit', 'turnstile-api')
  }
  await turnstilePromise
  await waitFor(() => window.turnstile)

  let widget = turnstileWidgets.get(action)
  if (!widget) {
    const container = document.createElement('div')
    container.style.display = 'none'
    document.body.appendChild(container)
    widget = { id: null, container }
    turnstileWidgets.set(action, widget)
  }

  return new Promise((resolve, reject) => {
    const options = {
      sitekey: siteKey,
      action,
      size: 'invisible',
      callback: token => resolve(token),
      'error-callback': () => reject(new Error('CAPTCHA verification failed')),
      'expired-callback': () => reject(new Error('CAPTCHA token expired'))
    }

    if (widget.id === null) {
      widget.id = window.turnstile.render(widget.container, options)
    } else {
      window.turnstile.reset(widget.id)
    }
    window.turnstile.execute(widget.id)
  })
}

async function executeRecaptcha(action) {
  if (!recaptchaPromise) {
    recaptchaPromise = loadScript(
      `https://www.google.com/recaptcha/api.js?render=${encodeURIComponent(siteKey)}`,
      'recaptcha-api'
    )
  }
  await recaptchaPromise
  await waitFor(() => window.grecaptcha)

  return new Promise(resolve => {
    window.grecaptcha.ready(() => {
      resolve(window.grecaptcha.execute(siteKey, { action }))
    })
  })
}

function waitFor(predicate) {
  return new Promise((resolve, reject) => {
    const startedAt = Date.now()
    const timer = window.setInterval(() => {
      if (predicate()) {
        window.clearInterval(timer)
        resolve()
        return
      }
      if (Date.now() - startedAt > 10000) {
        window.clearInterval(timer)
        reject(new Error('CAPTCHA provider did not initialize'))
      }
    }, 50)
  })
}
