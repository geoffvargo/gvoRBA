// Compiles the Tailwind classes used in the backend email templates into
// backend/gvoRBA-Backend/src/main/resources/email/tailwind.json, which TailwindInliner
// turns into inline styles. Run with `npm run email-css` after editing a template.
import { compile } from '@tailwindcss/node';
import { transform } from 'lightningcss';
import { watch } from 'node:fs';
import { mkdir, readdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const resources = join(root, '..', '..', 'backend', 'gvoRBA-Backend', 'src', 'main', 'resources');
const templatesDir = join(resources, 'templates', 'email');
const outFile = join(resources, 'email', 'tailwind.json');

async function collectClasses() {
  const classes = new Set();
  for (const file of (await readdir(templatesDir, { recursive: true })).filter((f) => f.endsWith('.html'))) {
    const html = await readFile(join(templatesDir, file), 'utf8');
    for (const [, value] of html.matchAll(/\bclass="([^"]*)"/g)) {
      value.split(/\s+/).filter(Boolean).forEach((c) => classes.add(c));
    }
  }
  return [...classes].sort();
}

/// Resolves var(--x) using the :root/:host custom properties declared in the compiled CSS.
function resolveVars(css) {
  const vars = new Map();
  for (const [, name, value] of css.matchAll(/(--[\w-]+)\s*:\s*([^;}]+)/g)) {
    if (!vars.has(name)) vars.set(name, value.trim());
  }
  const sub = (text) =>
    text.replace(/var\(\s*(--[\w-]+)\s*(?:,\s*([^()]*))?\)/g, (m, name, fallback) =>
      vars.has(name) ? vars.get(name) : (fallback ?? m).trim(),
    );
  let prev;
  do {
    prev = css;
    css = sub(css);
  } while (css !== prev);
  return css;
}

async function build() {
  const classes = await collectClasses();
  const compiler = await compile('@import "@maizzle/tailwindcss";', {
    base: root,
    onDependency: () => {},
  });
  const raw = resolveVars(compiler.build(classes));
  const { code } = transform({
    filename: 'email.css',
    code: Buffer.from(raw),
    minify: false,
    targets: { safari: 12 << 16 },
  });
  const css = code.toString();

  const rules = [];
  const extra = [];
  const classNames = new Set(classes);
  for (const [, selector, body] of css.matchAll(/^\.((?:\\.|[\w-])+)\s*\{([^}]*)\}/gm)) {
    const name = selector.replace(/\\(.)/g, '$1');
    if (!classNames.has(name)) continue;
    const style = body
      .split(';')
      .map((d) => d.replace(/\s*!important\s*$/, '').trim())
      .filter((d) => d && !d.startsWith('--'))
      .join('; ');
    if (style) rules.push({ class: name, style });
  }
  for (const [block] of css.matchAll(/^@media[^{]*\{(?:[^{}]*\{[^}]*\})*\s*\}/gm)) {
    extra.push(block.replace(/\s*!important/g, ''));
  }

  await mkdir(dirname(outFile), { recursive: true });
  await writeFile(outFile, JSON.stringify({ rules, extraCss: extra.join('\n') }, null, 2) + '\n');
  console.log(`email-css: wrote ${rules.length} rules to ${outFile}`);
  return classes;
}

await build();
if (process.argv.includes('--watch')) {
  watch(templatesDir, { recursive: true }, () => build().catch(console.error));
}
