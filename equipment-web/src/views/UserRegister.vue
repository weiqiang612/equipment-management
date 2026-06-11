<template>
  <div class="register-container">
    <!-- 左侧全屏深色插图（背景图已自带文字像素） -->
    <div class="register-left"></div>

    <!-- 右侧白底表单 -->
    <div class="register-right">
      <!-- 右上角快捷返回登录 -->
      <div class="header-link">
        <span>Already have an account? </span>
        <router-link to="/login" class="login-link">Log In</router-link>
      </div>

      <div class="register-box">
        <h2 class="form-title">Create Your Account</h2>

        <el-form
          ref="registerForm"
          :model="registerForm"
          :rules="registerRules"
          label-position="top"
          class="register-form"
          hide-required-asterisk
          @keyup.enter.native="handleRegister"
        >
          <el-form-item label="Username" prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="e.g., equipman"
              prefix-icon="el-icon-user"
              clearable
            ></el-input>
          </el-form-item>

          <el-form-item label="Password" prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="Choose password"
              prefix-icon="el-icon-lock"
              show-password
              clearable
            ></el-input>
          </el-form-item>

          <el-form-item label="Confirm Password" prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="Re-enter password"
              prefix-icon="el-icon-lock"
              show-password
              clearable
            ></el-input>
          </el-form-item>

          <el-form-item label="Department" prop="unitCode">
            <el-select
              v-model="registerForm.unitCode"
              placeholder="Select your department"
              style="width: 100%"
              clearable
            >
              <el-option
                v-for="item in departments"
                :key="item.unitCode"
                :label="item.unitName"
                :value="item.unitCode"
              ></el-option>
            </el-select>
          </el-form-item>

          <el-form-item class="submit-item">
            <el-button
              :loading="loading"
              type="success"
              class="register-btn"
              @click="handleRegister"
            >
              Create Account
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import { register } from '@/api/user'
import { getDepts } from '@/api/department'

export default {
  name: 'UserRegister',
  data() {
    const validatePass2 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('Please re-enter your password'))
      } else if (value !== this.registerForm.password) {
        callback(new Error('Passwords do not match!'))
      } else {
        callback()
      }
    }

    return {
      departments: [],
      registerForm: {
        username: '',
        password: '',
        confirmPassword: '',
        unitCode: ''
      },
      registerRules: {
        username: [
          { required: true, message: 'Please enter your username', trigger: 'blur' },
          { min: 3, max: 20, message: 'Length must be between 3 and 20 characters', trigger: 'blur' }
        ],
        password: [
          { required: true, message: 'Please enter your password', trigger: 'blur' },
          { min: 6, max: 20, message: 'Length must be between 6 and 20 characters', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, validator: validatePass2, trigger: 'blur' }
        ],
        unitCode: [
          { required: true, message: 'Please select your department', trigger: 'change' }
        ]
      },
      loading: false
    }
  },
  created() {
    this.fetchDepartments()
  },
  methods: {
    // 获取所有部门列表
    fetchDepartments() {
      getDepts()
        .then(data => {
          this.departments = data || []
        })
        .catch(() => {})
    },
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (!valid) {
          return false
        }
        this.loading = true
        // 设计稿无 realName，我们将 realName 默认传入和 username 相同以匹配后端参数校验
        const { username, password, unitCode } = this.registerForm
        const realName = username
        
        register({ username, realName, password, unitCode })
          .then(() => {
            this.loading = false
            this.$message({
              message: '注册成功，即将为您跳转到登录页',
              type: 'success',
              duration: 1500
            })
            setTimeout(() => {
              this.$router.push('/login')
            }, 1500)
          })
          .catch(() => {
            this.loading = false
          })
      })
    }
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  background-color: #ffffff;
  overflow: hidden;
  position: relative;
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

/* 左侧全屏分栏占 50.6% */
.register-left {
  width: 50.6%;
  height: 100%;
  background-image: url('~@/assets/register_bg.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/* 右侧白底表单占 49.4% */
.register-right {
  width: 49.4%;
  height: 100%;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  box-sizing: border-box;
  position: relative;
}

/* 右上角 Log In 链接 */
.header-link {
  position: absolute;
  top: 40px;
  right: 50px;
  font-size: 14px;
  color: #5a727a;
  font-weight: 500;
}

.login-link {
  color: #1c2e35;
  text-decoration: underline;
  font-weight: 700;
  margin-left: 5px;
  transition: color 0.2s;
}

.login-link:hover {
  color: #095263;
}

.register-box {
  width: 100%;
  max-width: 400px;
}

/* 表单标题 */
.form-title {
  font-size: 24px;
  font-weight: 700;
  color: #1c2e35;
  margin: 0 0 30px 0;
  text-align: left;
}

/* Form Item overrides */
.register-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #4a5d65;
  padding-bottom: 6px;
  font-size: 14px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.register-form :deep(.el-input__inner) {
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  border-color: #cbd5e1;
  font-size: 14px;
  font-family: 'Outfit', sans-serif;
  color: #1f2937;
}

.register-form :deep(.el-input__inner:focus) {
  border-color: #0fad5b;
}

.register-form :deep(.el-input__icon) {
  line-height: 44px;
}

.submit-item {
  margin-top: 36px;
}

/* 按钮样式还原 */
.register-btn {
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

.register-btn:hover {
  background-color: #0d964e !important;
  border-color: #0d964e !important;
  box-shadow: 0 4px 12px rgba(15, 173, 91, 0.25);
  transform: translateY(-0.5px);
}

.register-btn:active {
  transform: translateY(0);
}

@media (max-width: 950px) {
  .register-left {
    display: none;
  }
  .register-right {
    width: 100%;
  }
  .header-link {
    top: 24px;
    right: 24px;
  }
}
</style>
