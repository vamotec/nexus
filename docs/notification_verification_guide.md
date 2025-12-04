# 通知和验证码系统使用指南

## 📖 概述

Nexus 集成了完整的通知和验证码系统，包括：
- ✅ **邮件发送** - 基于 SMTP（支持 Gmail、阿里云邮件等）
- ✅ **短信发送** - 基于阿里云短信服务
- ✅ **验证码生成和校验** - 基于 Redis 存储
- ✅ **异步处理** - 通过 RabbitMQ 队列解耦

## 🏗️ 系统架构

```
用户请求
   ↓
NotificationService (业务层)
   ├─ 生成验证码 → VerificationCodeService (Redis)
   └─ 发布事件 → DomainEventPublisher
                    ↓
           HybridEventPublisher (智能路由)
                    ↓
            RabbitMQ Exchange
               ↙        ↘
    email 队列      sms 队列
         ↓                ↓
EmailNotificationConsumer  SmsNotificationConsumer
         ↓                ↓
    EmailService      SmsService
    (SMTP发送)       (阿里云短信)
```

## 🚀 快速开始

### 1. 配置环境变量

创建 `.env` 文件：

```bash
# SMTP 邮件配置
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_FROM=noreply@mosia.app
SMTP_FROM_NAME=Nexus

# 阿里云短信配置
ALIYUN_SMS_ACCESS_KEY_ID=your-access-key-id
ALIYUN_SMS_ACCESS_KEY_SECRET=your-access-key-secret
ALIYUN_SMS_SIGN_NAME=Nexus
ALIYUN_SMS_TEMPLATE_CODE=SMS_123456789

# Redis 配置
REDIS_URI=redis://localhost:6379

# RabbitMQ 配置
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

### 2. Gmail SMTP 配置

如果使用 Gmail 发送邮件：

1. 前往 Google 账户设置
2. 启用 "两步验证"
3. 生成 "应用专用密码"
4. 将密码设置为 `SMTP_PASSWORD`

### 3. 阿里云短信配置

1. 登录阿里云控制台
2. 开通短信服务
3. 创建短信签名和模板
4. 获取 AccessKey ID 和 Secret
5. 模板示例：`您的验证码是${code}，5分钟内有效。`

## 💻 使用示例

### 发送邮箱验证码

```scala
import domain.model.verification.VerificationCodeType
import domain.services.app.NotificationService

class AuthService(notificationService: NotificationService):

  def sendEmailVerificationCode(email: String): Task[String] =
    for
      // 生成并发送验证码
      code <- notificationService.sendEmailVerificationCode(
        email = email,
        codeType = VerificationCodeType.Register  // 注册验证码
      )

      _ <- ZIO.logInfo(s"Verification code sent to $email")
    yield code

  def verifyEmailCode(email: String, code: String): Task[Boolean] =
    notificationService.verifyEmailCode(
      email = email,
      code = code,
      codeType = VerificationCodeType.Register
    )
```

### 发送短信验证码

```scala
def sendSmsVerificationCode(phone: String): Task[String] =
  notificationService.sendSmsVerificationCode(
    phone = phone,
    codeType = VerificationCodeType.Login  // 登录验证码
  )

def verifySmsCode(phone: String, code: String): Task[Boolean] =
  notificationService.verifySmsCode(
    phone = phone,
    code = code,
    codeType = VerificationCodeType.Login
  )
```

### 发送欢迎邮件

```scala
def registerUser(email: String, username: String): Task[User] =
  for
    user <- createUser(email, username)

    // 发送欢迎邮件
    _ <- notificationService.sendWelcomeEmail(email, username)
  yield user
```

### 完整的注册流程示例

```scala
class UserRegistrationService(
  notificationService: NotificationService,
  userRepository: UserRepository
):

  /** 步骤 1: 发送邮箱验证码 */
  def sendVerificationCode(email: String): Task[Unit] =
    for
      // 检查邮箱是否已注册
      exists <- userRepository.existsByEmail(email)
      _ <- ZIO.when(exists)(
        ZIO.fail(new RuntimeException("邮箱已被注册"))
      )

      // 发送验证码
      _ <- notificationService.sendEmailVerificationCode(
        email = email,
        codeType = VerificationCodeType.Register
      )

      _ <- ZIO.logInfo(s"Registration verification code sent to $email")
    yield ()

  /** 步骤 2: 验证码校验并注册 */
  def registerWithCode(
    email: String,
    code: String,
    username: String,
    password: String
  ): Task[User] =
    for
      // 1. 验证验证码
      isValid <- notificationService.verifyEmailCode(
        email = email,
        code = code,
        codeType = VerificationCodeType.Register
      )

      _ <- ZIO.when(!isValid)(
        ZIO.fail(new RuntimeException("验证码错误或已过期"))
      )

      // 2. 创建用户
      user <- userRepository.create(
        email = email,
        username = username,
        password = hashPassword(password)
      )

      // 3. 发送欢迎邮件
      _ <- notificationService.sendWelcomeEmail(email, username)

      _ <- ZIO.logInfo(s"User registered successfully: ${user.id}")
    yield user
