import { readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const version = process.argv[2];
if (!version || !/^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/.test(version)) {
  throw new Error('Provide a semantic version such as 0.2.2 or 0.3.0-beta.1.');
}

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDirectory, '..', '..');
const packagePath = path.join(repositoryRoot, 'desktop', 'package.json');
const packageLockPath = path.join(repositoryRoot, 'desktop', 'package-lock.json');
const configPath = path.join(repositoryRoot, 'desktop', 'src-tauri', 'tauri.conf.json');
const cargoPath = path.join(repositoryRoot, 'desktop', 'src-tauri', 'Cargo.toml');

const packageJson = JSON.parse(await readFile(packagePath, 'utf8'));
packageJson.version = version;
await writeFile(packagePath, `${JSON.stringify(packageJson, null, 2)}\n`);

const packageLock = JSON.parse(await readFile(packageLockPath, 'utf8'));
packageLock.version = version;
packageLock.packages[''].version = version;
await writeFile(packageLockPath, `${JSON.stringify(packageLock, null, 2)}\n`);

const tauriConfig = JSON.parse(await readFile(configPath, 'utf8'));
tauriConfig.version = version;
await writeFile(configPath, `${JSON.stringify(tauriConfig, null, 2)}\n`);

const cargoToml = await readFile(cargoPath, 'utf8');
const updatedCargoToml = cargoToml.replace(
  /^version = "[^"]+"/m,
  `version = "${version}"`
);
if (updatedCargoToml === cargoToml && !cargoToml.includes(`version = "${version}"`)) {
  throw new Error('Could not update the desktop Cargo package version.');
}
await writeFile(cargoPath, updatedCargoToml);

console.log(`GitHub Rock companion version set to ${version}.`);
