package app.mosia.nexus
package infrastructure.monitoring

import domain.config.AppConfig
import domain.error.*
import domain.repository.{SessionRepository, TrainingRepository}
import domain.services.infra.SystemMetricsCollector

import zio.{Duration, Schedule, ZIO, ZLayer, durationInt}
import zio.json.*
import zio.http.*
import zio.json.ast.Json
import zio.metrics.connectors.MetricsConfig
import zio.metrics.{Metric, MetricKeyType}

import java.lang.management.ManagementFactory

final class SystemMetricsCollectorLive(
  config: AppConfig,
  sessionRepository: SessionRepository,
  trainingRepository: TrainingRepository
) extends SystemMetricsCollector:

  // 定义 Prometheus 指标
  private val jvmMemoryUsed  = Metric.gauge("jvm_memory_used_bytes")
  private val jvmMemoryMax   = Metric.gauge("jvm_memory_max_bytes")
  private val jvmThreadCount = Metric.gauge("jvm_threads_count")
  private val jvmCpuUsage    = Metric.gauge("jvm_cpu_usage_percent")

  private val activeSessionsCount   = Metric.gauge("nexus_active_sessions_total")
  private val runningTrainingsCount = Metric.gauge("nexus_running_trainings_total")

  private val httpRequestsTotal   = Metric.counter("nexus_http_requests_total")
  private val httpRequestDuration =
    Metric.histogram("nexus_http_request_duration_seconds", MetricKeyType.Histogram.Boundaries.linear(0, 0.1, 10))

  override def startCollection(): AppTask[Unit] =
    if config.monitoring.metrics.collectSystemMetrics then
      collectOnce().schedule(Schedule.fixed(Duration.fromSeconds(10))).fork.unit
    else ZIO.logInfo("System metrics collection disabled") *> ZIO.unit

  override def collectOnce(): AppTask[Unit] =
    for
      _ <- collectJvmMetrics()
      _ <- collectBusinessMetrics()
    yield ()

  /** 收集 JVM 指标 */
  private def collectJvmMetrics(): AppTask[Unit] =
    ZIO
      .attempt {
        val runtime    = Runtime.getRuntime
        val memoryBean = ManagementFactory.getMemoryMXBean
        val threadBean = ManagementFactory.getThreadMXBean
        val osBean     = ManagementFactory.getOperatingSystemMXBean

        // 内存指标
        val heapMemory    = memoryBean.getHeapMemoryUsage
        val nonHeapMemory = memoryBean.getNonHeapMemoryUsage

        val totalUsed = heapMemory.getUsed + nonHeapMemory.getUsed
        val totalMax  = heapMemory.getMax + nonHeapMemory.getMax

        // 线程指标
        val threadCount = threadBean.getThreadCount

        // CPU 使用率（简化版）
        val cpuUsage = osBean.getSystemLoadAverage

        (totalUsed, totalMax, threadCount, cpuUsage)
      }
      .flatMap { case (memUsed, memMax, threads, cpu) =>
        jvmMemoryUsed.set(memUsed.toDouble) *>
          jvmMemoryMax.set(memMax.toDouble) *>
          jvmThreadCount.set(threads.toDouble) *>
          jvmCpuUsage.set(cpu)
      }
      .mapError(err => err.toAppError("collect_jvm_metrics"))

  /** 收集业务指标 */
  private def collectBusinessMetrics(): AppTask[Unit] =
    for
      // 统计活跃会话数
      activeSessions <- countActiveSessions()
      _ <- activeSessionsCount.set(activeSessions.toDouble)

      // 统计运行中的训练任务数
      runningTrainings <- countRunningTrainings()
      _ <- runningTrainingsCount.set(runningTrainings.toDouble)
    yield ()

  /** 统计活跃会话数 */
  private def countActiveSessions(): AppTask[Int] =
    sessionRepository
      .findByUserId(domain.model.user.UserId(java.util.UUID.randomUUID()), 1000)
      .map(sessions =>
        sessions.count(s =>
          s.status == domain.model.session.SessionStatus.Running ||
            s.status == domain.model.session.SessionStatus.Initializing
        )
      )
      .catchAll(_ => ZIO.succeed(0))

  /** 统计运行中的训练任务数 */
  private def countRunningTrainings(): AppTask[Int] =
    ZIO.succeed(0) // TODO: 实现训练任务统计

object SystemMetricsCollectorLive:
  val live: ZLayer[AppConfig & SessionRepository & TrainingRepository, Nothing, SystemMetricsCollector] =
    ZLayer.fromFunction(SystemMetricsCollectorLive(_, _, _))

  val config: ZLayer[Any, Nothing, MetricsConfig] =
    ZLayer.succeed(MetricsConfig(interval = 1.second))
