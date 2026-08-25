export const MENU_PAGE_SIZE = 24

export function paginateMenu(items, requestedPage, pageSize = MENU_PAGE_SIZE) {
  const safeItems = Array.isArray(items) ? items : []
  const totalPages = Math.max(1, Math.ceil(safeItems.length / pageSize))
  const page = Math.min(totalPages, Math.max(1, Number(requestedPage) || 1))
  const start = (page - 1) * pageSize
  return { page, totalPages, items: safeItems.slice(start, start + pageSize) }
}
