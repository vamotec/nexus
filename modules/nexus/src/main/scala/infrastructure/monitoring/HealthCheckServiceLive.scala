package app.mosia.nexus
package infrastructure.monitoring

import infrastructure.persistence.BaseSource.*
import domain.services.infra.HealthCheckService
import domain.error.*
import domain.model.health.{ComponentHealth, HealthCheckResult, HealthStatus}

import zio.{Clock, Task, ZIO, ZLayer}

import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import zio.json.*
import zio.http.*
import zio.json.ast.Json

final class HealthCheckServiceLive(
  db: PostgresDataSource,
) extends HealthCheckService:

  override def checkHealth(): AppTask[HealthCheckResult] =
    for
      timestamp <- Clock.currentTime(java.util.concurrent.TimeUnit.MILLISECONDS)

      // 并行检查所有组件
      dbHealth <- checkDatabase().either
      jvmHealth <- checkJVM().either

      // 汇总结果
      checks = Map(
        "database" -> dbHealth.fold(
          _ => ComponentHealth(HealthStatus.Unhealthy, Some("Database connection failed")),
          identity
        ),
        "jvm" -> jvmHealth.fold(
          _ => ComponentHealth(HealthStatus.Unhealthy, Some("JVM check failed")),
          identity
        )
      )

      // 计算总体状态
      overallStatus = calculateOverallStatus(checks.values.toList)
    yield HealthCheckResult(overallStatus, checks, timestamp)

  /** 检查数据库连接 */
  private def checkDatabase(): AppTask[ComponentHealth] =
    def checkOne(ds: DataSource, name: String): Task[Boolean] =
      ZIO.acquireReleaseWith(
        ZIO.attempt(ds.getConnection)
      )(conn => ZIO.succeed(conn.close()))(conn => ZIO.attempt(conn.isValid(5)))

    (for {
      start <- Clock.currentTime(TimeUnit.MILLISECONDS)
      pgValid <- checkOne(db.ds, "PostgreSQL")
      responseTime <- Clock.currentTime(TimeUnit.MILLISECONDS).map(_ - start)
    } yield {
      if pgValid then
        ComponentHealth(
          HealthStatus.Healthy,
          Some("All databases connected"),
          Some(responseTime)
        )
      else
        val issues = List(
          if !pgValid then Some("PostgreSQL") else None
        ).flatten.mkString(", ")
        ComponentHealth(
          HealthStatus.Unhealthy,
          Some(s"Connection failed: $issues")
        )
    }).mapError(err => err.toAppError("check_database"))

  /** 检查 JVM 健康状态 */
  private def checkJVM(): AppTask[ComponentHealth] =
    ZIO
      .attempt {
        val runtime            = Runtime.getRuntime
        val maxMemory          = runtime.maxMemory()
        val totalMemory        = runtime.totalMemory()
        val freeMemory         = runtime.freeMemory()
        val usedMemory         = totalMemory - freeMemory
        val memoryUsagePercent = (usedMemory.toDouble / maxMemory * 100).toInt

        if memoryUsagePercent < 90 then
          ComponentHealth(
            HealthStatus.Healthy,
            Some(s"JVM memory usage: $memoryUsagePercent%"),
            None
          )
        else if memoryUsagePercent < 95 then
          ComponentHealth(
            HealthStatus.Degraded,
            Some(s"JVM memory usage high: $memoryUsagePercent%"),
            None
          )
        else
          ComponentHealth(
            HealthStatus.Unhealthy,
            Some(s"JVM memory usage critical: $memoryUsagePercent%"),
            None
          )
      }
      .mapError(err => err.toAppError("check_JVM"))

  /** 计算总体健康状态 */
  private def calculateOverallStatus(checks: List[ComponentHealth]): HealthStatus =
    if checks.forall(_.status == HealthStatus.Healthy) then HealthStatus.Healthy
    else if checks.exists(_.status == HealthStatus.Unhealthy) then HealthStatus.Unhealthy
    else HealthStatus.Degraded

object HealthCheckServiceLive:
  val live: ZLayer[PostgresDataSource, Nothing, HealthCheckServiceLive] =
    ZLayer.fromFunction(HealthCheckServiceLive(_))
