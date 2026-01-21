<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <img src="@/assets/images/logo.svg" alt="Logo" class="logo" />
        <h1 class="title">慕声报税管理系统</h1>
      </div>

      <a-form
        :model="formState"
        :rules="rules"
        @finish="handleLogin"
        layout="vertical"
        class="login-form"
      >
        <a-form-item name="username" label="用户名">
          <a-input
            v-model:value="formState.username"
            size="large"
            placeholder="请输入用户名"
            allow-clear
          >
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item name="password" label="密码">
          <a-input-password
            v-model:value="formState.password"
            size="large"
            placeholder="请输入密码"
            allow-clear
          >
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <div class="form-actions">
            <a-checkbox v-model:checked="formState.remember">记住我</a-checkbox>
            <a class="forgot-link">忘记密码?</a>
          </div>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="loading"
          >
            登 录
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login-footer">
        Copyright 2026 慕声公司
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/modules/auth'
import type { Rule } from 'ant-design-vue/es/form'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)

const formState = reactive({
  username: '',
  password: '',
  remember: true
})

const rules: Record<string, Rule[]> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度为4-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  loading.value = true
  try {
    await authStore.loginAction({
      username: formState.username,
      password: formState.password,
      remember: formState.remember
    })

    message.success('登录成功')

    // 跳转到目标页面
    const redirect = route.query.redirect as string || '/dashboard'
    router.push(redirect)
  } catch (error: any) {
    // 错误已在request中处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .login-container {
    width: 400px;
    padding: 40px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);

    .login-header {
      text-align: center;
      margin-bottom: 32px;

      .logo {
        width: 64px;
        height: 64px;
        margin-bottom: 16px;
      }

      .title {
        font-size: 24px;
        font-weight: 600;
        color: #262626;
        margin: 0;
      }
    }

    .login-form {
      .form-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .forgot-link {
          color: #1890ff;
          cursor: pointer;

          &:hover {
            text-decoration: underline;
          }
        }
      }
    }

    .login-footer {
      margin-top: 24px;
      text-align: center;
      color: #8c8c8c;
      font-size: 12px;
    }
  }
}
</style>