```

## 🔧 验证码类型

```scala
enum VerificationCodeType:
  case Email          // 邮箱验证
  case Sms            // 手机验证
  case Login          // 登录验证
  case Register       // 注册验证
  case ResetPassword  // 重置密码验证
```

## ⚙️ 配置说明

### 验证码配置

- **有效期**: 5 分钟（默认）
- **格式**: 6 位数字
- **存储**: Redis
- **一次性**: 验证成功后自动删除

### 邮件模板

验证码邮件包含：
- 精美的 HTML 样式
- 验证码用途说明
- 有效期提醒
- 安全提示

### 短信模板

需在阿里云控制台配置：
```
您的${purpose}验证码是${code}，5分钟内有效。【Nexus】
```

## 📊 监控和日志

### 日志示例

```
[info] Generated verification code for user@example.com (type=Register, valid=5min)
[info] Verification code sent to email: user@example.com (type=Register)
[info] Email sent successfully to user@example.com: 【Nexus】注册验证码
[info] Verification code verified successfully for user@example.com (type=Register)
```

### Redis 监控

查看验证码：
```bash
redis-cli
> KEYS verification_code:*
> GET verification_code:register:user@example.com
```

### RabbitMQ 监控

访问管理界面: http://localhost:15672

查看：
- `nexus.notifications.email` 队列状态
- `nexus.notifications.sms` 队列状态
- 消费速率和未处理消息数量

## 🚨 错误处理

### 常见错误

**邮件发送失败:**
```
Failed to send email: AuthenticationFailedException
```
解决：检查 SMTP 用户名和密码

**短信发送失败:**
```
Aliyun SMS API error: InvalidAccessKeyId
```
解决：检查 AccessKey 配置

**验证码错误:**
```
验证码错误或已过期
```
原因：验证码输入错误、已过期或已被使用

## 📝 REST API 示例

### 发送验证码

```http
POST /api/v1/auth/send-code
Content-Type: application/json

{
  "email": "user@example.com",
  "type": "register"
}
```

响应：
```json
{
  "message": "验证码已发送",
  "expiresIn": 300
}
```

### 验证码注册

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "code": "123456",
  "username": "johndoe",
  "password": "secure-password"
}
```

响应：
```json
{
  "user": {
    "id": "...",
    "email": "user@example.com",
    "username": "johndoe"
  },
  "token": "eyJhbGc..."
}
```

## 🔒 安全最佳实践

1. **验证码限流**: 每个邮箱/手机号每分钟最多发送 1 次
2. **验证码强度**: 6 位数字，共 100 万种组合
3. **有效期**: 5 分钟后自动过期
4. **一次性使用**: 验证成功后立即删除
5. **传输加密**: SMTP 使用 TLS，短信走 HTTPS

## 🎯 集成到 Main.scala

系统已自动集成，启动时会：

1. 初始化 VerificationCodeService (Redis)
2. 初始化 EmailService (SMTP)
3. 初始化 SmsService (阿里云)
4. 启动 EmailNotificationConsumer (RabbitMQ)
5. 启动 SmsNotificationConsumer (RabbitMQ)
6. 注册 NotificationService

## 📚 相关文档

- [RabbitMQ 集成文档](./rabbitmq_integration.md)
- [Redis Streams 迁移文档](./redis_streams_migration.md)
- [PostgreSQL Outbox Pattern](./postgresql_outbox_pattern.md)

## 💡 常见场景

### 场景 1: 用户注册

1. 用户输入邮箱
2. 后端发送验证码到邮箱
3. 用户输入验证码
4. 后端验证并创建账户
5. 发送欢迎邮件

### 场景 2: 密码重置

1. 用户输入邮箱
2. 后端发送验证码
3. 用户输入验证码
4. 后端验证并允许重置密码
5. 发送密码重置成功通知

### 场景 3: 手机号登录

1. 用户输入手机号
2. 后端发送短信验证码
3. 用户输入验证码
4. 后端验证并颁发 Token

## 🎉 总结

完整的通知和验证码系统已集成，包括：

✅ 验证码生成和校验（Redis）
✅ SMTP 邮件发送（支持 Gmail 等）
✅ 阿里云短信发送
✅ RabbitMQ 异步处理
✅ 精美的 HTML 邮件模板
✅ 完整的业务服务层 API

**下一步**: 集成到实际的用户注册/登录流程！
