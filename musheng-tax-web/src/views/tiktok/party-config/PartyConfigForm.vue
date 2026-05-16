<template>
  <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" @finish="handleSave">
    <a-divider>买方信息</a-divider>
    <a-row :gutter="24">
      <a-col :span="12">
        <a-form-item label="买方中文名" name="buyerName"><a-input v-model:value="form.buyerName" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="买方英文名" name="buyerNameEn"><a-input v-model:value="form.buyerNameEn" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="买方地址" name="buyerAddress"><a-input v-model:value="form.buyerAddress" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="买方电话" name="buyerPhone"><a-input v-model:value="form.buyerPhone" /></a-form-item>
      </a-col>
    </a-row>

    <a-divider>卖方信息</a-divider>
    <a-row :gutter="24">
      <a-col :span="12">
        <a-form-item label="卖方名称" name="sellerName"><a-input v-model:value="form.sellerName" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="卖方地址" name="sellerAddress"><a-input v-model:value="form.sellerAddress" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="卖方电话" name="sellerPhone"><a-input v-model:value="form.sellerPhone" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="供应商名称" name="supplierName"><a-input v-model:value="form.supplierName" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="客户繁体名" name="customerNameTc"><a-input v-model:value="form.customerNameTc" /></a-form-item>
      </a-col>
    </a-row>

    <a-divider>银行信息</a-divider>
    <a-row :gutter="24">
      <a-col :span="12">
        <a-form-item label="账户名" name="bankAccountName"><a-input v-model:value="form.bankAccountName" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="账号" name="bankAccountNumber"><a-input v-model:value="form.bankAccountNumber" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="银行名称" name="bankName"><a-input v-model:value="form.bankName" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="银行地址" name="bankAddress"><a-input v-model:value="form.bankAddress" /></a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="SWIFT代码" name="swiftCode"><a-input v-model:value="form.swiftCode" /></a-form-item>
      </a-col>
    </a-row>

    <a-form-item :wrapper-col="{ offset: 6, span: 16 }">
      <a-button type="primary" html-type="submit" :loading="saving">保存配置</a-button>
    </a-form-item>
  </a-form>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { saveTiktokPartyConfig, type TiktokPartyConfig } from '@/api/tiktok'

const props = defineProps<{ siteCode: string; config?: TiktokPartyConfig }>()
const emit = defineEmits(['saved'])
const saving = ref(false)

const form = ref<TiktokPartyConfig>({
  siteCode: props.siteCode,
  buyerName: '', buyerAddress: '', buyerPhone: '', buyerNameEn: '',
  sellerName: '', sellerAddress: '', sellerPhone: '',
  supplierName: '', customerNameTc: '',
  bankAccountName: '', bankAccountNumber: '', bankName: '', bankAddress: '', swiftCode: '',
})

watch(() => props.config, (val) => {
  if (val) form.value = { ...val }
  else form.value = { ...form.value, siteCode: props.siteCode }
}, { immediate: true })

async function handleSave() {
  saving.value = true
  try {
    await saveTiktokPartyConfig(form.value)
    message.success('保存成功')
    emit('saved')
  } finally { saving.value = false }
}
</script>
