package app.mosia.nexus.domain.model.agent

import app.mosia.nexus.domain.model.common.Behavior.*

/** 行为配置 */
case class BehaviorConfig(
  personality: PersonalityTraits,
  rules: List[BehaviorRule],
  constraints: List[BehaviorConstraint],
  emergencyBehaviors: List[EmergencyBehavior]
)
