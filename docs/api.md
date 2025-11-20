### REST API（辅助功能）
```
# ============================================
# 认证端点
# ============================================

# 传统认证
POST   /api/auth/register              # 用户注册
POST   /api/auth/login                 # 用户登录
POST   /api/auth/refresh               # 刷新 Token
POST   /api/auth/logout                # 登出
POST   /api/auth/forgot-password       # 忘记密码
POST   /api/auth/reset-password        # 重置密码

# OAUTH 2.0 认证
POST   /api/oauth/login                # oauth登录
POST   /api/oauth/callback/:provider   # oauth回调

  # --------------- 用户相关 ---------------
  "获取当前用户信息"
  me: User!

  "获取当前用户统计数据"
  myStatistics: UserStatistics!

  "获取当前用户配额使用情况"
  myQuota: QuotaUsage!
  
# 用户简单接口
POST   /api/users                      # 新建用户
GET    /api/me                         # 获取用户信息
# ============================================
# 文件操作
# ============================================

# 机器人模型
POST   /api/files/robot-models         # 上传机器人模型（URDF/SDF）
GET    /api/files/robot-models/:id     # 获取模型文件
DELETE /api/files/robot-models/:id     # 删除模型文件

# 环境场景
POST   /api/files/environments          # 上传环境文件
GET    /api/files/environments/:id      # 获取环境文件
DELETE /api/files/environments/:id      # 删除环境文件

# 会话数据
GET    /api/files/sessions/:sessionId/logs      # 下载会话日志
GET    /api/files/sessions/:sessionId/video     # 下载会话视频
GET    /api/files/sessions/:sessionId/data      # 下载会话数据
GET    /api/files/sessions/:sessionId/export    # 导出完整数据包

# 批量下载
POST   /api/files/batch-download        # 批量下载文件
      # Body: { fileIds: [string], format: "zip" | "tar" }

# ============================================
# 导入/导出
# ============================================

POST   /api/export/project/:projectId   # 导出项目（包含配置和数据）
POST   /api/import/project               # 导入项目
POST   /api/export/simulation/:simId    # 导出仿真配置
POST   /api/import/simulation            # 导入仿真配置

# ============================================
# Webhooks（第三方集成）
# ============================================

POST   /api/webhooks/github             # GitHub 集成
POST   /api/webhooks/stripe             # Stripe 支付回调
POST   /api/webhooks/slack              # Slack 通知集成

# ============================================
# 健康检查
# ============================================

GET    /health                          # 服务健康状态
GET    /ready                           # 服务就绪状态
GET    /metrics                         # Prometheus 指标

# ============================================
# 系统管理（仅管理员）
# ============================================

GET    /api/admin/system/status         # 系统状态
GET    /api/admin/users                 # 用户列表
GET    /api/admin/usage-reports         # 使用报告
POST   /api/admin/maintenance           # 维护模式切换
```

### WebSocket 端点
```
# ============================================
# WebSocket 连接（实时通信）
# ============================================

WS     /ws/sessions/:sessionId          # 会话实时数据流
       # 返回：状态更新、指标数据、机器人位置等

WS     /ws/notifications                # 用户通知流
       # 返回：系统通知、任务完成提醒等