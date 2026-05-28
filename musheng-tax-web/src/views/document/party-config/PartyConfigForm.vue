<template>
  <a-form :model="formData" :rules="rules" ref="formRef" layout="vertical">
    <a-form-item label="站点代码" name="siteCode">
      <a-select
        v-if="!isEdit"
        v-model:value="formData.siteCode"
        placeholder="请选择站点"
        :options="siteOptions"
      />
      <a-input v-else :value="formData.siteCode" disabled />
    </a-form-item>

    <a-divider orientation="left">买方信息</a-divider>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="买方中文名" name="buyerName">
          <a-input v-model:value="formData.buyerName" placeholder="请输入买方中文名" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="买方英文名" name="buyerNameEn">
          <a-input v-model:value="formData.buyerNameEn" placeholder="请输入买方英文名" />
        </a-form-item>
      </a-col>
      <a-col :span="16">
        <a-form-item label="买方地址" name="buyerAddress">
          <a-input v-model:value="formData.buyerAddress" placeholder="请输入买方地址" />
        </a-form-item>
      </a-col>
      <a-col :span="8">
        <a-form-item label="买方电话" name="buyerPhone">
          <a-input v-model:value="formData.buyerPhone" placeholder="请输入买方电话" />
        </a-form-item>
      </a-col>
    </a-row>

    <a-divider orientation="left">卖方信息</a-divider>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="卖方名称" name="sellerName">
          <a-input v-model:value="formData.sellerName" placeholder="请输入卖方名称" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="卖方电话" name="sellerPhone">
          <a-input v-model:value="formData.sellerPhone" placeholder="请输入卖方电话" />
        </a-form-item>
      </a-col>
      <a-col :span="24">
        <a-form-item label="卖方地址" name="sellerAddress">
          <a-input v-model:value="formData.sellerAddress" placeholder="请输入卖方地址" />
        </a-form-item>
      </a-col>
    </a-row>

    <a-divider orientation="left">供应商 / 客户</a-divider>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="供应商名称" name="supplierName">
          <a-input v-model:value="formData.supplierName" placeholder="请输入供应商名称" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="客户繁体名" name="customerNameTc">
          <a-input v-model:value="formData.customerNameTc" placeholder="請輸入客戶繁體名" />
        </a-form-item>
      </a-col>
    </a-row>

    <a-divider orientation="left">银行信息</a-divider>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="银行账户名" name="bankAccountName">
          <a-input v-model:value="formData.bankAccountName" placeholder="请输入银行账户名" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="银行账号" name="bankAccountNumber">
          <a-input v-model:value="formData.bankAccountNumber" placeholder="请输入银行账号" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="银行名称" name="bankName">
          <a-input v-model:value="formData.bankName" placeholder="请输入银行名称" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="SWIFT代码" name="swiftCode">
          <a-input v-model:value="formData.swiftCode" placeholder="请输入SWIFT代码" />
        </a-form-item>
      </a-col>
      <a-col :span="24">
        <a-form-item label="银行地址" name="bankAddress">
          <a-input v-model:value="formData.bankAddress" placeholder="请输入银行地址" />
        </a-form-item>
      </a-col>
    </a-row>

    <div class="form-footer">
      <a-space>
        <a-button @click="emit('cancel')">取消</a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">保存</a-button>
      </a-space>
    </div>
  </a-form>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { addPartyConfig, updatePartyConfig } from '@/api/documentPartyConfig'
import type { DocumentPartyConfig } from '@/api/documentPartyConfig'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

const props = defineProps<{
  config: DocumentPartyConfig
  isEdit?: boolean
}>()

const emit = defineEmits<{
  (e: 'saved'): void
  (e: 'cancel'): void
}>()

const siteOptions = ref<{ label: string; value: string }[]>([])

async function fetchSiteOptions() {
  try {
    const res = await getEnabledMarketplaces() as any
    const list: Marketplace[] = res?.data ?? res ?? []
    siteOptions.value = list.map(m => ({ label: m.siteCode, value: m.siteCode }))
  } catch {
    siteOptions.value = []
  }
}

onMounted(fetchSiteOptions)

const formRef = ref<FormInstance>()
const saving = ref(false)
const formData = ref<DocumentPartyConfig>({ ...props.config })

watch(() => props.config, (val) => {
  formData.value = { ...val }
}, { deep: true })

const rules = {
  siteCode: [{ required: true, message: '请选择站点代码' }],
  buyerName: [{ required: true, message: '请输入买方中文名' }],
  sellerName: [{ required: true, message: '请输入卖方名称' }],
  supplierName: [{ required: true, message: '请输入供应商名称' }],
  customerNameTc: [{ required: true, message: '請輸入客戶繁體名' }]
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    // 表单校验失败，ant-design-vue 会自动高亮错误字段
    return
  }
  saving.value = true
  try {
    if (props.isEdit) {
      await updatePartyConfig({ ...formData.value })
    } else {
      await addPartyConfig({ ...formData.value })
    }
    message.success('保存成功')
    emit('saved')
  } catch (err: any) {
    // 后端返回的错误信息（如"该站点配置已存在"）
    const msg = err?.response?.data?.message || err?.message || '保存失败，请重试'
    message.error(msg)
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.form-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid $border-color;
  display: flex;
  justify-content: flex-end;
}
</style>
