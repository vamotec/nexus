package app.mosia.nexus
package domain.model.storage

case class ErasureCoding(
  dataShards: Int,
  parityShards: Int
)
