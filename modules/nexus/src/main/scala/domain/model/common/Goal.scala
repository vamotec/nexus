package app.mosia.nexus
package domain.model.common

import zio.*

import java.time.Instant
import java.util.UUID

object Goal:
  case class GoalId(value: UUID) extends EntityId[GoalId]

  /** 目标类型 */
  enum GoalType:
    case Navigation, Interaction, Collection, Avoidance,
      Timing, Precision, Endurance, Custom

  /** 目标状态 */
  enum GoalStatus:
    case Pending, Active, Success, Failed, Cancelled

  /** 目标配置 */
  case class GoalConfig(
    id: GoalId,
    name: String,
    goalType: GoalType,
    description: String,

    // 目标条件
    condition: GoalCondition,
    

    // 时间限制
    timeConstraints: TimeConstraints,

    // 优先级和权重
    priority: Int = 1,
    weight: Double = 1.0,

    // 依赖关系
    dependencies: Set[GoalId] = Set.empty,

    // 奖励惩罚
    rewards: RewardStructure,

    // 重试策略
    retryPolicy: RetryPolicy = RetryPolicy.default,

    // 监控配置
    monitoring: GoalMonitoring,

    // 元数据
    tags: Set[String] = Set.empty,
    metadata: Map[String, String] = Map.empty
  )

  /** 目标条件 */
  enum GoalCondition:
    case ReachPosition(target: Vector3D, tolerance: Double, orientation: Option[Quaternion] = None)
    case FollowPath(waypoints: List[Vector3D], maxDeviation: Double, speedLimit: Option[Double] = None)
    case AvoidObject(objectId: String, minDistance: Double)
    case CollectItem(itemId: String, quantity: Int = 1)
    case MaintainSpeed(targetSpeed: Double, tolerance: Double, duration: Duration)
    case CompleteBefore(deadline: Instant)
    case StayInArea(center: Vector3D, radius: Double)
    case InteractWith(agentId: String, interactionType: String)
    case CustomCondition(expression: String) // 用于自定义逻辑

  /** 时间约束 */
  case class TimeConstraints(
    startTime: Option[Instant] = None, // 最早开始时间
    deadline: Option[Instant] = None, // 最晚完成时间
    maxDuration: Option[Duration] = None, // 最大执行时长
    minDuration: Option[Duration] = None // 最小执行时长
  )

  /** 奖励结构 */
  case class RewardStructure(
    successReward: Double = 100.0,
    failurePenalty: Double = -50.0,
    partialRewards: List[PartialReward] = List.empty,
    timeBonus: Option[TimeBonus] = None,
    efficiencyBonus: Option[EfficiencyBonus] = None
  )

  /** 部分奖励 */
  case class PartialReward(
    milestone: String, // 里程碑描述
    condition: PartialCondition, // 达成条件
    reward: Double // 奖励分数
  )

  /** 部分条件 */
  enum PartialCondition:
    case Progress(percentage: Double) // 进度百分比
    case WaypointReached(waypointIndex: Int) // 到达路径点
    case DurationElapsed(duration: Duration) // 持续时间
    case CustomMilestone(condition: String) // 自定义条件

  /** 时间奖励 */
  case class TimeBonus(
    baseTime: Duration, // 基准时间
    bonusPerSecond: Double, // 每秒奖励
    maxBonus: Double // 最大奖励
  )

  /** 效率奖励 */
  case class EfficiencyBonus(
    metric: EfficiencyMetric, // 效率指标
    targetValue: Double, // 目标值
    bonusMultiplier: Double // 奖励倍数
  )

  enum EfficiencyMetric:
    case EnergyConsumption, PathEfficiency, Smoothness, Accuracy

  /** 重试策略 */
  case class RetryPolicy(
    maxAttempts: Int = 3,
    delay: Duration = Duration.fromSeconds(5),
    backoffMultiplier: Double = 2.0,
    resetOnSuccess: Boolean = true
  ) {
    def calculateDelay(attempt: Int): Duration =
      delay.multipliedBy(math.pow(backoffMultiplier, attempt - 1).toLong)
  }

  object RetryPolicy {
    val default: RetryPolicy = RetryPolicy()
    val noRetry: RetryPolicy = RetryPolicy(maxAttempts = 1)
  }

  /** 目标监控 */
  case class GoalMonitoring(
    updateFrequency: Duration = Duration.fromSeconds(1),
    metricsToTrack: Set[GoalMetric] = Set(GoalMetric.Progress, GoalMetric.TimeRemaining),
    alerts: List[GoalAlert] = List.empty,
  )

  /** 目标指标 */
  enum GoalMetric:
    case Progress, TimeRemaining, DistanceToTarget, CurrentScore, Efficiency

  /** 目标告警 */
  case class GoalAlert(
    condition: AlertCondition,
    severity: AlertSeverity,
    message: String,
    actions: List[AlertAction]
  )

  enum AlertSeverity:
    case Info, Warning, Error, Critical

  enum AlertAction:
    case Notify, Log, Retry, Escalate, Abort

  /** 告警条件 */
  case class AlertCondition(
    condition: String, // 告警触发条件
    severity: AlertSeverity,
    duration: Duration = Duration.Zero, // 持续多长时间触发
    hysteresis: Double = 0.0, // 迟滞值
    grouping: AlertGrouping = AlertGrouping.None
  )

  /** 告警分组 */
  enum AlertGrouping:
    case None, ByAgent, ByType, ByLocation, Custom

  /** 复合告警条件 */
  case class CompositeAlertCondition(
    conditions: List[AlertCondition],
    operator: LogicalOperator,
    minConditions: Int = 1
  )

  /** 逻辑运算符 */
  enum LogicalOperator:
    case And, Or, Xor, Not

  /** 状态条件 */
  case class StateCondition(
    variable: String,
    operator: ComparisonOperator,
    value: Any,
    tolerance: Double = 0.0
  )

  enum ComparisonOperator:
    case Equal, NotEqual, Greater, GreaterEqual, Less, LessEqual, In, Contains

  /** 约束告警 */
  case class ConstraintAlert(
    condition: String,
    severity: AlertSeverity,
    message: String,
    autoAcknowledge: Boolean = false
  )
