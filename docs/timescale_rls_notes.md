# TimescaleDB RLS 特殊说明

## 概述

TimescaleDB 的 RLS 实施与普通 PostgreSQL 表略有不同，需要理解以下特性：

## TimescaleDB RLS 的特殊行为

### 1. Hypertable 和 Chunks

**Hypertable 是什么？**
- Hypertable 是 TimescaleDB 的核心概念
- 底层由多个"chunks"（时间分区）组成
- 每个 chunk 是一个独立的 PostgreSQL 表

**RLS 如何应用到 Hypertable？**

```sql
-- 为 hypertable 启用 RLS
ALTER TABLE session_metrics_history ENABLE ROW LEVEL SECURITY;

-- 自动效果：
-- ✓ 所有现有 chunks 继承 RLS 设置
-- ✓ 所有未来 chunks 自动启用 RLS
-- ✓ 不需要为每个 chunk 单独配置
```

**验证 Chunks 的 RLS 状态：**

```sql
-- 查看 hypertable 的所有 chunks
SELECT
  ht.table_name as hypertable,
  ch.chunk_name,
  ch.range_start,
  ch.range_end
FROM timescaledb_information.chunks ch
JOIN timescaledb_information.hypertables ht
  ON ch.hypertable_name = ht.table_name
WHERE ht.table_name = 'session_metrics_history'
ORDER BY ch.range_start DESC
LIMIT 10;

-- 检查 chunks 的 RLS 状态
SELECT
  schemaname,
  tablename,
  rowsecurity
FROM pg_tables
WHERE tablename LIKE '_hyper_%'
  AND schemaname LIKE '_timescaledb_internal%'
ORDER BY tablename;
```

### 2. Continuous Aggregates（物化视图）

**什么是 Continuous Aggregates？**
- TimescaleDB 的自动聚合物化视图
- 在我们的项目中：
  - `session_metrics_1min`（每分钟聚合）
  - `session_metrics_1hour`（每小时聚合）

**RLS 对 Continuous Aggregates 的影响：**

```sql
-- ❌ 不能直接为物化视图启用 RLS
ALTER TABLE session_metrics_1min ENABLE ROW LEVEL SECURITY;
-- ERROR: cannot enable row level security on a materialized view

-- ✅ 正确方式：控制源表（hypertable）的访问
-- Continuous Aggregate 继承源表的权限
-- 如果用户能查询 session_metrics_history，就能查询聚合视图
```

**访问控制机制：**

| 表/视图 | RLS 应用方式 | 说明 |
|---------|-------------|------|
| `session_metrics_history` | 直接应用 RLS | 源 hypertable |
| `session_metrics_1min` | 继承权限 | Continuous Aggregate，继承源表权限 |
| `session_metrics_1hour` | 继承权限 | Continuous Aggregate，继承源表权限 |
| Chunks (`_hyper_*`) | 自动继承 | Hypertable 的物理分区 |

### 3. 压缩（Compression）

**压缩数据的 RLS 行为：**

```sql
-- 我们的压缩配置（在 V1 中已设置）
ALTER TABLE session_metrics_history SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'session_id',
  timescaledb.compress_orderby = 'time DESC'
);

-- 7 天后自动压缩
SELECT add_compression_policy(
  'session_metrics_history',
  INTERVAL '7 days'
);
```

**RLS 与压缩的关系：**
- ✅ RLS 策略同样应用到压缩的 chunks
- ✅ 查询压缩数据时，RLS 检查正常工作
- ✅ 解压缩时，RLS 仍然有效
- ⚡ service_role 绕过 RLS，性能不受影响

### 4. 数据保留策略（Retention Policy）

**保留策略的独立性：**

```sql
-- 我们的保留策略（在 V1 中已设置）
SELECT add_retention_policy(
  'session_metrics_history',
  INTERVAL '90 days'
);
```

**RLS 不影响数据保留：**
- ✅ 数据会按照保留策略自动删除
- ✅ 删除操作不受 RLS 限制
- ✅ 后台任务使用超级用户权限
- ℹ️ RLS 只影响查询和手动修改

## 我们的配置

### TimescaleDB 表结构

```
session_metrics_snapshot (普通表)
  └─ RLS: ✓ 启用
  └─ 策略: service_role 绕过

session_metrics_history (Hypertable)
  └─ RLS: ✓ 启用
  └─ 策略: service_role 绕过
  └─ Chunks: 自动继承 RLS
  └─ 压缩: RLS 仍生效
  └─ 保留: 90 天自动删除

session_metrics_1min (Continuous Aggregate)
  └─ RLS: 继承源表权限
  └─ 访问: service_role 自动拥有

session_metrics_1hour (Continuous Aggregate)
  └─ RLS: 继承源表权限
  └─ 访问: service_role 自动拥有
```

## 性能影响分析

### Service Role 绕过模式（我们的配置）

```sql
-- 后端服务使用 service_role 连接
-- 查询示例：
SELECT * FROM session_metrics_history
WHERE session_id = 'xxx'
  AND time > NOW() - INTERVAL '1 hour';

-- RLS 检查：
-- ✓ 策略存在，但 service_role 绕过
-- ✓ 查询计划与无 RLS 时相同
-- ✓ 性能影响：零
```

**验证性能：**

```sql
-- 开启性能分析
EXPLAIN (ANALYZE, BUFFERS) SELECT
  session_id,
  time,
  current_fps
FROM session_metrics_history
WHERE session_id = 'your-session-id'
  AND time > NOW() - INTERVAL '1 day'
ORDER BY time DESC;

-- 检查查询计划中是否有 RLS 检查
-- 使用 service_role 时，不应该看到额外的 Filter 步骤
```

