import i18n from '@/i18n'

function activeLocale() {
  return i18n.global.locale.value === 'en' ? 'en-US' : 'vi-VN'
}

export function formatCurrency(value) {
  return new Intl.NumberFormat(activeLocale(), {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0
  }).format(Number(value || 0))
}

export function formatNumber(value) {
  return new Intl.NumberFormat(activeLocale()).format(Number(value || 0))
}

export function formatDate(value) {
  return value ? new Intl.DateTimeFormat(activeLocale(), { dateStyle: 'medium' }).format(new Date(value)) : '-'
}

export function formatTime(value) {
  return value ? new Intl.DateTimeFormat(activeLocale(), { timeStyle: 'short' }).format(new Date(value)) : '-'
}

export function formatDateTime(value) {
  return value ? new Intl.DateTimeFormat(activeLocale(), { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '-'
}

export function useFormatters() {
  return { formatCurrency, formatNumber, formatDate, formatTime, formatDateTime }
}
