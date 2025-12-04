package app.mosia.nexus
package domain.event

import zio.json.{JsonCodec, SnakeCase, jsonMemberNames}

import java.time.Instant

/** 短信通知事件
 *
 * 用于发送各类短信通知
 *
 * @param eventId
 *   事件 ID
 * @param to
 *   收件人手机号
 * @param message
 *   短信内容
 * @param templateId
 *   短信模板 ID（可选）
 * @param templateData
 *   模板数据（可选）
 * @param occurredAt
 *   事件发生时间
 */
@jsonMemberNames(SnakeCase)
case class SmsNotificationEvent(
                                 eventId: String,
                                 to: String,
                                 message: String,
                                 templateId: Option[String] = None,
                                 templateData: Option[Map[String, String]] = None,
                                 occurredAt: Instant = Instant.now()
                               ) extends NotificationEvent derives JsonCodec
