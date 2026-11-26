<template>
  <AdminShell title="用户管理">
    <section class="card">
      <div class="card-head">
        <div class="filter-bar">
          <div class="field" style="min-width:180px">
            <el-input v-model="filters.keyword" placeholder="搜索用户名 / 姓名 / 手机号" clearable @keyup.enter="load" />
          </div>
          <div class="field" style="min-width:130px">
            <el-select v-model="filters.role" clearable style="width:100%" @change="load">
              <el-option label="全部角色" value="" />
              <el-option v-for="(v, k) in ROLE" :key="k" :label="v.label" :value="k" />
            </el-select>
          </div>
          <div class="field" style="min-width:130px">
            <el-select v-model="filters.status" clearable style="width:100%" @change="load">
              <el-option label="全部状态" value="" /><el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" />
            </el-select>
          </div>
        </div>
        <div class="card-actions">
          <el-button type="primary" @click="openCreate"><AppIcon name="i-plus" :size="15" />&nbsp;新增用户</el-button>
        </div>
      </div>
      <div class="table-wrap">
        <table class="tbl">
          <thead>
            <tr>
              <th style="width:180px">用户</th>
              <th style="width:56px">性别</th>
              <th style="width:130px">手机号</th>
              <th style="min-width:180px">电子邮箱</th>
              <th style="width:110px">角色</th>
              <th style="width:120px">部门</th>
              <th style="width:80px">状态</th>
              <th style="width:110px">创建时间</th>
              <th style="width:150px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id">
              <td>
                <div class="u-cell">
                  <div class="avatar">{{ u.realName?.charAt(0) }}</div>
                  <div><div class="u-name">{{ u.username }}</div><div class="u-sub">{{ u.realName }}</div></div>
                </div>
              </td>
              <td>{{ u.gender === 'M' ? '男' : u.gender === 'F' ? '女' : '—' }}</td>
              <td>{{ u.phone || '—' }}</td>
              <td>{{ u.email || '—' }}</td>
              <td><span class="tag" :class="roleCls(u.role)"><span class="dot"></span>{{ roleLabel(u.role) }}</span></td>
              <td>{{ deptLabel(u.dept) }}</td>
              <td>
                <el-switch :model-value="u.enabled" @change="(v: any) => toggle(u, v)" />
              </td>
              <td>{{ u.createdAt }}</td>
              <td class="actions-cell">
                <button class="link-btn" @click="openEdit(u)">编辑</button>
                <button class="link-btn" @click="resetPwd(u)">重置密码</button>
                <button v-if="u.id !== userStore.user?.id" class="link-btn danger" @click="del(u)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="form-foot" style="position:static">
        <span class="form-foot-sum">共 {{ total }} 位用户</span>
        <span class="toolbar" style="margin-left:auto">
          <el-button size="small" :disabled="filters.page <= 1" @click="filters.page--; load()">上一页</el-button>
          <el-button size="small" :disabled="filters.page * filters.size >= total" @click="filters.page++; load()">下一页</el-button>
        </span>
      </div>
    </section>

    <!-- 新增/编辑弹窗：字段与 API.md §3.2 一致；单列布局，每行一个控件 -->
    <el-dialog v-model="dialog" :title="editingId ? '编辑用户' : '新增用户'" width="440px">
      <div class="modal-body">
        <div class="field"><label class="field-label">用户名 *</label><el-input v-model="form.username" :disabled="!!editingId" placeholder="登录账号，如 yinshua-04" /></div>
        <div class="field"><label class="field-label">{{ editingId ? '重置密码' : '初始密码 *' }}</label><el-input v-model="form.password" type="password" show-password placeholder="6 位以上，含字母和数字" /></div>
        <div class="field"><label class="field-label">姓名 * </label><el-input v-model="form.realName" placeholder="真实姓名（日报签名用）" /></div>
        <div class="field"><label class="field-label">性别</label>
          <el-select v-model="form.gender" style="width:100%"><el-option label="男" value="M" /><el-option label="女" value="F" /></el-select>
        </div>
        <div class="field"><label class="field-label">手机号</label><el-input v-model="form.phone" inputmode="numeric" placeholder="11 位手机号" /></div>
        <div class="field"><label class="field-label">电子邮箱</label><el-input v-model="form.email" placeholder="name@example.com" /></div>
        <div class="field"><label class="field-label">角色 *</label>
          <el-select v-model="form.role" style="width:100%" @change="form.dept = ['DEPT_LEAD','WORKER'].includes(form.role) ? form.dept : ''">
            <el-option v-for="(v, k) in ROLE" :key="k" :label="v.label" :value="k" />
          </el-select>
        </div>
        <div class="field"><label class="field-label">部门</label>
          <el-select v-model="form.dept" style="width:100%" :disabled="!['DEPT_LEAD','WORKER'].includes(form.role)">
            <el-option label="—" value="" />
            <el-option v-for="(v, k) in DEPT" :key="k" :label="v.label" :value="k" />
          </el-select>
        </div>
        <div class="field"><label class="field-label">住址</label><el-input v-model="form.address" placeholder="如：印刷车间 / 宿舍 3 号楼" /></div>
        <div class="field"><label class="field-label">状态</label>
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" inline-prompt style="--el-switch-on-color: var(--success)" />
        </div>
      </div>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>
  </AdminShell>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminShell from '@/components/AdminShell.vue'
