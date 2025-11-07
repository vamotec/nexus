package app.mosia.nexus.domain.model.common

import zio.Duration

object Planning:
  /** 规划模块配置 */
  case class PlanningConfig(
    // 路径规划
    pathPlanning: PathPlanningConfig,

    // 行为规划
    behaviorPlanning: BehaviorPlanningConfig,

    // 运动规划
    motionPlanning: MotionPlanningConfig,

    // 重规划策略
    replanning: ReplanningConfig
  )

  /** 路径规划配置 */
  case class PathPlanningConfig(
    algorithm: PathPlanningAlgorithm,
    costFunction: CostFunction,
//                                 constraints: PathConstraints,
//                                 heuristic: HeuristicFunction
  )

  enum PathPlanningAlgorithm:
    case AStar, Dijkstra, RRT, RRTStar, PRM, HybridAStar

  /** 代价函数 */
  case class CostFunction(
    distanceWeight: Double = 1.0,
    timeWeight: Double = 0.5,
    riskWeight: Double = 2.0,
    comfortWeight: Double = 0.3,
    customWeights: Map[String, Double] = Map.empty
  )

  /** 行为规划配置 */
  case class BehaviorPlanningConfig(
    horizon: Duration = Duration.fromSeconds(10), // 规划时域
    resolution: Duration = Duration.fromMillis(100), // 时间分辨率
//                                     decisionMaking: DecisionMakingConfig,
//                                     interaction: InteractionModel
  )

  /** 运动规划配置 */
  case class MotionPlanningConfig(
    algorithm: MotionPlanningAlgorithm,
//                                   dynamics: DynamicsModel,
//                                   bounds: MotionBounds,
//                                   smoothing: SmoothingConfig
  )

  enum MotionPlanningAlgorithm:
    case Lattice, OptimizationBased, SamplingBased, LearningBased

  /** 重规划配置 */
  case class ReplanningConfig(
    trigger: ReplanningTrigger,
    frequency: Duration = Duration.fromSeconds(1),
    maxReplanningRate: Double = 5.0 // 最大重规划频率 Hz
  )

  case class ReplanningTrigger(
    deviation: Double = 2.0, // 路径偏离阈值
    obstacleAppearance: Boolean = true,
    goalChange: Boolean = true,
    timeout: Duration = Duration.fromSeconds(2)
  )
