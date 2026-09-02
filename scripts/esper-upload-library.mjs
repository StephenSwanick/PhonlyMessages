/**
 * Upload signed Phonly Messages to the Esper enterprise app library.
 * Does not change blueprints. Does not CONVERGE.
 *
 *   node scripts/esper-upload-library.mjs --dry-run
 *   node scripts/esper-upload-library.mjs
 *
 * Esper credentials come from Phonly Code `.env` (same as Phone).
 */
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dryRun = process.argv.includes('--dry-run');
const PKG = 'co.phonly.messages';
const VERSION_CODE = 31;
const APK = join(
  process.env.LOCALAPPDATA || join(process.env.USERPROFILE, 'AppData/Local'),
  `phonly-messages-build/app/outputs/apk/foss/release/messages-${VERSION_CODE}-foss-release.apk`,
);
const APK_NAME = `messages-${VERSION_CODE}-foss-release.apk`;

function loadEnv() {
  const messagesRoot = join(dirname(fileURLToPath(import.meta.url)), '..');
  const codeEnv = join(messagesRoot, '..', 'Phonly Code', '.env');
  if (!existsSync(codeEnv)) {
    throw new Error(`Missing ${codeEnv}`);
  }
  const env = {};
  for (const line of readFileSync(codeEnv, 'utf8').split(/\r?\n/)) {
    if (!line.trim() || line.trim().startsWith('#') || !line.includes('=')) continue;
    const i = line.indexOf('=');
    env[line.slice(0, i).trim()] = line.slice(i + 1).trim().replace(/^["']|["']$/g, '');
  }
  return env;
}

const env = loadEnv();
const { ESPER_TENANT: tenant, ESPER_API_KEY: apiKey } = env;
if (!tenant || !apiKey) {
  console.error('ESPER_TENANT and ESPER_API_KEY required in Phonly Code .env');
  process.exit(1);
}

const base = `https://${tenant}-api.esper.cloud/api`;
const jsonHeaders = {
  Authorization: `Bearer ${apiKey}`,
  'Content-Type': 'application/json',
};

async function esper(method, path, body) {
  const resp = await fetch(`${base}${path}`, {
    method,
    headers: jsonHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await resp.text();
  let parsed = null;
  try {
    parsed = text ? JSON.parse(text) : null;
  } catch {
    parsed = text;
  }
  return { ok: resp.ok, status: resp.status, parsed, text };
}

async function resolveEnterpriseId() {
  if (env.ESPER_ENTERPRISE_ID) return env.ESPER_ENTERPRISE_ID;
  const r = await esper('GET', '/enterprise/');
  const results = r.parsed?.content?.results ?? r.parsed?.results ?? [];
  return results[0]?.id;
}

async function findApp(enterpriseId, packageName) {
  const urls = [
    `/v1/enterprise/${enterpriseId}/application/?package_name=${encodeURIComponent(packageName)}`,
    `/v0/enterprise/${enterpriseId}/application/?package_name=${encodeURIComponent(packageName)}`,
    `/v0/enterprise/${enterpriseId}/application/?limit=50&search=${encodeURIComponent(packageName)}`,
  ];
  for (const url of urls) {
    const r = await esper('GET', url);
    if (!r.ok) continue;
    const results = r.parsed?.results ?? r.parsed?.content?.results ?? [];
    const hit = results.find((a) => (a.package_name ?? '') === packageName);
    if (hit) return hit;
  }
  return null;
}

async function uploadApk(enterpriseId) {
  const buf = readFileSync(APK);
  const fd = new FormData();
  fd.append(
    'app_file',
    new Blob([buf], { type: 'application/vnd.android.package-archive' }),
    APK_NAME,
  );
  const resp = await fetch(`${base}/enterprise/${enterpriseId}/application/upload/`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${apiKey}` },
    body: fd,
  });
  const text = await resp.text();
  let parsed = null;
  try {
    parsed = text ? JSON.parse(text) : null;
  } catch {
    parsed = text;
  }
  return { ok: resp.ok, status: resp.status, parsed, text };
}

if (!existsSync(APK)) {
  console.error('Missing signed APK:', APK);
  console.error('Build with: .\\gradlew.bat assembleFossRelease');
  process.exit(1);
}

const enterpriseId = await resolveEnterpriseId();
console.log(dryRun ? 'DRY RUN' : 'LIVE');
console.log('apk', APK);
console.log('package', PKG);
console.log('enterprise', enterpriseId);

const existing = await findApp(enterpriseId, PKG);
console.log('library before', existing ? { id: existing.id, package_name: existing.package_name } : 'not yet');

if (dryRun) {
  process.exit(0);
}

const uploaded = await uploadApk(enterpriseId);
if (!uploaded.ok) {
  console.error('UPLOAD failed', uploaded.status, String(uploaded.text).slice(0, 2000));
  process.exit(1);
}
const application = uploaded.parsed?.application ?? uploaded.parsed?.content ?? uploaded.parsed;
console.log('uploaded', {
  id: application?.id,
  package_name: application?.package_name,
  name: application?.application_name ?? application?.app_name,
  versions: (application?.versions ?? []).map((v) => ({
    id: v.id,
    version_code: v.version_code,
    release_name: v.release_name,
  })),
});

const catalog = application?.package_name === PKG ? application : await findApp(enterpriseId, PKG);
if (!catalog) {
  console.error('Upload succeeded but catalog lookup missed', PKG);
  process.exit(1);
}
console.log('library after', catalog.id, catalog.package_name);
console.log('Ready to SHOW on PhonlyV2 - DEV. CONVERGE AAAAY only after debug sideload is gone.');
