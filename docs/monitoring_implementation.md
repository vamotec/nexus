# Monitoring 功能实现文档

## 📊 概述

已完整实现 Nexus 的监控功能，包括：
- ✅ Prometheus 指标导出
- ✅ 健康检查端点
- ✅ 系统指标自动收集
- ✅ JVM 和业务指标

## 🏗️ 架构设计

### 1. **HealthCheckService** - 健康检查服务
位置: `infra/monitoring/HealthCheckService.scala`

**职责**:
- 检查数据库连接状态
- 检查 JVM 内存使用情况
- 汇总各组件健康状态

**健康状态枚举**:
- `Healthy`: 所有组件正常
- `Degraded`: 部分组件降级（如内存使用率 90-95%）
- `Unhealthy`: 关键组件故障

**API 响应示例**:
```json
{
  "status": "Healthy",
  "checks": {
    "database": {
      "status": "Healthy",
      "message": "Database connected",
      "responseTime": 15
    },
    "jvm": {
      "status": "Healthy",
      "message": "JVM memory usage: 45%"
    }
  },
  "timestamp": 1699876543210
}
```

### 2. **SystemMetricsCollector** - 系统指标收集器
位置: `infra/monitoring/SystemMetricsCollector.scala`

**收集的指标**:

#### JVM 指标:
- `jvm_memory_used_bytes` - JVM 使用的内存（字节）
- `jvm_memory_max_bytes` - JVM 最大内存（字节）
- `jvm_threads_count` - 线程数
- `jvm_cpu_usage_percent` - CPU 使用率

#### 业务指标:
- `nexus_active_sessions_total` - 活跃会话数
- `nexus_running_trainings_total` - 运行中的训练任务数
- `nexus_http_requests_total` - HTTP 请求总数（计数器）
- `nexus_http_request_duration_seconds` - HTTP 请求耗时（直方图）

**自动收集**:
- 每 10 秒收集一次系统指标
- 可通过配置 `app.monitoring.metrics.collect-system-metrics` 开关

### 3. **PrometheusExporter** - Prometheus 指标导出器
位置: `infra/monitoring/PrometheusExporter.scala`

**职责**:
- 将 ZIO Metrics 收集的指标导出为 Prometheus 格式
- 兼容 Prometheus 抓取协议

**导出格式示例**:
```prometheus
# HELP jvm_memory_used_bytes JVM memory used in bytes
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes 536870912.0

# HELP nexus_active_sessions_total Active simulation sessions
# TYPE nexus_active_sessions_total gauge
nexus_active_sessions_total 12.0

# HELP nexus_http_requests_total Total HTTP requests
# TYPE nexus_http_requests_total counter
nexus_http_requests_total 45678.0
```

### 4. **MonitoringRoutes** - 监控 HTTP 路由
位置: `presentation/http/routes/MonitoringRoutes.scala`

**路由配置**:
- `GET /health` - 健康检查端点（可配置路径）
- `GET /metrics` - Prometheus 指标端点（可配置路径）

**特点**:
- 不需要认证（方便监控系统访问）
- 独立于业务路由（不会被 CORS 和 Auth 中间件影响）
- 支持动态开关（通过配置 `enabled` 字段）

## ⚙️ 配置说明

### application.conf 配置

```hocon
app.monitoring {
  # Prometheus 配置
  prometheus {
    enabled = true
    port = 9090      # 预留，暂未使用独立端口
    path = "/metrics"
  }

  # 健康检查配置
  health-check {
    enabled = true
    path = "/health"
    interval = 30s   # 预留，用于主动健康检查
  }

  # 指标收集配置
  metrics {
    session-sync-interval = 5s      # 会话指标同步间隔
    training-update-interval = 1s   # 训练进度更新间隔
    collect-system-metrics = true   # 是否收集系统指标
  }
}
```

### Scala 配置类

```scala
case class MonitoringConfig(
  prometheus: PrometheusConfig,
  healthCheck: HealthCheckConfig,
  metrics: MetricsConfig
)

case class PrometheusConfig(
  enabled: Boolean,
  port: Int,
  path: String
)

case class HealthCheckConfig(
  enabled: Boolean,
  path: String,
  interval: Duration
)

case class MetricsConfig(
  sessionSyncInterval: Duration,
  trainingUpdateInterval: Duration,
  collectSystemMetrics: Boolean
)
```

## 🚀 使用方式

### 1. 访问健康检查

