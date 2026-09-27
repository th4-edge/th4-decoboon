import fs from 'node:fs'
const root='/private/tmp/th4-decoboon-web-remote'
const read=(p)=>fs.readFileSync(`${root}/${p}`,'utf8')
const view=read('src/views/ai/AiCenterView.vue')
const api=read('src/domains/ai/api.ts')
const model=read('src/domains/ai/model.ts')
const router=read('src/router/index.ts')
const nav=read('src/config/navigation.ts')
const perm=read('src/roles/permission.ts')
const results=[
 {id:'route-and-boss-capability',status:(()=>{const nonBoss=['storeManager','teamLeader','marketer','designer','pm','projectSupport','finance'].some(role=>{const idx=perm.indexOf(role+': [');if(idx<0)return false;const end=perm.indexOf('\n  ],',idx);const block=perm.slice(idx,end<0?idx+500:end);return block.includes("'ai'")});return router.includes("path: 'ai/center'")&&router.includes("capabilities: ['ai']")&&!nonBoss})()?'PASS':'FAIL'},
 {id:'usage-filters-and-pagination',status:view.includes('el-date-picker')&&view.includes('v-model="storeId"')&&view.includes('v-model="capabilityCode"')&&view.includes('el-pagination')&&api.includes('totalElements')&&api.includes('current: query.current')?'PASS':'FAIL'},
 {id:'no-platform-cost-or-prompt-ui',status:!/platformCost|prompt|客户正文/.test(model)&&view.includes('不展示提示词、客户正文、平台成本')?'PASS':'FAIL'},
 {id:'manual-brief-generation-only',status:/onMounted\(\(\) => \{[\s\S]*?loadBrief\(\)/.test(view)?'FAIL':'PASS',detail:'onMounted 不得自动调用 loadBrief；必须由 BOSS 点击生成'},
 {id:'pre-generation-estimated-points',status:/预计[^\n]*(点|消耗)|estimated/i.test(view)?'PASS':'FAIL',detail:'生成前必须展示预计资源/影子点数'},
 {id:'source-drawer-has-source-records',status:model.includes('sources')||/来源/.test(view.match(/<el-drawer[\s\S]*?<\/el-drawer>/)?.[0]||'')?'PASS':'FAIL',detail:'来源抽屉必须展示 Provider/对象/版本/更新时间'},
 {id:'repeat-click-guard',status:view.includes(':loading="briefLoading"')?'PASS':'FAIL'},
 {id:'brief-results-do-not-show-pseudo-margin',status:!/毛利率|毛利润|净利润/.test(view)?'PASS':'FAIL'}
]
console.log(JSON.stringify({request:'REQ-20260926-AI-USAGE-BRIEF-ACCEPTANCE',results},null,2))
if(results.some(x=>x.status==='FAIL'))process.exitCode=1
