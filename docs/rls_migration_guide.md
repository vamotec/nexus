# RLS 迁移实施指南

## 已创建的迁移文件

### PostgreSQL 迁移（3 个文件）

#### V14__enable_rls_for_all_tables.sql
- **目的**：为所有 PostgreSQL 表启用 Row Level Security
- **覆盖表**：20 个表（用户、组织、项目、仿真、会话、训练、资源、审计等）
- **影响**：启用 RLS 但不创建限制性策略

#### V15__create_service_role_bypass_policies.sql
- **目的**：创建服务角色绕过策略
- **策略类型**：`FOR ALL TO service_role USING (true) WITH CHECK (true)`
- **影响**：后端服务（使用 service_role）可以完全访问所有数据

#### V16__fix_security_definer_views.sql
- **目的**：修复视图的安全属性
- **修改视图**：
  - `user_project_context`
  - `user_resource_context`
  - `user_organization_context`
- **更改**：添加 `WITH (security_invoker=true)` 选项

### TimescaleDB 迁移（2 个文件）

#### V3__enable_rls_for_timescale_tables.sql
- **目的**：为 TimescaleDB 表启用 RLS
- **覆盖表**：2 个表
  - `session_metrics_snapshot`（普通表）
  - `session_metrics_history`（Hypertable）
- **特殊说明**：
  - Hypertable 的 RLS 自动应用到所有 chunks
  - Continuous Aggregates 继承源表权限

#### V4__create_service_role_bypass_policies_timescale.sql
- **目的**：为 TimescaleDB 表创建服务角色绕过策略
- **影响**：service_role 可以完全访问所有时序数据
- **性能**：零影响（绕过所有 RLS 检查）

## 应用迁移步骤

### 方式 1：通过应用启动自动迁移（推荐）

```bash
# 1. 确保 Supabase 数据库正在运行
# 检查 .env 文件中的数据库连接配置

# 2. 启动应用（Flyway 会自动执行迁移）
sbt run

# 3. 查看日志确认迁移成功
# PostgreSQL 应该看到：
# [info] Successfully applied 3 migration(s)
# [info] - V14: enable_rls_for_all_tables
# [info] - V15: create_service_role_bypass_policies
# [info] - V16: fix_security_definer_views

# TimescaleDB 应该看到：
# [info] Successfully applied 2 migration(s)
# [info] - V3: enable_rls_for_timescale_tables
# [info] - V4: create_service_role_bypass_policies_timescale
```

### 方式 2：手动运行迁移

```bash
# 运行 migration 模块
sbt "migration/run"
```

### 方式 3：直接在 Supabase Dashboard 执行

1. 登录 Supabase Dashboard
2. 进入 SQL Editor
3. 依次复制并执行 V14、V15、V16 文件的内容

## 验证迁移结果

### 1. 检查 RLS 状态

在 Supabase SQL Editor 中运行：

```sql
-- 检查所有表的 RLS 状态
SELECT
  schemaname,
  tablename,
  rowsecurity as rls_enabled
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
```

**期望结果**：所有表的 `rls_enabled` 应该为 `true`

### 2. 检查 RLS 策略

```sql
-- 检查所有 RLS 策略
SELECT
  schemaname,
  tablename,
  policyname,
  permissive,
  roles,
  cmd as operation
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;
```

**期望结果**：
- 每个表应该有一个 "Service role has full access" 策略
- roles 应该包含 `service_role`
- cmd 应该是 `*`（ALL 操作）

### 3. 检查视图安全属性

```sql
-- 检查视图的 security_invoker 选项
SELECT
  c.relname AS view_name,
  unnest(c.reloptions) AS options
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE c.relkind = 'v'
  AND n.nspname = 'public'
  AND c.relname IN (
    'user_project_context',
    'user_resource_context',
    'user_organization_context'
  );
```

**期望结果**：应该看到 `security_invoker=true` 选项

### 4. 在 Supabase Dashboard 验证

1. 进入 Supabase Dashboard
2. 查看 Table Editor
3. 检查表是否不再显示 "unrestricted" 标记
4. 查看 Database → Policies，应该看到所有策略

## 配置要求

### 环境变量

确保你的 `.env` 或 `application.conf` 中配置了正确的 Supabase 连接：

```conf
# application.conf
app.db.postgres {
  url = "jdbc:postgresql://db.xxx.supabase.co:5432/postgres"
  user = "postgres"
  password = ${SUPABASE_DB_PASSWORD}  # 使用 service_role 或 postgres 密码
}
```

### Supabase 连接角色

确认你的应用使用以下角色之一连接数据库：

- ✅ **postgres** (超级用户) - 默认，拥有所有权限
- ✅ **service_role** - Supabase 服务角色，绕过 RLS

**不要使用**：
- ❌ **anon** - 匿名角色，受 RLS 限制
- ❌ **authenticated** - 认证用户角色，受 RLS 限制

## 性能影响

### 预期影响

- **查询性能**：零影响（service_role 绕过所有策略）
- **写入性能**：零影响
- **数据库开销**：极小（仅策略元数据）

### 基准测试（可选）

迁移前后运行以下查询，比较性能：

```sql
-- 简单查询
EXPLAIN ANALYZE
SELECT * FROM users WHERE id = 'your-user-id';

-- 复杂连接查询
EXPLAIN ANALYZE
SELECT u.*, o.*, om.*
FROM users u
JOIN organization_members om ON u.id = om.user_id
JOIN organizations o ON om.organization_id = o.id
WHERE u.id = 'your-user-id';
```

