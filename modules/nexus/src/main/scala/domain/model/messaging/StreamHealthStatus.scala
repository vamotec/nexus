package app.mosia.nexus
package domain.model.messaging

/** Stream 健康状态 */
case class StreamHealthStatus(
                               streamKey: String,
                               length: Long,
                               consumerGroups: Int,
                               totalPending: Long,
                               status: String,
                               firstEntryId: Option[String],
                               lastEntryId: Option[String],
                               groups: List[ConsumerGroupInfo]
                             )
