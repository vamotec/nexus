package app.mosia.nexus
package application.states

import domain.model.outbox.EventOutbox

/** Outbox 统计信息 */
case class OutboxStats(
                        pendingCount: Long,
                        processingCount: Long,
                        publishedCount: Long,
                        failedCount: Long,
                        recentFailures: List[EventOutbox]
                      )
