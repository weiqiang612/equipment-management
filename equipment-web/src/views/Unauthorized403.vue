<template>
  <div class="err-container">
    <div class="err-content">
      <h1 class="err-code">403</h1>
      <h2 class="err-msg">抱歉，您无权访问此页面</h2>
      <p class="err-desc">当前用户的权限等级不足以查看此系统资源，请联系管理员或切换账号。</p>
      <div class="err-actions">
        <el-button type="primary" size="medium" @click="goHome">返回首页</el-button>
        <el-button size="medium" @click="logout">退出并切换账号</el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Unauthorized403',
  methods: {
    goHome() {
      this.$router.push('/')
    },
    logout() {
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('realName')
      localStorage.removeItem('username')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.err-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 80vh;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  text-align: center;
}

.err-content {
  max-width: 500px;
  padding: 30px;
}

.err-code {
  font-size: 120px;
  font-weight: 800;
  margin: 0;
  color: #f56c6c;
  line-height: 1;
  letter-spacing: -2px;
}

.err-msg {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin: 20px 0 10px 0;
}

.err-desc {
  font-size: 15px;
  color: #909399;
  margin: 0 0 30px 0;
  line-height: 1.6;
}

.err-actions .el-button {
  margin: 0 10px;
}
</style>
