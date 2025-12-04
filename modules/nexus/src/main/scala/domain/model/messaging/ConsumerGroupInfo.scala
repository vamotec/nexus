package app.mosia.nexus
package domain.model.messaging

/** 消费者组信息 */
case class ConsumerGroupInfo(
                              name: String,
                              consumers: Long,
                              pending: Long,
                              lastDeliveredId: String
                            )
