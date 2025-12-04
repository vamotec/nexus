# 多环境迁移配置指南

## 问题说明

你的应用需要同时支持两种环境：
- **本地开发**：使用 Docker Compose 的 PostgreSQL + TimescaleDB
- **生产环境**：使用 Supabase 托管数据库

主要区别：
- Supabase 有预定义的 `service_role` 角色
- 本地环境通常只有 `postgres` 超级用户

## 解决方案

### 新增的迁移文件

我创建了 **V14.5__create_service_role_if_not_exists.sql** 来自动处理这个差异：

```sql
-- 智能检测环境
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN
    -- 本地环境：创建 service_role
    CREATE ROLE service_role WITH LOGIN SUPERUSER BYPASSRLS;
    RAISE NOTICE 'Created service_role for local environment';
  ELSE
    -- Supabase 环境：service_role 已存在
    RAISE NOTICE 'service_role already exists (Supabase environment)';
  END IF;
END $$;
```

### 迁移执行顺序

```
V14  → 启用 RLS for all tables
V14.5 → 创建 service_role（如果不存在）⭐ 新增
V15  → 创建 service_role 绕过策略
V16  → 修复视图安全属性
```

### 工作原理

| 环境 | V14.5 的行为 | V15 的结果 |
|------|-------------|-----------|
| **Supabase** | 检测到 service_role 已存在，跳过创建 | ✅ 使用现有的 service_role |
| **本地开发** | 创建新的 service_role | ✅ 使用新创建的 service_role |
| **两者** | 统一使用 service_role 连接 | ✅ 策略完全相同 |

## 配置你的连接

### 本地环境 (docker-compose.dev.yml)

**选项 A：使用 postgres 用户（简单）**

```yaml
# application.conf 或 .env
app.db.postgres {
  url = "jdbc:postgresql://localhost:5432/mosia_dev"
  user = "postgres"
  password = "your_password"
}
```

**选项 B：使用 service_role（推荐）**

运行迁移后，可以切换到 service_role：

```yaml
app.db.postgres {
  url = "jdbc:postgresql://localhost:5432/mosia_dev"
  user = "service_role"
  password = "same_as_postgres_or_custom"
}

app.db.timescale {
  url = "jdbc:postgresql://localhost:5433/nexus-ts"
  user = "service_role"
  password = "same_as_postgres_or_custom"
}
```

### Supabase 环境

```yaml
# 使用 Supabase 的 service_role
app.db.postgres {
  url = "jdbc:postgresql://db.xxx.supabase.co:5432/postgres"
  user = "postgres"  # 或使用 service_role JWT
  password = ${SUPABASE_DB_PASSWORD}
}
```

## 迁移流程

### 1. 本地开发环境

```bash
# 启动本地数据库
docker-compose -f docker-compose.dev.yml up -d

# 运行应用（自动执行迁移）
sbt run

# 日志输出：
# [info] Migrating schema `public` to version "14 - enable rls for all tables"
# [info] Migrating schema `public` to version "14.5 - create service role if not exists"
# [NOTICE] Created service_role for local environment  ⭐
# [info] Migrating schema `public` to version "15 - create service role bypass policies"
# [info] Migrating schema `public` to version "16 - fix security definer views"
```

### 2. Supabase 环境

```bash
# 配置 Supabase 连接
export SUPABASE_DB_PASSWORD="your_password"

# 运行应用（自动执行迁移）
sbt run

# 日志输出：
# [info] Migrating schema `public` to version "14 - enable rls for all tables"
# [info] Migrating schema `public` to version "14.5 - create service role if not exists"
# [NOTICE] service_role already exists (Supabase environment)  ⭐
# [info] Migrating schema `public` to version "15 - create service role bypass policies"
# [info] Migrating schema `public` to version "16 - fix security definer views"
```

## 验证配置

### 检查角色

```sql
-- 查看所有角色
SELECT
  rolname,
  rolsuper,
  rolbypassrls,
  rolcanlogin,
  CASE
    WHEN rolname = 'service_role' AND rolbypassrls THEN 'Supabase or Local'
    WHEN rolname = 'postgres' THEN 'Built-in superuser'
    ELSE 'Other'
  END as environment
FROM pg_roles
WHERE rolname IN ('postgres', 'service_role', 'authenticated', 'anon')
ORDER BY rolname;
```

**期望结果：**

| 环境 | postgres | service_role | authenticated | anon |
|------|----------|--------------|---------------|------|
| **本地** | ✓ (超级用户) | ✓ (V14.5 创建) | ❌ | ❌ |
| **Supabase** | ✓ (超级用户) | ✓ (预定义) | ✓ (预定义) | ✓ (预定义) |

### 检查 RLS 策略

```sql
-- 查看 service_role 的策略
SELECT
  schemaname,
  tablename,
  policyname,
  roles
FROM pg_policies
WHERE 'service_role' = ANY(roles)
ORDER BY tablename;
```

