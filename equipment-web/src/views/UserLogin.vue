<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 左侧背景插图 -->
      <div class="login-left"></div>

      <!-- 右侧表单区域 -->
      <div class="login-right">
        <div class="brand-header">
          <h1 class="brand-title">EquipTrack</h1>
          <p class="brand-subtitle">Enterprise Equipment Management</p>
        </div>

        <h2 class="form-title">Log In to Your Account</h2>

        <el-form
          ref="loginForm"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          class="login-form"
          hide-required-asterisk
          @keyup.enter.native="handleLogin"
        >
          <el-form-item label="Username" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="Enter your username"
              prefix-icon="el-icon-user"
              clearable
            ></el-input>
          </el-form-item>

          <el-form-item prop="password">
            <template slot="label">
              <div class="password-label-wrapper">
                <span class="label-text">Password</span>
                <a href="javascript:void(0)" class="forgot-link" @click="handleHelp">Forgot Password?</a>
              </div>
            </template>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="Enter your password"
              prefix-icon="el-icon-lock"
              show-password
              clearable
            ></el-input>
          </el-form-item>

          <el-form-item class="submit-item">
            <el-button
              :loading="loading"
              type="success"
              class="login-btn"
              @click="handleLogin"
            >
              Log In
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <router-link to="/register" class="signup-link">Sign Up</router-link>
          <span class="divider">|</span>
          <a href="javascript:void(0)" class="help-link" @click="handleHelp">Need Help?</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { login } from '@/api/user'

export default {
  name: 'UserLogin',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      loginRules: {
        username: [
          { required: true, message: 'Please enter your username', trigger: 'blur' },
          { min: 3, max: 20, message: 'Length must be between 3 and 20 characters', trigger: 'blur' }
        ],
        password: [
          { required: true, message: 'Please enter your password', trigger: 'blur' },
          { min: 6, max: 20, message: 'Length must be between 6 and 20 characters', trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  methods: {
    decodeToken(token) {
      try {
        const payload = token.split('.')[1]
        const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
        const jsonPayload = decodeURIComponent(
          atob(base64)
            .split('')
            .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
            .join('')
        )
        return JSON.parse(jsonPayload)
      } catch (e) {
        console.error('Failed to parse JWT token', e)
        return null
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) {
          return false
        }
        this.loading = true
        login(this.loginForm)
          .then(data => {
            this.loading = false
            if (data && typeof data === 'string') {
              const payload = this.decodeToken(data)
              if (payload) {
                localStorage.setItem('token', data)
                localStorage.setItem('role', payload.role)
                localStorage.setItem('realName', payload.realName || '')
                localStorage.setItem('username', payload.username || this.loginForm.username)

                this.$message({
                  message: '登录成功',
                  type: 'success',
                  duration: 1500
                })
                this.$router.push('/')
              } else {
                this.$message.error('解析登录令牌载荷失败')
              }
            } else {
              this.$message.error('登录返回数据异常')
            }
          })
          .catch(() => {
            this.loading = false
          })
      })
    },
    handleHelp() {
      this.$message({
        message: '请联系系统管理员获取帮助',
        type: 'info',
        duration: 3000
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100vw;
  background-color: #f2f8f8;
  overflow: hidden;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.login-card {
  display: flex;
  width: 923px;
  height: 658px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

/* 左侧裁切插图 */
.login-left {
  width: 462px;
  height: 100%;
  background-image: url('~@/assets/login_bg.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/* 右侧白底表单 */
.login-right {
  width: 461px;
  height: 100%;
  padding: 60px 48px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

/* 品牌标志 */
.brand-header {
  margin-bottom: 30px;
}

.brand-title {
  font-size: 32px;
  font-weight: 800;
  color: #095263; /* 像素级匹配设计图的深蓝绿色 */
  margin: 0;
  letter-spacing: -0.5px;
}

.brand-subtitle {
  font-size: 13px;
  font-weight: 500;
  color: #5a727a;
  margin: 4px 0 0 0;
}

/* 表单标题 */
.form-title {
  font-size: 20px;
  font-weight: 700;
  color: #1c2e35;
  margin: 0 0 25px 0;
}

/* 密码项 Label Wrapper */
.password-label-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  line-height: normal;
}

.forgot-link {
  font-size: 12px;
  font-weight: 600;
  color: #5a727a;
  text-decoration: none;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: #095263;
  text-decoration: underline;
}

/* Form Item overrides */
.login-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #4a5d65;
  padding-bottom: 6px;
  font-size: 14px;
  display: flex;
  width: 100%;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__inner) {
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  border-color: #cbd5e1;
  font-size: 14px;
  font-family: 'Outfit', sans-serif;
  color: #1f2937;
}

.login-form :deep(.el-input__inner:focus) {
  border-color: #0fad5b;
}

.login-form :deep(.el-input__icon) {
  line-height: 44px;
}

.submit-item {
  margin-top: 32px;
}

/* 按钮样式还原 */
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 700;
  border-radius: 22px; /* 完美半圆角 */
  background-color: #0fad5b !important;
  border-color: #0fad5b !important;
  color: #ffffff !important;
  transition: all 0.2s ease;
  font-family: 'Outfit', sans-serif;
}

.login-btn:hover {
  background-color: #0d964e !important;
  border-color: #0d964e !important;
  box-shadow: 0 4px 12px rgba(15, 173, 91, 0.25);
  transform: translateY(-0.5px);
}

.login-btn:active {
  transform: translateY(0);
}

/* 底部页脚链接 */
.login-footer {
  margin-top: 25px;
  text-align: center;
  font-size: 14px;
  color: #5a727a;
}

.signup-link, .help-link {
  color: #5a727a;
  text-decoration: none;
  font-weight: 700;
  transition: color 0.2s;
}

.signup-link:hover, .help-link:hover {
  color: #095263;
  text-decoration: underline;
}

.divider {
  margin: 0 10px;
  color: #cbd5e1;
}

@media (max-width: 950px) {
  .login-card {
    width: 461px;
  }
  .login-left {
    display: none;
  }
}
</style>
