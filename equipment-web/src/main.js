import Vue from 'vue'
import App from './App.vue'
import router from './router' // 引入路由
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'

Vue.use(ElementUI)
Vue.config.productionTip = false


new Vue({
  render: h => h(App),
  router // 注入路由
}).$mount('#app')