**期望结果：**
- 本地和 Supabase 都应该看到相同的策略
- 每个表都有 "Service role has full access" 策略

## 常见问题

### Q1: 为什么不直接使用 postgres 角色？

**A:** 可以！但使用 service_role 有以下好处：
1. **统一性**：本地和 Supabase 使用相同的角色名
2. **最小权限**：可以为 service_role 配置特定权限（虽然目前是超级用户）
3. **未来扩展**：如果需要区分不同的后端服务，可以创建多个服务角色
4. **Supabase 兼容**：符合 Supabase 的最佳实践

### Q2: 本地环境可以删除 service_role 吗？

**A:** 可以，如果你想使用 postgres：

```sql
-- 删除策略
DROP POLICY "Service role has full access to users" ON users;
-- ... 删除其他所有策略

-- 为 postgres 创建策略（可选，因为 postgres 是超级用户）
CREATE POLICY "Postgres has full access to users"
  ON users FOR ALL TO postgres
  USING (true) WITH CHECK (true);

-- 删除角色
DROP ROLE service_role;
```

但**不推荐**，因为会失去环境一致性。

### Q3: 如何在本地测试 RLS 策略？

**A:** 创建一个测试用户：

```sql
-- 创建测试用户（模拟前端用户）
CREATE ROLE test_user WITH LOGIN PASSWORD 'test123';

-- 给予基本权限
GRANT USAGE ON SCHEMA public TO test_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO test_user;

-- 以测试用户连接
-- psql -U test_user -d mosia_dev

-- 测试查询（应该被 RLS 阻止，因为没有策略）
SELECT * FROM users;
-- 错误: new row violates row-level security policy for table "users"
```

### Q4: TimescaleDB 也需要 service_role 吗？

**A:** 是的，TimescaleDB 迁移（V3、V4）也使用 service_role。但由于 TimescaleDB 和 PostgreSQL 共享角色系统：
- 在 PostgreSQL 中创建 service_role 后
- TimescaleDB 自动拥有这个角色
- 不需要重复创建

### Q5: 生产环境切换到 Supabase 需要重新迁移吗？

**A:** 不需要！流程：

1. **导出本地数据**（如果需要）
   ```bash
   pg_dump -U postgres mosia_dev > backup.sql
   ```

2. **在 Supabase 创建新项目**
   - Supabase 已经有 service_role
   - 配置连接字符串

3. **运行迁移**
   ```bash
   sbt run
   ```
   - V14.5 会检测到 service_role 已存在
   - 其他迁移正常执行

4. **导入数据**（如果需要）
   ```bash
   psql -h db.xxx.supabase.co -U postgres -d postgres < backup.sql
   ```

## Docker Compose 配置建议

### 本地开发环境

```yaml
# docker-compose.dev.yml
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: mosia_dev
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"

  timescaledb:
    image: timescale/timescaledb:latest-pg15
    environment:
      POSTGRES_DB: nexus-ts
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5433:5432"
```

### 应用配置

```conf
# application.conf
app {
  db {
    postgres {
      url = "jdbc:postgresql://localhost:5432/mosia_dev"
      url = ${?POSTGRES_URL}  # 环境变量覆盖

      user = "postgres"
      user = ${?POSTGRES_USER}

      password = "dev_password"
      password = ${?POSTGRES_PASSWORD}
    }

    timescale {
      url = "jdbc:postgresql://localhost:5433/nexus-ts"
      url = ${?TIMESCALE_URL}

      user = "postgres"
      user = ${?TIMESCALE_USER}

      password = "dev_password"
      password = ${?TIMESCALE_PASSWORD}
    }
  }
}
```

### 环境变量配置

```bash
# .env.local (本地开发)
POSTGRES_URL=jdbc:postgresql://localhost:5432/mosia_dev
POSTGRES_USER=postgres
POSTGRES_PASSWORD=dev_password

TIMESCALE_URL=jdbc:postgresql://localhost:5433/nexus-ts
TIMESCALE_USER=postgres
TIMESCALE_PASSWORD=dev_password

# .env.production (Supabase)
POSTGRES_URL=jdbc:postgresql://db.xxx.supabase.co:5432/postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=${SUPABASE_DB_PASSWORD}

TIMESCALE_URL=jdbc:postgresql://db.xxx.supabase.co:5432/postgres
TIMESCALE_USER=postgres
TIMESCALE_PASSWORD=${SUPABASE_DB_PASSWORD}
```

## 总结

✅ **现在你可以**：
- 在本地环境开发和测试
- 无缝切换到 Supabase 生产环境
- 使用统一的迁移脚本
- 保持数据库配置的一致性

✅ **迁移文件会自动**：
- 检测环境（本地 vs Supabase）
- 创建或跳过 service_role
- 应用相同的 RLS 策略

✅ **你只需要**：
- 运行 `sbt run` 执行迁移
- 根据环境配置连接字符串
- 享受多环境支持！🎉
