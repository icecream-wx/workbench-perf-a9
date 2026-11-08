#!/usr/bin/env node
/**
 * 员工接口烟测：列表/详情/新增/编辑/校验/唯一冲突/分页边界/OpenAPI。
 */
const BACKEND = process.env.APP_BACKEND_URL || 'http://127.0.0.1:8080';

async function call(path, options = {}) {
  const response = await fetch(BACKEND + path, {
    ...options,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', ...(options.headers || {}) },
  });
  const text = await response.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* non-JSON */ }
  return { status: response.status, json, text };
}

let failed = false;
function check(label, ok, detail = '') {
  console.log(`${ok ? 'PASS' : 'FAIL'} ${label}${detail ? `: ${detail}` : ''}`);
  if (!ok) failed = true;
}

const empNo = `SMOKE${Date.now()}`;
const name = '烟测员工';
let createdId = null;
try {
  const health = await call('/actuator/health');
  check('health UP', health.status === 200 && health.json?.status === 'UP');

  const openapi = await call('/v3/api-docs');
  const openapiPaths = Object.keys(openapi.json?.paths || {});
  check('openapi has employee paths',
    openapi.status === 200
      && openapiPaths.includes('/api/employees')
      && openapiPaths.includes('/api/employees/{id}'),
    openapiPaths.filter((item) => item.includes('employees')).join(','));

  const list = await call('/api/employees?page=1&size=10');
  check('list', list.status === 200 && list.json?.code === 0);
  const totalBefore = list.json?.data?.total ?? 0;

  const created = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify({ empNo, name, department: '烟测部', hireDate: '2024-01-01' }),
  });
  createdId = created.json?.data?.id;
  check('create 201', created.status === 201 && Boolean(createdId), created.text.slice(0, 200));
  check('create defaults', created.json?.data?.status === 'ACTIVE'
    && created.json?.data?.hireDate === '2024-01-01'
    && /^\d{4}-\d{2}-\d{2}T/.test(created.json?.data?.createdAt || ''), created.text.slice(0, 200));

  const detail = await call(`/api/employees/${createdId}`);
  check('detail', detail.status === 200
    && detail.json?.data?.empNo === empNo
    && detail.json?.data?.name === name, detail.text.slice(0, 160));

  const byKeyword = await call(`/api/employees?keyword=${encodeURIComponent(empNo)}&page=1&size=10`);
  check('keyword by empNo', byKeyword.status === 200 && byKeyword.json?.data?.total === 1);

  const duplicate = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify({ empNo, name }),
  });
  check('duplicate 409', duplicate.status === 409 && duplicate.json?.code === 40901,
    duplicate.text.slice(0, 160));
  const afterDuplicate = await call('/api/employees?page=1&size=10');
  check('duplicate not persisted', afterDuplicate.json?.data?.total === totalBefore + 1);

  const missingEmpNo = await call('/api/employees', { method: 'POST', body: JSON.stringify({ name }) });
  check('validation missing empNo 400', missingEmpNo.status === 400
    && String(missingEmpNo.json?.message || '').includes('empNo'), missingEmpNo.text.slice(0, 160));

  const badEmail = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify({ empNo: `${empNo}E`, name, email: 'abc' }),
  });
  check('validation email 400', badEmail.status === 400
    && String(badEmail.json?.message || '').includes('email'), badEmail.text.slice(0, 160));

  const badDate = await call('/api/employees', {
    method: 'POST',
    body: JSON.stringify({ empNo: `${empNo}D`, name, hireDate: '2024/01/01' }),
  });
  check('validation hireDate 400', badDate.status === 400
    && String(badDate.json?.message || '').includes('hireDate'), badDate.text.slice(0, 160));

  const updated = await call(`/api/employees/${createdId}`, {
    method: 'PUT',
    body: JSON.stringify({ empNo, name: `${name}改`, status: 'INACTIVE' }),
  });
  check('update 200', updated.status === 200
    && updated.json?.data?.name === `${name}改`
    && updated.json?.data?.status === 'INACTIVE', updated.text.slice(0, 160));
  check('update keeps createdAt', updated.json?.data?.createdAt === created.json?.data?.createdAt);

  const notFound = await call('/api/employees/99999999', {
    method: 'PUT',
    body: JSON.stringify({ empNo: `${empNo}X`, name }),
  });
  check('update 404', notFound.status === 404 && notFound.json?.code === 40401, notFound.text.slice(0, 160));

  const missingDetail = await call('/api/employees/99999999');
  check('detail 404', missingDetail.status === 404 && missingDetail.json?.code === 40401);

  const badPage = await call('/api/employees?page=0');
  const badSize = await call('/api/employees?size=101');
  check('paging 400', badPage.status === 400 && badSize.status === 400);
} catch (error) {
  console.error('Employee API smoke failed:', error.message);
  failed = true;
}
if (failed) process.exitCode = 1;
else console.log('Employee API smoke: PASS');
