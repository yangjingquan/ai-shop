<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  wechatSettingsApi,
  type MerchantWechatSettingsVO,
} from '@/api/wechat-settings'

const loading = ref(false)
const list = ref<MerchantWechatSettingsVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const formRef = ref<FormInstance>()
const current = ref<MerchantWechatSettingsVO | null>(null)
const form = reactive({
  wxAppId: '',
  wxSecret: '',
  wxMchId: '',
  wxPayApiV3Key: '',
  wxPayMchSerialNo: '',
  wxPayPrivateKey: '',
  wxPayPublicKey: '',
  wxPayPublicKeyId: '',
  wxPayNotifyUrl: '',
  wxPayEnabled: 0,
})

const rules: FormRules = {
  wxMchId: [{ pattern: /^$|^[0-9]{6,32}$/, message: '微信支付商户号需为 6-32 位数字', trigger: 'blur' }],
}

async function fetchList() {
  loading.value = true
  try {
    const data = await wechatSettingsApi.page({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  fetchList()
}

async function openEdit(row: MerchantWechatSettingsVO) {
  dialogLoading.value = true
  try {
    current.value = await wechatSettingsApi.get(row.merchantId)
    form.wxAppId = current.value.wxAppId ?? ''
    form.wxSecret = ''
    form.wxMchId = current.value.wxMchId ?? ''
    form.wxPayApiV3Key = ''
    form.wxPayMchSerialNo = current.value.wxPayMchSerialNo ?? ''
    form.wxPayPrivateKey = ''
    form.wxPayPublicKey = ''
    form.wxPayPublicKeyId = current.value.wxPayPublicKeyId ?? ''
    form.wxPayNotifyUrl = current.value.wxPayNotifyUrl ?? ''
    form.wxPayEnabled = current.value.wxPayEnabled ?? 0
    dialogVisible.value = true
  } finally {
    dialogLoading.value = false
  }
}

async function submit() {
  if (!current.value || !formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  dialogLoading.value = true
  try {
    await wechatSettingsApi.update(current.value.merchantId, {
      wxAppId: form.wxAppId || undefined,
      wxSecret: form.wxSecret || undefined,
      wxMchId: form.wxMchId,
      wxPayApiV3Key: form.wxPayApiV3Key || undefined,
      wxPayMchSerialNo: form.wxPayMchSerialNo,
      wxPayPrivateKey: form.wxPayPrivateKey || undefined,
      wxPayPublicKey: form.wxPayPublicKey || undefined,
      wxPayPublicKeyId: form.wxPayPublicKeyId,
      wxPayNotifyUrl: form.wxPayNotifyUrl,
      wxPayEnabled: form.wxPayEnabled,
    })
    ElMessage.success('微信设置保存成功')
    dialogVisible.value = false
    await fetchList()
  } finally {
    dialogLoading.value = false
  }
}

function statusType(row: MerchantWechatSettingsVO) {
  if (row.wxPayConfigured) return 'success'
  if (row.wxPayEnabled === 1) return 'warning'
  return 'info'
}

function statusText(row: MerchantWechatSettingsVO) {
  if (row.wxPayConfigured) return '已启用'
  if (row.wxPayEnabled === 1) return '配置不完整'
  return '未启用'
}

onMounted(fetchList)
</script>

<template>
  <div class="wechat-settings">
    <div class="page-header">
      <div>
        <span class="page-kicker">WECHAT SETTINGS</span>
        <h1 class="page-title">微信设置</h1>
        <p class="page-desc">按商户维护小程序和微信支付配置，密钥类字段不会回显。</p>
      </div>
    </div>

    <el-card>
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="按商家名称或商户代码搜索"
          clearable
          style="width: 280px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-button type="primary" @click="onSearch">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="merchantName" label="商家名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="merchantCode" label="商户代码" width="130" />
        <el-table-column label="AppID" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.wxAppId || '未配置' }}</template>
        </el-table-column>
        <el-table-column label="小程序密钥" width="100">
          <template #default="{ row }">
            <el-tag :type="(row as MerchantWechatSettingsVO).wxSecretConfigured ? 'success' : 'info'">
              {{ (row as MerchantWechatSettingsVO).wxSecretConfigured ? '已配置' : '未配置' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row as MerchantWechatSettingsVO)">{{ statusText(row as MerchantWechatSettingsVO) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回调地址" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.wxPayNotifyUrl || '未配置' }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="150" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :loading="dialogLoading" @click="openEdit(row as MerchantWechatSettingsVO)">配置</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next"
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="`微信设置 · ${current?.merchantName || ''}`"
      width="720px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-divider content-position="left">微信小程序配置</el-divider>
        <el-form-item label="小程序 AppID">
          <el-input v-model="form.wxAppId" maxlength="64" clearable />
        </el-form-item>
        <el-form-item label="小程序 AppSecret">
          <el-input
            v-model="form.wxSecret"
            show-password
            maxlength="128"
            :placeholder="current?.wxSecretConfigured ? '留空则保持原值' : '请输入 AppSecret'"
          />
          <div v-if="current?.wxSecretConfigured" class="hint">当前已配置 AppSecret，留空不会覆盖。</div>
        </el-form-item>

        <el-divider content-position="left">微信支付配置</el-divider>
        <el-form-item label="微信支付商户号" prop="wxMchId">
          <el-input v-model="form.wxMchId" maxlength="32" clearable />
        </el-form-item>
        <el-form-item label="API v3 密钥">
          <el-input
            v-model="form.wxPayApiV3Key"
            show-password
            maxlength="128"
            :placeholder="current?.wxPayApiV3KeyConfigured ? '留空则保持原值' : '请输入 32 位 API v3 密钥'"
          />
          <div class="hint">保存后以 AES-GCM 密文存储，编辑时留空不会覆盖。</div>
        </el-form-item>
        <el-form-item label="证书序列号">
          <el-input v-model="form.wxPayMchSerialNo" maxlength="128" clearable />
        </el-form-item>
        <el-form-item label="商户私钥 PEM">
          <el-input
            v-model="form.wxPayPrivateKey"
            type="textarea"
            :rows="6"
            :placeholder="current?.wxPayPrivateKeyConfigured ? '留空则保持原值' : '请输入 apiclient_key.pem 内容'"
          />
          <div class="hint">保存后以 AES-GCM 密文存储，编辑时留空不会覆盖。</div>
        </el-form-item>
        <el-form-item label="微信支付公钥 ID">
          <el-input v-model="form.wxPayPublicKeyId" maxlength="64" clearable placeholder="请输入微信支付公钥 ID" />
        </el-form-item>
        <el-form-item label="微信支付公钥">
          <el-input
            v-model="form.wxPayPublicKey"
            type="textarea"
            :rows="6"
            maxlength="8192"
            :placeholder="current?.wxPayPublicKeyConfigured ? '留空则保持原值' : '请输入微信支付公钥 PEM 内容'"
          />
          <div class="hint">使用微信支付平台公钥验签；保存后以 AES-GCM 密文存储，编辑时留空不会覆盖。</div>
        </el-form-item>
        <el-form-item label="支付回调地址">
          <el-input
            v-model="form.wxPayNotifyUrl"
            maxlength="255"
            clearable
            :placeholder="`https://域名/api/callback/wxpay/${current?.merchantCode || '商户代码'}`"
          />
          <div class="hint">必须为无参数 HTTPS 地址，路径必须为 /api/callback/wxpay/{{ current?.merchantCode || '商户代码' }}。</div>
        </el-form-item>
        <el-form-item label="启用微信支付">
          <el-switch v-model="form.wxPayEnabled" :active-value="1" :inactive-value="0" />
          <span class="switch-hint">启用时会校验完整支付配置。</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="submit">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.hint,
.switch-hint {
  color: var(--shop-text-muted);
  font-size: 12px;
}

.hint {
  margin-top: 6px;
}

.switch-hint {
  margin-left: 10px;
}
</style>
