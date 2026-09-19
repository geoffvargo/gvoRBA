// Preloaded with `node -r` (see the "ng" script in package.json). Angular's compiler reads
// component templates through ts.sys.readFile, so expanding variant groups there means the
// compiled templates contain ordinary Tailwind classes while the .html source keeps the groups.
const ts = require('typescript');
const { expandTemplate } = require('./tvg-html.cjs');

const readFile = ts.sys.readFile;
ts.sys.readFile = function (fileName, encoding) {
  const text = readFile.call(this, fileName, encoding);
  if (text === undefined || !/\.html$/i.test(fileName) || /[\/]node_modules[\/]/.test(fileName)) {
    return text;
  }
  return expandTemplate(text, fileName).code;
};
