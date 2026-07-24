import assert from 'node:assert/strict';
import { access, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDirectory, '..', '..');
const desktopRoot = path.join(repositoryRoot, 'desktop');
const tauriRoot = path.join(desktopRoot, 'src-tauri');

const packageJson = JSON.parse(await readFile(path.join(desktopRoot, 'package.json'), 'utf8'));
const packageLock = JSON.parse(await readFile(path.join(desktopRoot, 'package-lock.json'), 'utf8'));
const config = JSON.parse(await readFile(path.join(tauriRoot, 'tauri.conf.json'), 'utf8'));
const cargoToml = await readFile(path.join(tauriRoot, 'Cargo.toml'), 'utf8');
const rustEntryPoint = await readFile(path.join(tauriRoot, 'src', 'main.rs'), 'utf8');
const appScript = await readFile(path.join(repositoryRoot, 'site', 'assets', 'app.js'), 'utf8');
const buildWorkflow = await readFile(
  path.join(repositoryRoot, '.github', 'workflows', 'cross-platform-build.yml'),
  'utf8'
);
const releaseWorkflow = await readFile(
  path.join(repositoryRoot, '.github', 'workflows', 'release.yml'),
  'utf8'
);

assert.equal(packageJson.version, config.version, 'npm and Tauri versions must match.');
assert.equal(packageLock.version, config.version, 'npm lockfile and Tauri versions must match.');
assert.equal(packageLock.packages[''].version, config.version);
assert.match(cargoToml, new RegExp(`^version = "${config.version.replaceAll('.', '\\.')}"$`, 'm'));
assert.match(rustEntryPoint, /windows_subsystem = "windows"/);
assert.equal(config.identifier, 'com.sayanthrock.githubrock.companion');
assert.equal(config.build.frontendDist, '../../site');
assert.equal(config.app.withGlobalTauri, true);
assert.equal(config.bundle.active, true);
assert.ok(config.bundle.targets === 'all' || config.bundle.targets.length > 0);
assert.equal(config.bundle.iOS.minimumSystemVersion, '15.0');
assert.equal(config.bundle.macOS.minimumSystemVersion, '11.0');
assert.match(appScript, /__TAURI__/);
assert.match(appScript, /opener\.openUrl/);
assert.match(appScript, /!nativeShell && 'serviceWorker' in navigator/);

for (const expected of [
  'appimage,deb,rpm',
  'msi,nsis',
  'app,dmg',
  'aarch64-apple-darwin',
  'x86_64-apple-darwin',
  'GitHub-Rock-$VERSION-linux-$ASSET_ARCHITECTURE.pkg.tar.zst',
  'GitHub-Rock-$env:VERSION-windows-$env:ASSET_ARCHITECTURE-portable.zip',
  'IOS_CERTIFICATE',
  'IOS_MOBILE_PROVISION',
  'GitHub-Rock-$VERSION-ios-arm64.ipa'
]) {
  assert.ok(buildWorkflow.includes(expected), `Cross-platform workflow is missing: ${expected}`);
}

for (const expected of [
  'GitHub-Rock-$VERSION.apk',
  'GitHub-Rock-$VERSION-macos-arm64.dmg',
  'GitHub-Rock-$VERSION-macos-arm64.pkg',
  'GitHub-Rock-$VERSION-windows-x64.msi',
  'GitHub-Rock-$VERSION-windows-x64-setup.exe',
  'GitHub-Rock-$VERSION-linux-x64.AppImage',
  'GitHub-Rock-$VERSION-linux-x64.pkg.tar.zst',
  'GitHub-Rock-$VERSION-ios-arm64.ipa'
]) {
  assert.ok(releaseWorkflow.includes(expected), `Release workflow is missing: ${expected}`);
}

for (const file of [
  'build.rs',
  'src/main.rs',
  'src/lib.rs',
  'capabilities/default.json'
]) {
  await access(path.join(tauriRoot, file));
}

for (const icon of config.bundle.icon) {
  await access(path.join(tauriRoot, icon));
}

console.log('Cross-platform Tauri configuration checks passed.');