import AppIcon from '@/components/AppIcon.vue'
import { api } from '@/api/http'
import { useUserStore } from '@/stores/user'
import { ROLE, DEPT } from '@/constants'

const userStore = useUserStore()
const users = ref<any[]>([])
const total = ref(0)
const filters = reactive({ keyword: '', role: '', status: '', page: 1, size: 20 })
const dialog = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive<any>({})

const roleLabel = (r: string) => (ROLE as any)[r]?.label ?? r
const roleCls = (r: string) => ({ ADMIN: 'tag-red', DEPT_LEAD: 'tag-blue', WORKER: 'tag-gray', MANAGEMENT: 'tag-teal' }[r] ?? 'tag-gray')
const deptLabel = (d: string) => d ? (DEPT as any)[d]?.label : '—'

async function load() {
  const data = await api.get('/users', {
    params: { page: filters.page, size: filters.size, keyword: filters.keyword || undefined, role: filters.role || undefined, status: filters.status || undefined }
  })
  users.value = data.records
  total.value = data.total
}

function openCreate() {
  Object.assign(form, { id: null, username: '', password: '', realName: '', gender: 'M', phone: '', email: '', address: '', role: 'WORKER', dept: 'PRINT', enabled: true })
  editingId.value = null
  dialog.value = true
}

function openEdit(u: any) {
  Object.assign(form, { ...u, password: '' })
  editingId.value = u.id
  dialog.value = true
}

async function saveUser() {
  // 前端必填校验（与后端 400 对齐）
  if (!form.username?.trim()) { ElMessage.warning('请填写用户名'); return }
  if (!editingId.value && !form.password?.trim()) { ElMessage.warning('请填写初始密码'); return }
  if (!form.realName?.trim()) { ElMessage.warning('请填写姓名'); return }
  if (form.phone && !/^1\d{10}$/.test(form.phone.replace(/\s/g, ''))) { ElMessage.warning('手机号格式不正确'); return }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(`/users/${editingId.value}`, { ...form })
      if (form.password) await api.put(`/users/${editingId.value}/password`, { newPassword: form.password })
      ElMessage.success(`用户「${form.realName}」已更新`)
    } else {
      await api.post('/users', { ...form })
      ElMessage.success(`用户「${form.realName}」创建成功，初始密码已生效`)
    }
    dialog.value = false
    load()
  } catch { /* 拦截器已提示 */ }
  saving.value = false
}

async function toggle(u: any, enabled: boolean) {
  if (!enabled) {
    await ElMessageBox.confirm(`停用后「${u.username}」将无法登录，确认停用？`, '停用账号', { type: 'warning' })
  }
  await api.put(`/users/${u.id}/status`, { enabled })
  ElMessage.success(enabled ? '已启用' : '已停用')
  load()
}

async function resetPwd(u: any) {
  const { value } = await ElMessageBox.prompt(`为「${u.username}」设置新密码（至少 6 位）`, '重置密码', { inputPattern: /.{6,}/, inputErrorMessage: '密码至少 6 位' })
  await api.put(`/users/${u.id}/password`, { newPassword: value })
  ElMessage.success('密码已重置')
}

async function del(u: any) {
  await ElMessageBox.confirm(`确认删除用户「${u.username}」？`, '删除', { type: 'warning' })
  await api.delete(`/users/${u.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(() => { load().catch(() => {}) })
</script>

<style scoped>
.modal-body { display: flex; flex-direction: column; gap: 4px; }
.modal-body .field { padding: 4px 0; }
.u-cell { display: flex; align-items: center; gap: 10px; }
.u-name { font-weight: 500; }
.u-sub { font-size: var(--fs-xs); color: var(--muted); margin-top: 1px; }
</style>
