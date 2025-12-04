package app.mosia.nexus
package domain.model.messaging

/** Stream 统计信息 */
case class StreamInfo(
                       length: Long,
                       firstEntryId: Option[String],
                       lastEntryId: Option[String]
                     )

