package app.mosia.nexus.domain.model.training

import java.time.Instant

import app.mosia.nexus.domain.model.common.ValueObject

case class TrainingNodeAssignment(nodeId: TrainingNodeId, gpuIds: List[Int], assignedAt: Instant) extends ValueObject
