// Expands Tailwind variant groups, e.g. md:(flex gap-4), inside static class="..." attributes
// of Angular templates. Shared by the compiler preload and the PostCSS plugin so the markup and
// the generated CSS always agree.
const { expandVariantGroupsInText } = require('tailwind-variant-groups');

const CLASS_ATTRIBUTE = /(\sclass\s*=\s*)(["'])([\s\S]*?)\2/g;

function expandTemplate(html, filename) {
  const candidates = [];
  const code = html.replace(CLASS_ATTRIBUTE, (match, prefix, quote, value) => {
    if (!value.includes('(')) return match;
    const expanded = expandVariantGroupsInText(value, { filename, source: html });
    candidates.push(...expanded.candidates);
    return prefix + quote + expanded.code + quote;
  });
  return { code, candidates };
}

module.exports = { expandTemplate };
