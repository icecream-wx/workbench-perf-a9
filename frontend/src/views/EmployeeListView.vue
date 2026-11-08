<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  ElButton,
  ElDatePicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElPagination,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import PageHeader from '../components/PageHeader.vue';
import {
  createEmployee,
  listEmployees,
  updateEmployee,
  type Employee,
  type EmployeePayload,
  type EmployeeStatus,
} from '../api/employee';

interface EmployeeForm {
  empNo: string;
  name: string;
  department: string;
  position: string;
  phone: string;
  email: string;
  hireDate: string;
  status: EmployeeStatus;
}

type DialogMode = 'create' | 'edit';

const FIELD_NAMES = [
  'empNo',
  'name',
  'department',
  'position',
  'phone',
  'email',
  'hireDate',
  'status',
] as const;

const PHONE_PATTERN = /^[0-9+\-\s]*$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const router = useRouter();

const pageSizes = [10, 20, 50];
const statusOptions: Array<{ value: EmployeeStatus; label: string }> = [
  { value: 'ACTIVE', label: '在职' },
  { value: 'INACTIVE', label: '停用' },
];

const keyword = ref('');
const appliedKeyword = ref('');
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);
const items = ref<Employee[]>([]);
const loading = ref(false);
const loadError = ref<string | null>(null);
let loadSeq = 0;

const dialogVisible = ref(false);
const dialogMode = ref<DialogMode>('create');
const editingId = ref<number | null>(null);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<EmployeeForm>(emptyForm());
const serverErrors = reactive<Record<string, string>>({});

const rules: FormRules<EmployeeForm> = {
  empNo: [
    { required: true, whitespace: true, message: '工号不能为空', trigger: 'blur' },
    { max: 32, message: '工号长度不能超过32个字符', trigger: 'blur' },
  ],
  name: [
    { required: true, whitespace: true, message: '姓名不能为空', trigger: 'blur' },
    { max: 64, message: '姓名长度不能超过64个字符', trigger: 'blur' },
  ],
  department: [{ max: 64, message: '部门长度不能超过64个字符', trigger: 'blur' }],
  position: [{ max: 64, message: '职位长度不能超过64个字符', trigger: 'blur' }],
  phone: [
    { max: 32, message: '手机号长度不能超过32个字符', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (!value || PHONE_PATTERN.test(value)) {
          callback();
          return;
        }
        callback(new Error('手机号只能包含数字、+、- 或空格'));
      },
      trigger: 'blur',
    },
  ],
  email: [
    { max: 128, message: '邮箱长度不能超过128个字符', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (!value || EMAIL_PATTERN.test(value)) {
          callback();
          return;
        }
        callback(new Error('邮箱格式不正确'));
      },
      trigger: 'blur',
    },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

function emptyForm(): EmployeeForm {
  return {
    empNo: '',
    name: '',
    department: '',
    position: '',
    phone: '',
    email: '',
    hireDate: '',
    status: 'ACTIVE',
  };
}

function display(value: string | null | undefined): string {
  return value && value.trim() !== '' ? value : '-';
}

function statusLabel(value: string | null | undefined): string {
  if (value === 'ACTIVE') return '在职';
  if (value === 'INACTIVE') return '停用';
  return display(value);
}

function statusTagType(value: string | null | undefined): 'success' | 'info' {
  return value === 'ACTIVE' ? 'success' : 'info';
}

function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed === '' ? null : trimmed;
}

function toPayload(): EmployeePayload {
  return {
    empNo: form.empNo.trim(),
    name: form.name.trim(),
    department: blankToNull(form.department),
    position: blankToNull(form.position),
    phone: blankToNull(form.phone),
    email: blankToNull(form.email),
    hireDate: blankToNull(form.hireDate),
    status: form.status,
  };
}

function clearServerErrors(): void {
  for (const field of FIELD_NAMES) {
    delete serverErrors[field];
  }
}

function clearServerError(field: (typeof FIELD_NAMES)[number]): void {
  delete serverErrors[field];
}

