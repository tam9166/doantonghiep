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

describe('canonical visual theme', () => {
  it('keeps the legacy green, gold and cream palette out of source styles', () => {
    expect(paletteViolations(sourceFiles(sourceRoot))).toEqual([])
  })

  it('keeps the generated backend bundle free of the legacy palette', () => {
    const staticRoot = resolve(sourceRoot, '..', '..', '..', 'quanlynhahang', 'src', 'main', 'resources', 'static')
    expect(paletteViolations(sourceFiles(staticRoot))).toEqual([])
  })

  it('declares the burgundy and navy canonical tokens', () => {
    const tokens = readFileSync(join(sourceRoot, 'assets', 'theme-tokens.css'), 'utf8').toLowerCase()
    expect(tokens).toContain('--color-primary: #b7102a')
    expect(tokens).toContain('--color-secondary: #485f84')
  })
})
