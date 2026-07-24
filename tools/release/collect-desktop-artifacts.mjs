import { copyFile, mkdir, readdir, stat } from 'node:fs/promises';
import path from 'node:path';

const [bundleRoot, destination, version, platform, architecture] = process.argv.slice(2);
const supportedPlatforms = new Set(['macos', 'windows', 'linux']);

if (![bundleRoot, destination, version, platform, architecture].every(Boolean)) {
  throw new Error(
    'Usage: collect-desktop-artifacts.mjs <bundle-root> <destination> <version> <platform> <architecture>'
  );
}
if (!supportedPlatforms.has(platform)) {
  throw new Error(`Unsupported desktop platform: ${platform}`);
}
if (!/^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/.test(version)) {
  throw new Error(`Invalid companion version: ${version}`);
}

const extensionsByPlatform = {
  macos: ['.dmg'],
  windows: ['.msi', '.exe'],
  linux: ['.AppImage', '.deb', '.rpm']
};

const walk = async (directory) => {
  const entries = await readdir(directory, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) files.push(...await walk(entryPath));
    else if (entry.isFile()) files.push(entryPath);
  }
  return files;
};

const normalizedExtension = (filePath) => {
  if (filePath.endsWith('.AppImage')) return '.AppImage';
  return path.extname(filePath).toLowerCase();
};

const matchingFiles = (await walk(bundleRoot)).filter((filePath) =>
  extensionsByPlatform[platform].includes(normalizedExtension(filePath))
);

if (matchingFiles.length === 0) {
  throw new Error(`No ${platform} installer was found under ${bundleRoot}.`);
}

await mkdir(destination, { recursive: true });
const copied = [];
for (const source of matchingFiles) {
  const extension = normalizedExtension(source);
  const suffix = platform === 'windows' && extension === '.exe'
    ? '-setup.exe'
    : extension;
  const filename = `GitHub-Rock-${version}-${platform}-${architecture}${suffix}`;
  const target = path.join(destination, filename);
  const sourceStats = await stat(source);
  if (sourceStats.size === 0) throw new Error(`Installer is empty: ${source}`);
  await copyFile(source, target);
  copied.push(filename);
}

console.log(`Collected ${copied.length} ${platform} installer(s):`);
copied.forEach((filename) => console.log(`- ${filename}`));
