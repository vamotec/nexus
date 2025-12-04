# Supabase 数据库安全问题分析与修正方案

## 当前问题总结

### 1. 所有表标记为 unrestricted（无限制）
- **原因**：没有启用 Row Level Security (RLS)
- **风险**：任何有数据库访问权限的用户都可以读写所有数据

### 2. RLS 未启用
- **当前状态**：所有表都没有执行 `ALTER TABLE ... ENABLE ROW LEVEL SECURITY;`
- **Supabase 推荐**：默认启用 RLS 作为核心安全机制

### 3. SECURITY DEFINER 视图警告
- **检测到的视图**：
  - `user_project_context`
  - `user_resource_context`
  - `user_organization_context`
- **问题**：这些视图可能绕过 RLS 策略，使用创建者的权限而非查询用户的权限

## 是否需要修正？关键判断

### 场景 A：使用 Supabase PostgREST API（需要修正 ⚠️）

如果你的应用：
- 使用 Supabase 自动生成的 REST API
- 前端直接调用 Supabase API
- 使用 Supabase Auth 进行身份验证

**必须立即修正！** RLS 是你唯一的数据保护层。

### 场景 B：仅使用 Supabase 作为 PostgreSQL 数据库（可选 ✓）

如果你的应用（**你当前的架构**）：
- ✅ 使用自己的后端服务（ZIO HTTP + Scala）
- ✅ 有完整的应用层权限控制（OrganizationService, 权限验证等）
- ✅ 前端不直接访问 Supabase
- ✅ 只有后端服务连接数据库

**可以不启用 RLS**，因为：
- 你的应用层已经有完整的权限控制
- RLS 会增加数据库查询开销
- 你的服务使用单一数据库用户连接（service role）

## 推荐方案：多层防御策略

即使使用场景 B，我也建议**启用 RLS 作为防御深度措施**，原因：

1. **防止 SQL 注入**：即使应用层有漏洞，RLS 也能提供保护
2. **开发安全**：防止开发/测试时意外的数据泄露
3. **未来扩展**：如果将来需要直接 API 访问，已经有保护
4. **Supabase 最佳实践**：符合 Supabase 推荐的安全配置

## 修正方案

### 方案 1：完整 RLS 策略（推荐）

为每个表启用 RLS 并创建详细的策略。

### 方案 2：服务角色绕过 RLS（简化版）

启用 RLS 但允许服务角色（你的后端）绕过策略。

### 方案 3：保持现状（不推荐）

如果你确定：
- 绝不会使用 Supabase 的 REST API
- 数据库只能从你的后端服务访问
- 已经有严格的网络安全和防火墙规则

可以保持现状，但需要在文档中明确记录这个决定。

## 实施建议

考虑到你当前的架构（完整的后端服务 + 应用层权限控制），我建议：

**采用方案 2：服务角色绕过 RLS（平衡安全与性能）**

优点：
- ✅ 符合 Supabase 最佳实践
- ✅ 不影响现有应用性能
- ✅ 提供额外的安全防护
- ✅ 为未来扩展预留空间
- ✅ 解决 Supabase Dashboard 的警告

缺点：
- ⚠️ 需要执行一次迁移
- ⚠️ 增加少量数据库配置

## 后续步骤

如果你决定实施 RLS（推荐），我可以为你创建：

1. **V14__enable_rls_for_all_tables.sql** - 启用 RLS
2. **V15__create_rls_policies.sql** - 创建基本策略（服务角色绕过）
3. **V16__fix_security_definer_views.sql** - 修复视图的安全属性

这样可以保持数据库安全性，同时不影响你现有的应用逻辑。
