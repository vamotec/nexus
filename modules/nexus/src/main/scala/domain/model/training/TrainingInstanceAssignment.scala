package app.mosia.nexus
package domain.model.training

import domain.model.common.ValueObject

import java.time.Instant

case class TrainingInstanceAssignment(instanceId: Option[TrainingInstanceId], gpuIds: List[Int], assignedAt: Instant)
    extends ValueObject
