#!/usr/bin/env node
/**
 * 员工页面烟测：直达路由、导航入口、列表加载、新增、工号冲突提示、编辑。
 */
import fs from 'node:fs';
import { Browser } from './cdp.mjs';

const BASE = process.env.APP_FRONTEND_URL || 'http://127.0.0.1:5173';
const candidates = [
  process.env.CHROME_PATH,
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
].filter(Boolean);
const chrome = candidates.find((candidate) => fs.existsSync(candidate));
if (!chrome) {
  console.error('Employee UI smoke: no Chrome/Edge found');
  process.exit(1);
}

const browser = await new Browser({ chromePath: chrome, width: 1440, height: 900 }).launch();
let failed = false;
function check(label, ok, detail = '') {
  console.log(`${ok ? 'PASS' : 'FAIL'} ${label}${detail ? `: ${detail}` : ''}`);
  if (!ok) failed = true;
}

const dialogOpen = '(() => { const d = document.querySelector(".el-dialog"); return Boolean(d && d.offsetParent !== null); })()';
const dialogClosed = '(() => { const d = document.querySelector(".el-dialog"); return !d || d.offsetParent === null; })()';

async function fillDialog(values) {
  await browser.evaluate(`(() => {
    const inputs = [...document.querySelectorAll('.el-dialog input')];
    ${values.map(([index, value]) => `
    if (inputs[${index}]) { inputs[${index}].focus(); inputs[${index}].value = ${JSON.stringify(value)}; inputs[${index}].dispatchEvent(new Event('input', { bubbles: true })); }`).join('')}
    return true;
  })()`);
}

async function clickDialogButton(text) {
  return browser.evaluate(`(() => {
    const button = [...document.querySelectorAll('.el-dialog__footer button')].find((el) => el.textContent.includes(${JSON.stringify(text)}));
    if (!button) return false;
    button.click();
    return true;
  })()`);
}

try {
  await browser.goto(`${BASE}/employees`, { waitMs: 1800 });
  await browser.waitFor('document.querySelector(".employee-table")', 20000);
  await browser.waitFor('document.querySelector(".el-pagination")', 15000);
  check('employees route loads', true);

  const nav = await browser.evaluate(`document.querySelector('.app-nav')?.innerText || ''`);
  check('nav entry visible', nav.includes('员工管理'), nav.replace(/\s+/g, ' '));

  const code = `UI${Date.now()}`;
  const name = `界面烟测员工${Date.now()}`;

  const opened = await browser.evaluate(`(() => {
    const button = [...document.querySelectorAll('button')].find((el) => el.textContent.includes('新增员工'));
    if (!button) return false;
    button.click();
    return true;
  })()`);
  check('open create dialog', opened);
  await browser.waitFor(dialogOpen, 10000);
  const title = await browser.evaluate(`document.querySelector('.el-dialog__title')?.innerText || ''`);
  check('create dialog title', title.includes('新增员工'), title);
  const defaultStatus = await browser.evaluate(`(() => {
    const item = [...document.querySelectorAll('.el-dialog .el-form-item')].find((el) => el.innerText.includes('状态'));
    return item?.innerText || '';
  })()`);
  check('default status active', defaultStatus.includes('在职'), defaultStatus.replace(/\s+/g, ' '));

  await fillDialog([[0, code], [1, name]]);
  await clickDialogButton('保存');
  await browser.waitFor(dialogClosed, 20000);
  await browser.waitFor(`document.body.innerText.includes(${JSON.stringify(name)})`, 20000);
  check('created row visible', true);

  const openedDuplicate = await browser.evaluate(`(() => {
    const button = [...document.querySelectorAll('button')].find((el) => el.textContent.includes('新增员工'));
    if (!button) return false;
    button.click();
    return true;
  })()`);
  check('open duplicate dialog', openedDuplicate);
  await browser.waitFor(dialogOpen, 10000);
  await fillDialog([[0, code], [1, '重复工号员工']]);
  await clickDialogButton('保存');
  await browser.waitFor('document.querySelector(".el-dialog")?.innerText.includes("工号已存在")', 15000);
  const stillOpen = await browser.evaluate(dialogOpen);
  check('duplicate keeps dialog open with field error', Boolean(stillOpen));
  await clickDialogButton('取消');
  await browser.waitFor(dialogClosed, 10000);

  const openedEdit = await browser.evaluate(`(() => {
    const rows = [...document.querySelectorAll('.employee-table .el-table__row')];
    const row = rows.find((el) => el.innerText.includes(${JSON.stringify(name)}));
    if (!row) return false;
    const button = [...row.querySelectorAll('button')].find((el) => el.textContent.includes('编辑'));
    if (!button) return false;
    button.click();
    return true;
  })()`);
  check('open edit dialog', openedEdit);
  await browser.waitFor(dialogOpen, 10000);
  const editTitle = await browser.evaluate(`document.querySelector('.el-dialog__title')?.innerText || ''`);
  check('edit dialog title', editTitle.includes('编辑员工'), editTitle);
  const prefilled = await browser.evaluate(`(() => {
    const inputs = [...document.querySelectorAll('.el-dialog input')];
    return { empNo: inputs[0]?.value || '', name: inputs[1]?.value || '' };
  })()`);
  check('edit form prefilled', prefilled.empNo === code && prefilled.name === name,
    JSON.stringify(prefilled));

  const updatedName = `${name}改`;
  await fillDialog([[1, updatedName]]);
  await clickDialogButton('保存');
  await browser.waitFor(dialogClosed, 20000);
  await browser.waitFor(`document.body.innerText.includes(${JSON.stringify(updatedName)})`, 20000);
  check('updated row visible', true);

  const errors = browser.consoleLogs.filter((entry) => entry.type === 'error' && !/favicon/i.test(entry.text));
  if (browser.exceptions.length || errors.length) {
    console.error('browser errors:', browser.exceptions.join(' | '), errors.map((entry) => entry.text).join(' | '));
    failed = true;
  }
} catch (error) {
  console.error('Employee UI smoke failed:', error.message);
  failed = true;
} finally {
  await browser.close();
}
if (failed) process.exitCode = 1;
else console.log('Employee UI smoke: PASS');