function applyServerError(message: string): void {
  let matched = false;
  for (const segment of message.split(';')) {
    const trimmed = segment.trim();
    const separator = trimmed.indexOf(': ');
    if (separator <= 0) continue;
    const field = trimmed.slice(0, separator);
    if ((FIELD_NAMES as readonly string[]).includes(field)) {
      serverErrors[field] = trimmed.slice(separator + 2);
      matched = true;
    }
  }
  if (!matched && message.includes('工号已存在')) {
    serverErrors.empNo = message;
  }
}

async function load(): Promise<void> {
  const seq = ++loadSeq;
  loading.value = true;
  loadError.value = null;
  try {
    const result = await listEmployees({
      page: page.value,
      size: pageSize.value,
      keyword: appliedKeyword.value === '' ? undefined : appliedKeyword.value,
    });
    if (seq !== loadSeq) return;
    items.value = result.items;
    total.value = result.total;
  } catch (error) {
    if (seq !== loadSeq) return;
    const message = error instanceof Error ? error.message : '加载员工列表失败';
    loadError.value = message;
    ElMessage.error(message);
  } finally {
    if (seq === loadSeq) {
      loading.value = false;
    }
  }
}

function search(): void {
  appliedKeyword.value = keyword.value.trim();
  page.value = 1;
  void load();
}

function reset(): void {
  keyword.value = '';
  appliedKeyword.value = '';
  page.value = 1;
  void load();
}

function handleCurrentChange(current: number): void {
  page.value = current;
  void load();
}

function handleSizeChange(size: number): void {
  pageSize.value = size;
  page.value = 1;
  void load();
}

function openCreate(): void {
  dialogMode.value = 'create';
  editingId.value = null;
  Object.assign(form, emptyForm());
  clearServerErrors();
  dialogVisible.value = true;
  void nextTick(() => formRef.value?.clearValidate());
}

function openEditFromRow(row: unknown): void {
  openEdit(row as Employee);
}

