const fs = require('node:fs');
const file = process.argv[2];
const source = fs.readFileSync(file, 'utf8');
const version = file.endsWith('.json') ? JSON.parse(source).version : source.match(/version\s*=\s*['"]([^'"]+)/)?.[1];
if (!version || !/^[a-zA-Z0-9_.-]+$/.test(version)) throw new Error('Missing or invalid image version: ' + file);
console.log(version);
