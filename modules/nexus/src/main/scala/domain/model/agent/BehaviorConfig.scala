package app.mosia.nexus
package domain.model.agent

import domain.model.common.Behavior.*

/** 行为配置 */
case class BehaviorConfig(
  personality: PersonalityTraits,
  rules: List[BehaviorRule],
  constraints: List[BehaviorConstraint],
  emergencyBehaviors: List[EmergencyBehavior]
)
