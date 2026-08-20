const CUSTOMER_ROUTES = ['/', '/reservation', '/reservation-lookup', '/history', '/profile']
const SUPERVISOR_ROLES = ['ROLE_ADMIN', 'ROLE_MANAGER']
const ALL_STAFF_ROLES = [...SUPERVISOR_ROLES, 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER']

const ADMIN_ROUTE_ROLES = {
  '/admin/orders': ALL_STAFF_ROLES,
  '/admin/ingredients': [...SUPERVISOR_ROLES, 'ROLE_KITCHEN'],
  '/admin/popular-items': [...SUPERVISOR_ROLES, 'ROLE_KITCHEN'],
}

export function customerRouteRedirect(path, roles) {
  if (!CUSTOMER_ROUTES.includes(path) || roles.some(role => SUPERVISOR_ROLES.includes(role))) {
    return null
  }
  if (roles.includes('ROLE_KITCHEN')) return '/kitchen'
  if (roles.includes('ROLE_WAITER')) return '/waiter'
  if (roles.includes('ROLE_CASHIER')) return '/cashier'
  return null
}

export function canAccessOperationalWorkspace(path, roles) {
  if (path.startsWith('/kitchen')) {
    return roles.some(role => ['ROLE_KITCHEN', ...SUPERVISOR_ROLES].includes(role))
  }
  if (path.startsWith('/waiter')) {
    return roles.some(role => ['ROLE_WAITER', ...SUPERVISOR_ROLES].includes(role))
  }
  if (path.startsWith('/cashier')) {
    return roles.some(role => ['ROLE_CASHIER', ...SUPERVISOR_ROLES].includes(role))
  }
  return true
}

export function canAccessAdminRoute(path, roles) {
  const allowedRoles = ADMIN_ROUTE_ROLES[path] || SUPERVISOR_ROLES
  return roles.some(role => allowedRoles.includes(role))
}
