package app.mosia.nexus
package domain.event

import zio.json.*

import java.time.Instant

/** 通知事件基础 Trait
  *
  * 所有通知事件都应实现此 trait
  */
trait NotificationEvent:
  def eventId: String
  def occurredAt: Instant
