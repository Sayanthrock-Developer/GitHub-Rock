import assert from 'node:assert/strict';
import { access, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDirectory, '..', '..');
const siteRoot = path.join(repositoryRoot, 'site');

const readSiteFile = (relativePath) =>
  readFile(path.join(siteRoot, relativePath), 'utf8');

const manifest = JSON.parse(await readSiteFile('manifest.webmanifest'));
const html = await readSiteFile('index.html');
const appScript = await readSiteFile('assets/app.js');
const serviceWorker = await readSiteFile('sw.js');

assert.equal(manifest.display, 'standalone');
assert.equal(manifest.prefer_related_applications, false);
assert.match(manifest.start_url, /^\.\//);
assert.match(manifest.scope, /^\.\//);
assert.ok(Array.isArray(manifest.icons), 'Manifest icons must be an array.');

for (const size of ['192x192', '512x512']) {
  const icon = manifest.icons.find((candidate) => candidate.sizes === size);
  assert.ok(icon, `Manifest is missing its ${size} icon.`);
  assert.equal(icon.type, 'image/png');
  assert.match(icon.purpose, /\bany\b/);

  const iconPath = path.resolve(siteRoot, icon.src);
  assert.ok(
    iconPath.startsWith(`${siteRoot}${path.sep}`),
    `Manifest icon escapes the site directory: ${icon.src}`
  );
  await access(iconPath);
}

for (const platform of ['macOS', 'Windows', 'Linux', 'iOS / iPadOS']) {
  assert.ok(html.includes(platform), `Website is missing the ${platform} install surface.`);
}

assert.match(html, /rel="manifest"/);
assert.match(html, /data-install-app/);
assert.match(html, /data-install-dialog/);
assert.match(appScript, /beforeinstallprompt/);
assert.match(appScript, /showInstallGuide/);
assert.match(appScript, /serviceWorker\.register/);
assert.match(serviceWorker, /manifest\.webmanifest/);
assert.match(serviceWorker, /apple-touch-icon\.png/);

console.log('PWA installability checks passed for macOS, Windows, Linux, iOS, and Android.');
