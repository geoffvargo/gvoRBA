// Compiles the Tailwind classes used in the backend's email templates into
// backend/gvoRBA-Backend/src/main/resources/email/tailwind.json (committed):
//   rules:    [{ class, style }] in CSS order; TailwindInliner writes these into style="" at send time
//   extraCss: @media / :hover rules, which can't be inlined and go into a <style> tag instead
// Usage: npm run email-css         (once)
//        npm run email-css:watch   (rebuild whenever a template changes)
import { compile, optimize } from '@tailwindcss/node'
import { Scanner } from '@tailwindcss/oxide'
import { transform } from 'lightningcss'
import postcss from 'postcss'
import { mkdirSync, readFileSync, watch, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const backendResources = join(here, '../../../../backend/gvoRBA-Backend/src/main/resources')
const templatesDir = join(backendResources, 'templates/email')
const outFile = join(backendResources, 'email/tailwind.json')

/** Matches a selector that is exactly one class, e.g. `.p-6` or `.w-1\/2`. */
const SINGLE_CLASS = /^\.((?:\\.|[^\s.:,>+~[\]()])+)$/
/** Matches `var(--name)` or `var(--name, fallback)`; the fallback may not contain parentheses. */
const VAR_REF = /var\(\s*(--[\w-]+)\s*(?:,\s*([^()]*))?\)/g

async function build() {
  const candidates = new Scanner({
    sources: [{ base: templatesDir, pattern: '**/*.html', negated: false }],
  }).scan()

  const compiler = await compile(readFileSync(join(here, 'input.css'), 'utf8'), {
    base: here,
    onDependency() {},
  })
  const optimized = optimize(compiler.build(candidates)).code

  // Old browser targets make Lightning CSS turn logical properties (margin-inline) into
  // margin-left/right, which Outlook and older clients understand.
  const lowered = transform({
    filename: 'email.css',
    code: Buffer.from(optimized),
    targets: { ie: 11 << 16, safari: 9 << 16 },
  }).code.toString()

  const root = postcss.parse(lowered)

  // @property blocks hold the default values of Tailwind's internal --tw-* variables.
  const initialValues = {}
  root.walkAtRules('property', (atRule) => {
    atRule.walkDecls('initial-value', (decl) => (initialValues[atRule.params.trim()] = decl.value))
    atRule.remove()
  })
  root.walkAtRules('layer', (atRule) => atRule.remove())
  root.walkComments((comment) => comment.remove())

  const resolveVars = (value) =>
    value.replace(VAR_REF, (match, name, fallback) => initialValues[name] ?? fallback?.trim() ?? match)

  const rules = []
  root.each((node) => {
    const match = node.type === 'rule' && node.selector.match(SINGLE_CLASS)
    if (!match) return
    const style = node.nodes
      .filter((child) => child.type === 'decl')
      .map((decl) => `${decl.prop}: ${resolveVars(decl.value)}${decl.important ? ' !important' : ''}`)
      .join('; ')
    if (style.includes('var(')) console.warn(`[email-css] unresolved var() left in .${match[1]}: ${style}`)
    rules.push({ class: match[1].replace(/\\(.)/g, '$1'), style })
    node.remove()
  })

  root.walkDecls((decl) => (decl.value = resolveVars(decl.value)))
  const extraCss = root.toString().trim()

  mkdirSync(dirname(outFile), { recursive: true })
  writeFileSync(outFile, JSON.stringify({ rules, extraCss }, null, 2) + '\n')
  console.log(`[email-css] ${rules.length} classes -> ${outFile}`)
}

await build()

if (process.argv.includes('--watch')) {
  let timer
  console.log(`[email-css] watching ${templatesDir}`)
  watch(templatesDir, { recursive: true }, () => {
    clearTimeout(timer)
    timer = setTimeout(() => build().catch((e) => console.error(e)), 100)
  })
}