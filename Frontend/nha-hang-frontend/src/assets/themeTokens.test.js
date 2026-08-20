import { describe, expect, it } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { extname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const sourceRoot = fileURLToPath(new URL('../', import.meta.url))
const legacyHex = [
  '#1a170f', '#201d14', '#33422a', '#22301b', '#5a6e45', '#7fa08f',
  '#c08a2e', '#b98229', '#8a641f', '#e2dcc2', '#ded8c2', '#e7e3d2',
  '#cfc7a8', '#7a7460', '#55503e', '#b23b2e', '#2f8f5b'
]
const legacyRgb = [
  '90, 110, 69', '192, 138, 46', '185, 130, 41', '178, 59, 46',
  '47, 143, 91', '51, 66, 42', '207, 199, 168'
]

function sourceFiles(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) return sourceFiles(path)
    return ['.vue', '.css'].includes(extname(entry.name)) ? [path] : []
  })
}

describe('canonical visual theme', () => {
  it('keeps the legacy green, gold and cream palette out of source styles', () => {
    const violations = []
    for (const path of sourceFiles(sourceRoot)) {
      const source = readFileSync(path, 'utf8').toLowerCase()
      for (const color of [...legacyHex, ...legacyRgb]) {
        if (source.includes(color)) violations.push(`${path}: ${color}`)
      }
    }
    expect(violations).toEqual([])
  })

  it('declares the burgundy and navy canonical tokens', () => {
    const tokens = readFileSync(join(sourceRoot, 'assets', 'theme-tokens.css'), 'utf8').toLowerCase()
    expect(tokens).toContain('--color-primary: #b7102a')
    expect(tokens).toContain('--color-secondary: #485f84')
  })
})
