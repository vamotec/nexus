package app.mosia.nexus
package domain.model.user

case class UserStatistics(
  totalProjects: Int,
  totalSimulations: Int,
  totalSessions: Int,
  totalTrainingTime: Int, // 秒
  recentActivity: List[Activity],
  projectTrend: List[TrendPoint],
  sessionTrend: List[TrendPoint]
)

case class Activity(
  // 根据你的Activity类型定义补充字段
)

case class TrendPoint(
  // 根据你的TrendPoint类型定义补充字段
)