### 如果未来需要支持前端直接访问

```sql
-- 为认证用户添加细粒度策略
CREATE POLICY "Users can read own session metrics"
  ON session_metrics_history
  FOR SELECT
  TO authenticated
  USING (
    session_id IN (
      SELECT id FROM sessions WHERE user_id = auth.uid()
    )
  );

-- 性能影响：
-- ⚠️ 每个查询都需要 JOIN sessions 表
-- ⚠️ 时序查询可能变慢
-- ⚠️ 需要仔细优化索引
```

## 监控和调试

### 1. 检查 RLS 是否正确应用

```sql
-- 检查所有 TimescaleDB 表的 RLS 状态
SELECT
  t.schemaname,
  t.tablename,
  t.rowsecurity as rls_enabled,
  (
    SELECT COUNT(*)
    FROM pg_policies p
    WHERE p.schemaname = t.schemaname
      AND p.tablename = t.tablename
  ) as policy_count
FROM pg_tables t
WHERE t.schemaname = 'public'
  AND t.tablename LIKE 'session_metrics%'
ORDER BY t.tablename;
```

### 2. 验证 Service Role 权限

```sql
-- 以 service_role 身份测试查询
SET ROLE service_role;

-- 应该成功返回数据
SELECT COUNT(*) FROM session_metrics_history;
SELECT COUNT(*) FROM session_metrics_1min;
SELECT COUNT(*) FROM session_metrics_1hour;

-- 重置角色
RESET ROLE;
```

### 3. 检查 Hypertable 健康状态

```sql
-- 查看 hypertable 信息
SELECT * FROM timescaledb_information.hypertables
WHERE table_name = 'session_metrics_history';

-- 查看压缩状态
SELECT
  chunk_schema,
  chunk_name,
  compression_status,
  uncompressed_total_bytes,
  compressed_total_bytes
FROM timescaledb_information.chunks
WHERE hypertable_name = 'session_metrics_history'
ORDER BY range_start DESC
LIMIT 10;

-- 查看保留策略
SELECT * FROM timescaledb_information.jobs
WHERE proc_name LIKE '%retention%';
```

## 故障排查

### 问题 1：Continuous Aggregate 刷新失败

**症状**：
```
ERROR: permission denied for table session_metrics_history
```

**原因**：
- Continuous Aggregate 的刷新策略使用后台 worker
- Worker 可能没有足够的权限

**解决方案**：
```sql
-- 确保 continuous aggregate 的 owner 有正确权限
ALTER MATERIALIZED VIEW session_metrics_1min OWNER TO postgres;
ALTER MATERIALIZED VIEW session_metrics_1hour OWNER TO postgres;

-- 或者检查刷新策略是否正常
SELECT * FROM timescaledb_information.jobs
WHERE proc_name LIKE '%continuous_aggregate%';
```

### 问题 2：压缩任务失败

**症状**：
压缩策略运行但数据未压缩

**解决方案**：
```sql
-- 检查压缩策略
SELECT * FROM timescaledb_information.jobs
WHERE proc_name LIKE '%compression%';

-- 手动触发压缩（测试）
CALL run_job(job_id);  -- 使用实际的 job_id

-- 检查压缩是否成功
SELECT
  chunk_name,
  compression_status,
  uncompressed_total_bytes,
  compressed_total_bytes
FROM timescaledb_information.chunks
WHERE hypertable_name = 'session_metrics_history'
ORDER BY range_start DESC;
```

### 问题 3：查询性能下降

**检查清单**：

```sql
-- 1. 确认使用 service_role 连接
SELECT current_user, session_user;
-- 应该返回: postgres 或 service_role

-- 2. 检查查询计划
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT * FROM session_metrics_history
WHERE session_id = 'xxx' AND time > NOW() - INTERVAL '1 hour';

-- 3. 确认索引存在
SELECT
  schemaname,
  tablename,
  indexname,
  indexdef
FROM pg_indexes
WHERE tablename = 'session_metrics_history'
ORDER BY indexname;

-- 4. 检查是否有意外的策略
SELECT * FROM pg_policies
WHERE tablename = 'session_metrics_history';
-- 应该只有一个 service_role 绕过策略
```

## 最佳实践总结

### ✅ 推荐做法

1. **使用 service_role 连接**
   - 后端服务始终使用 service_role
   - 获得最佳性能（绕过 RLS）

2. **保持简单的策略**
   - 只使用绕过策略
   - 避免复杂的条件判断

3. **监控后台任务**
   - 确保压缩策略正常运行
   - 确保保留策略正常删除旧数据
   - 确保 continuous aggregate 刷新正常

4. **定期检查性能**
   - 使用 EXPLAIN ANALYZE 检查查询计划
   - 监控 chunk 数量和大小
   - 监控压缩率

### ❌ 避免的陷阱

1. **不要为 continuous aggregates 设置 RLS**
   - 它们是物化视图，不支持 RLS
   - 通过控制源表来控制访问

2. **不要忘记 chunks**
   - Hypertable 的策略会自动应用到 chunks
   - 不需要手动配置每个 chunk

3. **不要在前端直接访问 TimescaleDB**
   - 时序数据查询可能很昂贵
   - 应该通过后端 API 提供聚合数据

4. **不要禁用后台任务**
   - 压缩、保留、刷新任务对性能很重要
   - 它们不受 RLS 影响

## 参考资源

- [TimescaleDB RLS 文档](https://docs.timescale.com/use-timescale/latest/security/row-level-security/)
- [PostgreSQL RLS 文档](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)
- [Supabase RLS 最佳实践](https://supabase.com/docs/guides/auth/row-level-security)
