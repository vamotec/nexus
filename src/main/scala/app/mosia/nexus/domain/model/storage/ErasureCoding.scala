package app.mosia.nexus.domain.model.storage

case class ErasureCoding(
  dataShards: Int,
  parityShards: Int
)
