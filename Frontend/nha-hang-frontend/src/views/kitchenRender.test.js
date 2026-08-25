/** @vitest-environment jsdom */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createApp, nextTick } from 'vue'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  disconnect: vi.fn(),
  subscribe: vi.fn()
}))

vi.mock('@/services/api', () => ({
  default: { get: mocks.get, put: vi.fn(), post: vi.fn() }
}))
vi.mock('@/services/session', () => ({
  clearStaffSession: vi.fn(),
  getStaffToken: () => 'kitchen-token',
  getStaffUser: () => ({ username: 'kitchen', roles: ['ROLE_KITCHEN'] })
}))
vi.mock('@/composables/useDialog', () => ({
  useDialog: () => ({ confirmDialog: vi.fn(), promptDialog: vi.fn() })
}))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('sockjs-client', () => ({ default: vi.fn(function SockJsStub() { return {} }) }))
vi.mock('@stomp/stompjs', () => ({
  Stomp: {
    over: () => ({
      debug: null,
      connect: (_headers, connected) => connected(),
      subscribe: mocks.subscribe,
      disconnect: mocks.disconnect
    })
  }
}))
vi.mock('../components/TimekeepingWidget.vue', () => ({
  default: { template: '<div data-testid="timekeeping"></div>' }
}))
vi.mock('@/components/StaffOperationsAssistant.vue', () => ({
  default: { template: '<div data-testid="assistant"></div>' }
}))

import Kitchen from './Kitchen.vue'

const mountedApps = []
const flush = async () => {
  await Promise.resolve()
  await Promise.resolve()
  await nextTick()
}

function mountKitchen() {
  const host = document.createElement('div')
  document.body.appendChild(host)
  const app = createApp(Kitchen)
  app.component('UiIcon', {
    props: { name: { type: String, default: '' } },
    template: '<span :data-icon="name"></span>'
  })
  app.config.globalProperties.$router = { push: vi.fn() }
  app.mount(host)
  mountedApps.push({ app, host })
  return host
}

describe('Kitchen runtime resilience', () => {
  beforeEach(() => {
    mocks.get.mockReset()
    mocks.subscribe.mockReset()
    mocks.disconnect.mockReset()
  })

  afterEach(() => {
    mountedApps.splice(0).forEach(({ app, host }) => {
      app.unmount()
      host.remove()
    })
  })

  it('renders the Kitchen shell and empty queue with successful APIs', async () => {
    mocks.get.mockImplementation(url => {
      if (url.includes('/kitchen/board')) return Promise.resolve({ data: [] })
      return Promise.resolve({ data: [] })
    })

    const host = mountKitchen()
    await flush()

    expect(host.textContent).toContain('BẾP — MỘC VỊ RESTAURANT')
    expect(host.textContent).toContain('Không có đơn nào cần nấu!')
    expect(mocks.subscribe).toHaveBeenCalledWith('/topic/kitchen', expect.any(Function))
  })

  it('keeps the page visible and shows a section error when inventory fails', async () => {
    mocks.get.mockImplementation(url => {
      if (url.includes('/kitchen/board')) return Promise.resolve({ data: [] })
      if (url.includes('/ingredients')) return Promise.reject({ response: { data: { message: 'Kho tạm thời không khả dụng' } } })
      return Promise.resolve({ data: [] })
    })

    const host = mountKitchen()
    await flush()
    const inventoryTab = [...host.querySelectorAll('button')]
      .find(button => button.textContent.includes('Tồn Kho'))
    inventoryTab.click()
    await nextTick()

    expect(host.textContent).toContain('BẾP — MỘC VỊ RESTAURANT')
    expect(host.textContent).toContain('Không thể tải dữ liệu tồn kho')
    expect(host.textContent).toContain('Kho tạm thời không khả dụng')
  })

  it('renders queue and inventory data returned by their APIs', async () => {
    mocks.get.mockImplementation(url => {
      if (url.includes('/kitchen/board')) {
        return Promise.resolve({ data: [{
          id: 91,
          status: 1,
          createDate: new Date().toISOString(),
          orderDetails: [{ id: 911, status: 0, quantity: 2, product: { name: 'Phở bò' } }]
        }] })
      }
      if (url.endsWith('/api/admin/ingredients')) {
        return Promise.resolve({ data: [{ id: 5, name: 'Gạo', unit: 'kg', quantity: '12.5000', minStock: '5.0000' }] })
      }
      if (url.includes('/expiring-batches')) {
        return Promise.resolve({ data: [{ id: 51, ingredientId: 5, quantity: 2, expirationDate: '2026-08-27T00:00:00Z' }] })
      }
      return Promise.resolve({ data: [] })
    })

    const host = mountKitchen()
    await flush()
    expect(host.textContent).toContain('#91')
    expect(host.textContent).toContain('Phở bò')

    const inventoryTab = [...host.querySelectorAll('button')]
      .find(button => button.textContent.includes('Tồn Kho'))
    inventoryTab.click()
    await nextTick()
    expect(host.textContent).toContain('Gạo')
    expect(host.textContent).toContain('12.5')
    expect(host.textContent).toContain('Còn 2 kg')
  })

  it('shows the queue error inside Kitchen instead of replacing the whole page', async () => {
    mocks.get.mockImplementation(url => {
      if (url.includes('/kitchen/board')) {
        return Promise.reject({ response: { data: { message: 'Hàng đợi bếp tạm thời không khả dụng' } } })
      }
      return Promise.resolve({ data: [] })
    })

    const host = mountKitchen()
    await flush()

    expect(host.textContent).toContain('BẾP — MỘC VỊ RESTAURANT')
    expect(host.textContent).toContain('Không thể tải dữ liệu bếp')
    expect(host.textContent).toContain('Hàng đợi bếp tạm thời không khả dụng')
  })
})
