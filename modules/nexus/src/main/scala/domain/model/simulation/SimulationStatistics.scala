package app.mosia.nexus
package domain.model.simulation

import domain.model.session.{SessionId, SessionResult}

import java.time.Instant

/** 统计信息 (聚合数据) */
case class SimulationStatistics(
  totalRuns: Int,
  successfulRuns: Int,
  failedRuns: Int,
  avgCompletionTime: Double,
  avgCollisions: Double,
  bestSessionId: Option[SessionId],
  lastRunAt: Option[Instant]
) {
  def successRate: Double =
    if (totalRuns == 0) 0.0
    else successfulRuns.toDouble / totalRuns

  def addSessionResult(result: SessionResult): SimulationStatistics = {
    val newTotal      = totalRuns + 1
    val newSuccessful = if (result.success) successfulRuns + 1 else successfulRuns
    val newFailed     = if (!result.success) failedRuns + 1 else failedRuns

    // 计算新的平均值 (增量计算)
    val newAvgTime       = (avgCompletionTime * totalRuns + result.completionTime) / newTotal
    val newAvgCollisions = (avgCollisions * totalRuns + result.collisions) / newTotal

    // 更新最佳会话
    val newBest = bestSessionId match {
      case Some(id) =>
        // TODO: 从数据库加载比较，这里简化处理
        if (result.success && result.completionTime < avgCompletionTime)
          Some(result.sessionId)
        else Some(id)
      case None =>
        if (result.success) Some(result.sessionId) else None
    }

    copy(
      totalRuns = newTotal,
      successfulRuns = newSuccessful,
      failedRuns = newFailed,
      avgCompletionTime = newAvgTime,
      avgCollisions = newAvgCollisions,
      bestSessionId = newBest,
      lastRunAt = Some(Instant.now())
    )
  }
}

object SimulationStatistics {
  def empty: SimulationStatistics = SimulationStatistics(
    totalRuns = 0,
    successfulRuns = 0,
    failedRuns = 0,
    avgCompletionTime = 0.0,
    avgCollisions = 0.0,
    bestSessionId = None,
    lastRunAt = None
  )
}
