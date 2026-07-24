import { createHash } from 'node:crypto';
import { createReadStream } from 'node:fs';
import { readdir, stat, writeFile } from 'node:fs/promises';
import path from 'node:path';

const directory = process.argv[2];
if (!directory) throw new Error('Provide the directory containing release assets.');

const hashFile = (filePath) => new Promise((resolve, reject) => {
  const hash = createHash('sha256');
  const stream = createReadStream(filePath);
  stream.on('error', reject);
  stream.on('data', (chunk) => hash.update(chunk));
  stream.on('end', () => resolve(hash.digest('hex')));
});

const entries = await readdir(directory, { withFileTypes: true });
const files = entries
  .filter((entry) => entry.isFile() && !entry.name.endsWith('.sha256'))
  .map((entry) => entry.name)
  .sort();

if (files.length === 0) throw new Error(`No release assets found in ${directory}.`);

for (const filename of files) {
  const filePath = path.join(directory, filename);
  const fileStats = await stat(filePath);
  if (fileStats.size === 0) throw new Error(`Release asset is empty: ${filename}`);
  const digest = await hashFile(filePath);
  await writeFile(`${filePath}.sha256`, `${digest}  ${filename}\n`);
  console.log(`${filename}: ${digest}`);
}
