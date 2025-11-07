package app.mosia.nexus.domain.model.common

import app.mosia.nexus.domain.model.agent.AgentType
import Goal.{ConstraintAlert, LogicalOperator, RetryPolicy, StateCondition}
import app.mosia.nexus.domain.model.resource.SLA.TimeRange
import zio.Duration

import java.util.UUID

object Behavior:
  case class RuleId(value: UUID) extends EntityId[RuleId]

  /** 行为规则类型 */
  enum BehaviorRuleType:
    case ConditionAction, UtilityBased, FiniteStateMachine,
      RuleBasedSystem, NeuralNetwork, Hybrid

  /** 触发条件类型 */
  enum TriggerCondition:
    case TimeBased(delay: Duration, interval: Option[Duration])
    case EventBased(eventType: String, predicate: String => Boolean)
    case StateBased(stateVariable: String, condition: StateCondition)
    case Composite(conditions: List[TriggerCondition], operator: LogicalOperator)

  /** 行为规则 */
  case class BehaviorRule(
    id: RuleId,
    name: String,
    ruleType: BehaviorRuleType,
    description: String,

    // 触发条件
    trigger: TriggerCondition,

    // 前提条件
    preconditions: List[Precondition],

    // 执行动作
    actions: List[Action],

    // 优先级
    priority: Int = 1,

    // 执行限制
    execution: ExecutionConstraints,

    // 学习配置
    learning: Option[RuleLearningConfig],

    // 元数据
    tags: Set[String] = Set.empty,
    version: String = "1.0"
  )

  /** 前提条件 */
  case class Precondition(
    condition: String, // 可执行的表达式或查询
    required: Boolean = true,
    description: String
  )

  /** 执行动作 */
  case class Action(
    actionType: ActionType,
    target: String, // 执行目标
    parameters: Map[String, Any],
    delay: Duration = Duration.Zero,
    duration: Option[Duration] = None
  )

  enum ActionType:
    case Move, Rotate, Accelerate, Brake, ChangeLane,
      Signal, Communicate, ExecuteScript, Custom

  /** 执行约束 */
  case class ExecutionConstraints(
    maxExecutions: Int = Int.MaxValue, // 最大执行次数
    cooldown: Duration = Duration.Zero, // 冷却时间
    timeout: Option[Duration] = None, // 超时时间
    concurrent: Boolean = false // 是否允许并发执行
  )

  /** 规则学习配置 */
  case class RuleLearningConfig(
    learningRate: Double = 0.1,
    explorationRate: Double = 0.1,
    rewardFunction: String, // 奖励函数定义
    updateFrequency: Duration = Duration.fromSeconds(1)
  )
  // ============================== //

  case class ConstraintId(value: UUID) extends EntityId[ConstraintId]

  /** 约束类型 */
  enum ConstraintType:
    case Physical, Safety, Regulatory, Performance, Comfort, Custom

  /** 约束严重程度 */
  enum ConstraintSeverity:
    case Hard, Soft, Warning

  /** 行为约束 */
  case class BehaviorConstraint(
    id: ConstraintId,
    name: String,
    constraintType: ConstraintType,
    severity: ConstraintSeverity,
    description: String,

    // 约束条件
    condition: ConstraintCondition,

    // 违反处理
    violation: ViolationHandler,

    // 监控配置
    monitoring: ConstraintMonitoring,

    // 适用条件
    applicability: ApplicabilityConditions
  )

  /** 约束条件 */
  case class ConstraintCondition(
    expression: String, // 约束表达式
    parameters: Map[String, Any],
    evaluationFrequency: Duration = Duration.fromMillis(100)
  )

  /** 违反处理 */
  case class ViolationHandler(
    action: ViolationAction,
    penalty: Double = 0.0,
    escalation: Option[EscalationPolicy] = None
  )

  enum ViolationAction:
    case Stop, SlowDown, Correct, Notify, Log, Ignore

  /** 升级策略 */
  case class EscalationPolicy(
    levels: List[EscalationLevel],
    resetAfter: Duration = Duration.fromSeconds(60)
  )

  case class EscalationLevel(
    violationCount: Int,
    action: ViolationAction,
    message: String
  )

  /** 约束监控 */
  case class ConstraintMonitoring(
    enabled: Boolean = true,
    samplingRate: Duration = Duration.fromMillis(50),
    historySize: Int = 1000,
    alerts: List[ConstraintAlert]
  )

  /** 适用条件 */
  case class ApplicabilityConditions(
    environments: Set[String] = Set.empty, // 适用环境
    agentTypes: Set[AgentType] = Set.empty, // 适用智能体类型
    timeOfDay: Option[TimeRange] = None, // 适用时间段
//                                      weather: Set[WeatherCondition] = Set.empty // 适用天气
  )

  /** 紧急情况类型 */
  enum EmergencyType:
    case CollisionImminent, SystemFailure, EnvironmentalHazard,
      CommunicationLoss, PerformanceDegradation, ManualOverride

  // ===================================================== //

  case class EmergencyBehaviorId(value: UUID) extends EntityId[EmergencyBehaviorId]

  /** 紧急行为 */
  case class EmergencyBehavior(
    id: EmergencyBehaviorId,
    emergencyType: EmergencyType,
    name: String,
    description: String,

    // 触发条件
    trigger: EmergencyTrigger,

    // 响应动作
    response: EmergencyResponse,

    // 恢复策略
    recovery: RecoveryStrategy,

    // 优先级 (紧急行为通常有最高优先级)
    priority: Int = 1000
  )

  /** 紧急触发条件 */
  case class EmergencyTrigger(
    condition: String, // 紧急情况检测条件
    confidence: Double = 0.9, // 检测置信度
    latency: Duration = Duration.fromMillis(50), // 最大检测延迟
    debounce: Duration = Duration.fromMillis(100) // 防抖时间
  )

  /** 紧急响应 */
  case class EmergencyResponse(
    immediateActions: List[EmergencyAction],
    followupActions: List[EmergencyAction],
    communication: EmergencyCommunication
  )

  /** 紧急动作 */
  case class EmergencyAction(
    actionType: EmergencyActionType,
    parameters: Map[String, Any],
    delay: Duration = Duration.Zero
  )

  enum EmergencyActionType:
    case EmergencyStop, EvasiveManeuver, ReduceSpeed,
      SignalEmergency, RequestHelp, SwitchToBackup

  /** 紧急通信 */
  case class EmergencyCommunication(
    broadcast: Boolean = true,
    message: String,
    recipients: Set[String] = Set.empty,
    frequency: Duration = Duration.fromSeconds(1)
  )

  /** 恢复策略 */
  case class RecoveryStrategy(
    condition: String, // 恢复条件
    actions: List[RecoveryAction],
    timeout: Duration = Duration.fromSeconds(5 * 60),
    fallback: Option[FallbackStrategy]
  )

  case class RecoveryAction(
    action: String,
    verification: String, // 验证恢复成功的条件
    retryPolicy: RetryPolicy = RetryPolicy(maxAttempts = 3)
  )

  case class FallbackStrategy(
    action: String,
    severity: EmergencySeverity = EmergencySeverity.Critical
  )

  enum EmergencySeverity:
    case Low, Medium, High, Critical