```bash
curl http://localhost:8080/health

# 响应示例
{
  "status": "Healthy",
  "checks": {
    "database": {
      "status": "Healthy",
      "message": "Database connected",
      "responseTime": 15
    },
    "jvm": {
      "status": "Healthy",
      "message": "JVM memory usage: 45%"
    }
  },
  "timestamp": 1699876543210
}
```

### 2. 访问 Prometheus 指标

```bash
curl http://localhost:8080/metrics

# 响应示例 (Prometheus 格式)
# HELP jvm_memory_used_bytes JVM memory used
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes 536870912.0

# HELP nexus_active_sessions_total Active sessions
# TYPE nexus_active_sessions_total gauge
nexus_active_sessions_total 12.0
```

### 3. Prometheus 配置

在 `prometheus.yml` 中添加抓取配置：

```yaml
scrape_configs:
  - job_name: 'nexus'
    scrape_interval: 15s
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/metrics'
```

### 4. Grafana 仪表盘

推荐的监控面板：

**系统监控**:
- JVM 内存使用趋势
- JVM 线程数变化
- CPU 使用率

**业务监控**:
- 活跃会话数
- 运行中的训练任务数
- HTTP 请求速率和耗时

## 📦 依赖说明

需要在 `build.sbt` 中添加 ZIO Metrics 依赖：

```scala
// ZIO Metrics Prometheus
"dev.zio" %% "zio-metrics-connectors-prometheus" % "2.3.1"
```

## 🔧 集成说明

### Main.scala 中的集成

```scala
// 1. 导入监控服务
import infra.monitoring._
import presentation.http.routes.MonitoringRoutes

// 2. 在 app 中添加监控路由
val monitoring = MonitoringRoutes.routes(
  appConfig.monitoring.healthCheck,
  appConfig.monitoring.prometheus
)

// 3. 启动指标收集
metricsCollector <- ZIO.service[SystemMetricsCollector]
_ <- metricsCollector.startCollection().fork

// 4. 添加 ZLayer 依赖
HealthCheckServiceLive.live,
PrometheusExporterLive.live,
SystemMetricsCollectorLive.live,
prometheusLayer,  // ZIO Prometheus 连接器
```

## 🎯 扩展建议

### 1. 添加更多健康检查项

在 `HealthCheckService` 中添加：
```scala
// 检查 Redis 连接
private def checkRedis(): Task[ComponentHealth] = ???

// 检查 Kafka 连接
private def checkKafka(): Task[ComponentHealth] = ???

// 检查 Neuro gRPC 服务
private def checkNeuroService(): Task[ComponentHealth] = ???
```

### 2. 添加自定义业务指标

```scala
// 会话创建速率
private val sessionCreationRate = Metric.counter("nexus_session_created_total")

// 训练任务完成时长
private val trainingDuration = Metric.histogram("nexus_training_duration_seconds")

// 用户注册数
private val userRegistrations = Metric.counter("nexus_user_registrations_total")
```

### 3. 添加告警规则

在 Prometheus 中配置告警：
```yaml
groups:
  - name: nexus_alerts
    rules:
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
        for: 5m
        annotations:
          summary: "JVM memory usage above 90%"

      - alert: DatabaseDown
        expr: up{job="nexus"} == 0
        for: 1m
        annotations:
          summary: "Nexus service is down"
```

## 📊 监控最佳实践

1. **健康检查频率**: 建议 Kubernetes 配置为每 10 秒一次
2. **指标抓取间隔**: Prometheus 建议每 15 秒抓取一次
3. **告警阈值**:
   - JVM 内存 > 90%: Warning
   - JVM 内存 > 95%: Critical
   - 数据库响应时间 > 1s: Warning
4. **日志和指标关联**: 使用 trace_id 关联日志和指标

## 🐛 故障排查

### 健康检查返回 503

1. 检查数据库连接配置
2. 查看日志中的详细错误信息
3. 验证数据库服务是否正常运行

### Prometheus 指标无法抓取

1. 确认 `app.monitoring.prometheus.enabled = true`
2. 检查路径配置是否正确
3. 验证防火墙是否开放端口

### 指标不更新

1. 检查 `collect-system-metrics` 是否开启
2. 查看 `SystemMetricsCollector.startCollection()` 是否成功启动
3. 检查日志中是否有错误信息

## 🎉 总结

Nexus 监控功能现已完整实现，提供了：
- ✅ 标准化的健康检查 API
- ✅ Prometheus 兼容的指标导出
- ✅ 自动化的系统和业务指标收集
- ✅ 灵活的配置管理

可以与 Prometheus + Grafana + Alertmanager 完美集成，构建完整的可观测性平台！
