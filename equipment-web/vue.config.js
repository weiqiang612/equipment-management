const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false, 
    devServer: {
      port: 3000,
      client: {
        overlay: {
          warnings: false,
          errors: false
        }
      },
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          pathRewrite: {
            '^/api': '' // 转发给后端时去掉 /api
          }
        }
      }
    }

})
