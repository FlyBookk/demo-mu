<template>
  <div class="page-container">
    <div class="page-header">
      <h3>TK 交易方配置</h3>
      <p class="page-desc">配置各站点的买卖方信息，生成单据时自动填充</p>
    </div>

    <a-card :bordered="false">
      <a-tabs v-model:activeKey="activeSite">
        <a-tab-pane v-for="s in sites" :key="s.siteCode" :tab="s.siteCode + ' 站'">
          <PartyConfigForm :site-code="s.siteCode" :config="configs[s.siteCode]" @saved="loadConfigs" />
        </a-tab-pane>
      </a-tabs>
      <a-empty v-if="!sites.length && !loading" description="暂无站点配置，请先在系统配置中添加站点" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTiktokPartyConfigList, type TiktokPartyConfig } from '@/api/tiktok'
import { useTiktokSites } from '@/composables/tiktok/useTiktokSites'
import PartyConfigForm from './PartyConfigForm.vue'

const { sites, loading } = useTiktokSites()
const activeSite = ref('')
const configs = ref<Record<string, TiktokPartyConfig | undefined>>({})

async function loadConfigs() {
  const res: any = await getTiktokPartyConfigList()
  const list: TiktokPartyConfig[] = res.data || res || []
  configs.value = {}
  list.forEach(c => { configs.value[c.siteCode] = c })
  if (!activeSite.value && sites.value.length) {
    activeSite.value = sites.value[0].siteCode
  }
}

onMounted(loadConfigs)
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header { margin-bottom: 16px; }
.page-desc { color: #666; font-size: 13px; margin-top: 4px; }
</style>