**期望结果**：性能应该相同（因为 service_role 绕过 RLS）

## 安全注意事项

### ⚠️ 关键安全要求

1. **保护 service_role 密钥**
   ```bash
   # 正确：使用环境变量
   export SUPABASE_SERVICE_ROLE_KEY="your-secret-key"

   # 错误：硬编码在代码中
   val apiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." // ❌ 不要这样做
   ```

2. **不要在前端暴露 service_role**
   - ✅ 仅在后端服务中使用
   - ❌ 不要在前端代码、Git、日志中暴露

3. **定期轮换密钥**
   - 建议每 3-6 个月轮换一次
   - 在 Supabase Dashboard → Settings → API 中重新生成

4. **网络安全**
   - 配置 Supabase 的 IP 白名单
   - 仅允许你的后端服务器 IP 访问

## 回滚计划

如果迁移后发现问题，可以回滚：

### 回滚 RLS 策略（保留 RLS 启用状态）

```sql
-- 删除所有服务角色策略
DO $$
DECLARE
  pol record;
BEGIN
  FOR pol IN
    SELECT schemaname, tablename, policyname
    FROM pg_policies
    WHERE schemaname = 'public'
      AND policyname LIKE '%Service role%'
  LOOP
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I',
      pol.policyname, pol.schemaname, pol.tablename);
  END LOOP;
END $$;
```

### 完全禁用 RLS

```sql
-- 禁用所有表的 RLS（不推荐）
DO $$
DECLARE
  tbl record;
BEGIN
  FOR tbl IN
    SELECT schemaname, tablename
    FROM pg_tables
    WHERE schemaname = 'public'
  LOOP
    EXECUTE format('ALTER TABLE %I.%I DISABLE ROW LEVEL SECURITY',
      tbl.schemaname, tbl.tablename);
  END LOOP;
END $$;
```

## 未来扩展

### 如果需要支持前端直接访问 Supabase API

需要添加基于用户的细粒度策略，例如：

```sql
-- 示例：用户只能读取自己的数据
CREATE POLICY "Users can read own data"
  ON users
  FOR SELECT
  TO authenticated
  USING (auth.uid() = id);

-- 示例：组织成员可以读取组织数据
CREATE POLICY "Organization members can read org data"
  ON organizations
  FOR SELECT
  TO authenticated
  USING (
    id IN (
      SELECT organization_id
      FROM organization_members
      WHERE user_id = auth.uid() AND is_active = true
    )
  );
```

### 监控和审计

在生产环境中，建议启用：

1. **查询日志**
   ```sql
   -- 启用慢查询日志
   ALTER DATABASE postgres SET log_min_duration_statement = 1000; -- 1秒
   ```

2. **RLS 策略使用监控**
   - 定期检查 `pg_stat_all_tables` 视图
   - 监控 RLS 策略的性能影响

## 故障排查

### 问题 1：应用无法连接数据库

**症状**：应用启动后报错 "permission denied"

**解决方案**：
1. 检查连接使用的是 `postgres` 或 `service_role` 角色
2. 确认密码正确
3. 检查 Supabase 的网络配置

### 问题 2：RLS 策略未生效

**症状**：Supabase Dashboard 仍显示 "unrestricted"

**解决方案**：
1. 刷新浏览器缓存
2. 在 SQL Editor 中验证 RLS 状态（见上文）
3. 检查迁移是否成功执行

### 问题 3：查询性能下降

**症状**：迁移后查询变慢

**解决方案**：
1. 确认使用 service_role 连接（应该绕过 RLS）
2. 运行 EXPLAIN ANALYZE 分析查询计划
3. 检查是否意外使用了 authenticated 角色

## TimescaleDB 特殊说明

### Hypertable 和 Chunks

- **自动继承**：为 `session_metrics_history` 启用 RLS 后，所有 chunks 自动继承
- **无需手动配置**：不需要为每个 chunk 单独设置 RLS
- **压缩数据**：压缩的 chunks 同样受 RLS 保护

### Continuous Aggregates

- **无法直接启用 RLS**：`session_metrics_1min` 和 `session_metrics_1hour` 是物化视图
- **继承权限**：它们继承源表（`session_metrics_history`）的访问权限
- **service_role 访问**：后端服务自动拥有所有权限

### 后台任务

- **不受 RLS 影响**：压缩、保留、刷新策略使用超级用户权限
- **正常运行**：这些任务会继续按计划执行

详细说明请查看：`docs/timescale_rls_notes.md`

## 总结

✅ **已完成的工作**：
- **PostgreSQL**: 为 20 个表启用 RLS + 创建绕过策略 + 修复视图
- **TimescaleDB**: 为 2 个表启用 RLS + 创建绕过策略
- **总计**: 5 个迁移文件，覆盖所有数据库表
- 符合 Supabase 安全最佳实践

✅ **对应用的影响**：
- 零性能影响
- 零代码更改
- 提供额外的安全防护层

✅ **解决的问题**：
- ✓ 消除所有 "unrestricted" 警告
- ✓ 启用全面的 RLS 保护
- ✓ 修复 SECURITY DEFINER 视图警告
- ✓ 覆盖 PostgreSQL 和 TimescaleDB

现在你的 Supabase 数据库（PostgreSQL + TimescaleDB）已经完全符合安全最佳实践！🎉
