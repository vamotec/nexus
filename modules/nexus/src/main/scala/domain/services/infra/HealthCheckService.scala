package app.mosia.nexus
package domain.services.infra

import domain.error.*
import domain.model.health.HealthCheckResult

import javax.sql.DataSource
import scala.util.Try

/** 健康检查服务
  *
  * 检查各个依赖服务的健康状态
  */
trait HealthCheckService:
  def checkHealth(): AppTask[HealthCheckResult]
