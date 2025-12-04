package app.mosia.nexus
package domain.model.messaging

case class StreamMessage[K, V](
  stream: K,
  id: String,
  body: Map[K, V],
  millisElapsedFromDelivery: Option[Long],
  deliveredCount: Option[Long]
)
