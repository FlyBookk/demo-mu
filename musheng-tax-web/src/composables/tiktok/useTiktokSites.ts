import { ref, onMounted } from 'vue'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

/**
 * TK 站点列表 composable
 * 复用公共 Marketplace 服务获取站点，不自己搞一套
 */
export function useTiktokSites() {
  const sites = ref<Marketplace[]>([])
  const siteCodes = ref<string[]>([])
  const currentSite = ref<string>('')
  const loading = ref(false)

  async function loadSites() {
    loading.value = true
    try {
      const res: any = await getEnabledMarketplaces()
      sites.value = res.data || res || []
      siteCodes.value = sites.value.map(s => s.siteCode)
      if (siteCodes.value.length > 0 && !currentSite.value) {
        currentSite.value = siteCodes.value.includes('US') ? 'US' : siteCodes.value[0]
      }
    } finally {
      loading.value = false
    }
  }

  onMounted(loadSites)

  return { sites, siteCodes, currentSite, loading, loadSites }
}
