package app.mosia.nexus
package domain.model.common

import domain.model.common.Learning.LearningAlgorithm
import domain.model.training.TrainingConfig

import zio.json.*
import zio.*

object Control:
  /** 控制模块配置 */
  case class ControlConfig(
    // 控制器类型
    controllerType: ControllerType,

    // PID 控制参数
    pid: Option[PIDConfig],

    // MPC 控制参数
    mpc: Option[MPCConfig],

    // 学习控制参数
    learning: Option[LearningControlConfig],

    // 执行器限制
    actuator: ActuatorConfig

    // 安全限制
//                            safety: SafetyConfig
  )

  /** 控制器类型 */
  enum ControllerType:
    case PID, MPC, LQR, Fuzzy, NeuralNetwork, Hybrid

  /** PID 配置 */
  case class PIDConfig(
    kp: Double, // 比例增益
    ki: Double, // 积分增益
    kd: Double, // 微分增益
    windupLimit: Double, // 积分抗饱和
    outputLimit: Double // 输出限制
  )

  /** MPC 配置 */
  case class MPCConfig(
    horizon: Int, // 预测时域
    dt: Duration, // 时间步长
    q: Array[Double], // 状态权重
    r: Array[Double] // 输入权重
  )

  /** 学习控制配置 */
  case class LearningControlConfig(
    algorithm: LearningAlgorithm,
    training: TrainingConfig
  )

  /** 执行器配置 */
  case class ActuatorConfig(
    delay: Duration = Duration.fromMillis(50),
    rateLimit: Double = Double.MaxValue // 最大变化率
  )