function openEdit(row: Employee): void {
  dialogMode.value = 'edit';
  editingId.value = row.id;
  Object.assign(form, {
    empNo: row.empNo ?? '',
    name: row.name ?? '',
    department: row.department ?? '',
    position: row.position ?? '',
    phone: row.phone ?? '',
    email: row.email ?? '',
    hireDate: row.hireDate ?? '',
    status: (row.status === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE') as EmployeeStatus,
  });
  clearServerErrors();
  dialogVisible.value = true;
  void nextTick(() => formRef.value?.clearValidate());
}

function handleDialogClosed(): void {
  formRef.value?.clearValidate();
  clearServerErrors();
  submitting.value = false;
}

async function submit(): Promise<void> {
  if (submitting.value) {
    return;
  }
  const instance = formRef.value;
  if (!instance) {
    return;
  }
  submitting.value = true;
  clearServerErrors();
  try {
    const valid = await instance.validate().catch(() => false);
    if (!valid) {
      return;
    }
    const payload = toPayload();
    if (dialogMode.value === 'create') {
      await createEmployee(payload);
      ElMessage.success('新增成功');
      dialogVisible.value = false;
      const lastPage = Math.max(1, Math.ceil((total.value + 1) / pageSize.value));
      page.value = lastPage;
      await load();
      return;
    }
    if (editingId.value === null) {
      return;
    }
    await updateEmployee(editingId.value, payload);
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await load();
  } catch (error) {
    const message = error instanceof Error ? error.message : '提交失败';
    applyServerError(message);
    ElMessage.error(message);
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  void load();
});
</script>

<template>
  <main class="page employee-page">
    <PageHeader title="员工管理" subtitle="维护员工基础信息，支持按工号或姓名检索。">
      <template #actions>
        <ElButton @click="router.push({ name: 'home' })">返回首页</ElButton>
      </template>
    </PageHeader>

    <section class="card">
      <div class="toolbar">
        <ElInput
          v-model="keyword"
          class="toolbar-search"
          placeholder="工号/姓名"
          clearable
          @keyup.enter="search"
        />
        <ElButton type="primary" :loading="loading" @click="search">查询</ElButton>
        <ElButton :disabled="loading" @click="reset">重置</ElButton>
        <span class="toolbar-spacer" />
        <ElButton type="primary" :disabled="loading" @click="openCreate">新增员工</ElButton>
      </div>

      <div v-if="loadError" class="load-error">
        <p class="error">{{ loadError }}</p>
        <ElButton @click="load">重试</ElButton>
      </div>

      <ElTable
        v-loading="loading"
        :data="items"
        border
        stripe
        empty-text="暂无数据"
        class="employee-table"
      >
        <ElTableColumn prop="empNo" label="工号" min-width="110" />
        <ElTableColumn prop="name" label="姓名" min-width="100" />
        <ElTableColumn label="部门" min-width="110">
          <template #default="scope">{{ display(scope.row.department) }}</template>
        </ElTableColumn>
        <ElTableColumn label="职位" min-width="110">
          <template #default="scope">{{ display(scope.row.position) }}</template>
        </ElTableColumn>
        <ElTableColumn label="手机号" min-width="130">
          <template #default="scope">{{ display(scope.row.phone) }}</template>
        </ElTableColumn>
        <ElTableColumn label="邮箱" min-width="170">
          <template #default="scope">{{ display(scope.row.email) }}</template>
        </ElTableColumn>
        <ElTableColumn label="入职日期" min-width="120">
          <template #default="scope">{{ display(scope.row.hireDate) }}</template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="90">
          <template #default="scope">
            <ElTag :type="statusTagType(scope.row.status)" effect="light">
              {{ statusLabel(scope.row.status) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="90" fixed="right">
          <template #default="scope">
            <ElButton link type="primary" @click="openEditFromRow(scope.row)">编辑</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>

      <div class="pager">
        <ElPagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :current-page="page"
          :page-size="pageSize"
          :page-sizes="pageSizes"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <ElDialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增员工' : '编辑员工'"
      width="520px"
      :close-on-click-modal="false"
      :close-on-press-escape="!submitting"
      :show-close="!submitting"
      @closed="handleDialogClosed"
    >
      <ElForm ref="formRef" :model="form" :rules="rules" label-width="88px" @submit.prevent>
        <ElFormItem label="工号" prop="empNo" :error="serverErrors.empNo">
          <ElInput v-model="form.empNo" placeholder="请输入工号" @input="clearServerError('empNo')" />
        </ElFormItem>
        <ElFormItem label="姓名" prop="name" :error="serverErrors.name">
          <ElInput v-model="form.name" placeholder="请输入姓名" @input="clearServerError('name')" />
        </ElFormItem>
        <ElFormItem label="部门" prop="department" :error="serverErrors.department">
          <ElInput
            v-model="form.department"
            placeholder="请输入部门"
            @input="clearServerError('department')"
          />
        </ElFormItem>
        <ElFormItem label="职位" prop="position" :error="serverErrors.position">
          <ElInput
            v-model="form.position"
            placeholder="请输入职位"
            @input="clearServerError('position')"
          />
        </ElFormItem>
        <ElFormItem label="手机号" prop="phone" :error="serverErrors.phone">
          <ElInput
            v-model="form.phone"
            placeholder="请输入手机号"
            @input="clearServerError('phone')"
          />
        </ElFormItem>
        <ElFormItem label="邮箱" prop="email" :error="serverErrors.email">
          <ElInput v-model="form.email" placeholder="请输入邮箱" @input="clearServerError('email')" />
        </ElFormItem>
        <ElFormItem label="入职日期" prop="hireDate" :error="serverErrors.hireDate">
          <ElDatePicker
            v-model="form.hireDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择入职日期"
            style="width: 100%"
            @change="clearServerError('hireDate')"
          />
        </ElFormItem>
        <ElFormItem label="状态" prop="status" :error="serverErrors.status">
          <ElSelect
            v-model="form.status"
            placeholder="请选择状态"
            style="width: 100%"
            @change="clearServerError('status')"
          >
            <ElOption
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton :disabled="submitting" @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="submitting" @click="submit">保存</ElButton>
      </template>
    </ElDialog>
  </main>
</template>

<style scoped>
.employee-page {
  width: min(1280px, 100%);
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.toolbar-search {
  width: 220px;
}

.toolbar-spacer {
  flex: 1 1 auto;
}

.employee-table {
  width: 100%;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.load-error {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 8px;
}

.load-error p {
  margin: 0;
}

@media (max-width: 640px) {
  .toolbar-search {
    width: 100%;
  }
}
</style>
