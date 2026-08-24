import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { extname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const sourceRoot = fileURLToPath(new URL('../', import.meta.url))
const legacyHex = [
  '#1a170f', '#201d14', '#33422a', '#22301b', '#5a6e45', '#7fa08f',
  '#c08a2e', '#b98229', '#8a641f', '#e2dcc2', '#ded8c2', '#e7e3d2',
  '#cfc7a8', '#7a7460', '#55503e', '#b23b2e', '#2f8f5b',
  '#f6f1df', '#1f2b1a', '#24331f', '#e7d5b8', '#f7d77b', '#edca78',
  '#d1a13a', '#8dc6a4', '#176b3a', '#dfc99f', '#f2f7e9', '#32713b',
  '#668454', '#fbfff8', '#178143', '#5b8a4e', '#f7fff4'
]
const legacyRgb = [
  '90, 110, 69', '192, 138, 46', '185, 130, 41', '178, 59, 46',
  '47, 143, 91', '51, 66, 42', '207, 199, 168', '36, 51, 31',
  '31, 43, 26', '26, 23, 15'
]

function sourceFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) return sourceFiles(path)
    return ['.vue', '.css', '.js'].includes(extname(entry.name)) && entry.name !== 'themeTokens.test.js' ? [path] : []
  })
}

function paletteViolations(paths) {
  const violations = []
  const forbiddenRgb = new Set(legacyRgb.map(value => value.replace(/\s/g, '')))
  for (const path of paths) {
    const source = readFileSync(path, 'utf8').toLowerCase()
    for (const color of legacyHex) {
      if (source.includes(color)) violations.push(`${path}: ${color}`)
    }
    for (const match of source.matchAll(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/gi)) {
      const triplet = `${match[1]},${match[2]},${match[3]}`
      if (forbiddenRgb.has(triplet)) violations.push(`${path}: rgb(${triplet})`)
    }
  }
  return violations
}

function hueOf(red, green, blue) {
  const r = red / 255
  const g = green / 255
  const b = blue / 255
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  const delta = max - min
  if (delta === 0) return { hue: 0, saturation: 0 }
  let hue
  if (max === r) hue = 60 * (((g - b) / delta) % 6)
  else if (max === g) hue = 60 * ((b - r) / delta + 2)
  else hue = 60 * ((r - g) / delta + 4)
  if (hue < 0) hue += 360
  const lightness = (max + min) / 2
  const denominator = 1 - Math.abs(2 * lightness - 1)
  return { hue, saturation: denominator === 0 ? 0 : delta / denominator }
}

function blueThemeViolations(paths) {
  const violations = []
  for (const path of paths) {
    const source = readFileSync(path, 'utf8').toLowerCase()
    if (/\bblue(?:-\d{2,3})?\b/.test(source)) violations.push(`${path}: blue token/class`)
    for (const match of source.matchAll(/#([0-9a-f]{6})\b/gi)) {
      const value = match[1]
      const { hue, saturation } = hueOf(
        Number.parseInt(value.slice(0, 2), 16),
        Number.parseInt(value.slice(2, 4), 16),
        Number.parseInt(value.slice(4, 6), 16)
      )
      if (saturation >= 0.2 && hue >= 180 && hue <= 260) violations.push(`${path}: #${value}`)
    }
    for (const match of source.matchAll(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/gi)) {
      const { hue, saturation } = hueOf(Number(match[1]), Number(match[2]), Number(match[3]))
      if (saturation >= 0.2 && hue >= 180 && hue <= 260) {
        violations.push(`${path}: rgb(${match[1]},${match[2]},${match[3]})`)
      }
    }
  }
  return violations
}

function directEmojiViolations(paths) {
  const violations = []
  for (const path of paths) {
    const source = readFileSync(path, 'utf8')
    if (/\p{Extended_Pictographic}/u.test(source)) violations.push(path)
  }
  return violations
}

describe('canonical visual theme', () => {
  it('keeps the legacy green, gold and cream palette out of source styles', () => {
    expect(paletteViolations(sourceFiles(sourceRoot))).toEqual([])
  })

  it('keeps blue theme colors and blue utility classes out of source styles', () => {
    expect(blueThemeViolations(sourceFiles(sourceRoot))).toEqual([])
  })

  it('keeps the generated backend bundle free of the legacy palette', () => {
    const staticRoot = resolve(sourceRoot, '..', '..', '..', 'quanlynhahang', 'src', 'main', 'resources', 'static')
    expect(paletteViolations(sourceFiles(staticRoot))).toEqual([])
  })

  it('declares the canonical burgundy and rose tokens', () => {
    const tokens = readFileSync(join(sourceRoot, 'assets', 'theme-tokens.css'), 'utf8').toLowerCase()
    expect(tokens).toContain('--color-primary: #b7102a')
    expect(tokens).toContain('--color-secondary: #8f3044')
  })

  it('renders admin navigation icons from the shared currentColor SVG component', () => {
    const icon = readFileSync(join(sourceRoot, 'components', 'AdminNavIcon.vue'), 'utf8')
    const layout = readFileSync(join(sourceRoot, 'components', 'AdminLayout.vue'), 'utf8')
    expect(icon).toContain('stroke="currentColor"')
    expect(layout).toContain('<AdminNavIcon name="analytics" />')
  })

  it('keeps direct emoji out of frontend UI source', () => {
    expect(directEmojiViolations(sourceFiles(sourceRoot))).toEqual([])
  })
})
