// PostCSS plugin: scans Angular templates for variant groups and hands the expanded classes to
// Tailwind via @source inline(), because Tailwind's own scanner can't read `md:(flex gap-4)`.
// Must run before @tailwindcss/postcss.
const fs = require('node:fs/promises');
const path = require('node:path');
const fastGlob = require('fast-glob');
const postcss = require('postcss');
const { expandTemplate } = require('./tvg-html.cjs');

const TAILWIND_IMPORT = /^\s*["']tailwindcss(?:["']|\s)/u;

module.exports = (options = {}) => {
  const base = path.resolve(options.base ?? process.cwd());
  const include = options.include ?? ['src/**/*.html'];

  return {
    postcssPlugin: 'postcss-tvg-html',
    async Once(root, { result }) {
      let tailwindImport;
      root.walkAtRules('import', (atRule) => {
        if (TAILWIND_IMPORT.test(atRule.params)) {
          tailwindImport = atRule;
          return false;
        }
      });
      if (!tailwindImport) return;

      const files = await fastGlob(include, { absolute: true, cwd: base, onlyFiles: true });
      const candidates = new Set();
      for (const file of files) {
        const filename = path.normalize(file);
        const html = await fs.readFile(filename, 'utf8');
        for (const candidate of expandTemplate(html, filename).candidates) candidates.add(candidate);
        result.messages.push({ type: 'dependency', plugin: 'postcss-tvg-html', file: filename });
      }
      for (const glob of include) {
        result.messages.push({ type: 'dir-dependency', plugin: 'postcss-tvg-html', dir: base, glob });
      }

      if (candidates.size === 0) return;
      tailwindImport.parent.insertAfter(
        tailwindImport,
        postcss.atRule({ name: 'source', params: `inline(${JSON.stringify([...candidates].sort().join(' '))})` }),
      );
    },
  };
};
module.exports.postcss = true;
