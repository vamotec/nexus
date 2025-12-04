package app.mosia.nexus
package domain.event

import zio.json.{JsonCodec, SnakeCase, jsonMemberNames}

import java.time.Instant

/** Webhook 通知事件
 *
 * 用于发送 Webhook 回调通知
 *
 * @param eventId
 *   事件 ID
 * @param url
 *   Webhook URL
 * @param payload
 *   回调数据
 * @param headers
 *   自定义请求头（可选）
 * @param occurredAt
 *   事件发生时间
 */
@jsonMemberNames(SnakeCase)
case class WebhookNotificationEvent(
                                     eventId: String,
                                     url: String,
                                     payload: Map[String, String],
                                     headers: Option[Map[String, String]] = None,
                                     occurredAt: Instant = Instant.now()
                                   ) extends NotificationEvent derives JsonCodec
