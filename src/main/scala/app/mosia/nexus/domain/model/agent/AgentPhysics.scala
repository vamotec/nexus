package app.mosia.nexus.domain.model.agent

import app.mosia.nexus.domain.model.common.{Dimensions, Vector3}

/** 智能体物理属性 */
case class AgentPhysics(
  mass: Double, // 质量 (kg)
  dimensions: Dimensions, // 尺寸 (长宽高)
  maxSpeed: Double, // 最大速度 (m/s)
  maxAcceleration: Double, // 最大加速度 (m/s²)
  maxDeceleration: Double, // 最大减速度 (m/s²)
  wheelbase: Option[Double], // 轴距 (车辆)
  trackWidth: Option[Double], // 轮距 (车辆)
  centerOfMass: Vector3, // 重心位置
  dragCoefficient: Double, // 风阻系数
  frictionCoefficient: Double // 摩擦系数
)
