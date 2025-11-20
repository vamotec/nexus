package app.mosia.nexus
package domain.model.agent

/** 个性特征 */
case class PersonalityTraits(
  aggressiveness: Double, // 攻击性 0-1
  caution: Double, // 谨慎程度 0-1
  patience: Double, // 耐心程度 0-1
  obedience: Double, // 规则遵守程度 0-1
  predictability: Double // 可预测性 0-1
)
