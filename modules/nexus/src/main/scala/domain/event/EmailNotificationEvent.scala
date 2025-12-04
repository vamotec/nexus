package app.mosia.nexus
package domain.event

import zio.json.{JsonCodec, SnakeCase, jsonMemberNames}

import java.time.Instant


/** 邮件通知事件
 *
 * 用于发送各类邮件通知
 *
 * @param eventId
 *   事件 ID
 * @param to
 *   收件人邮箱地址
 * @param subject
 *   邮件主题
 * @param body
 *   邮件正文（支持 HTML）
 * @param templateId
 *   邮件模板 ID（可选）
 * @param templateData
 *   模板数据（可选）
 * @param occurredAt
 *   事件发生时间
 */
@jsonMemberNames(SnakeCase)
case class EmailNotificationEvent(
                                   eventId: String,
                                   to: String,
                                   subject: String,
                                   body: String,
                                   templateId: Option[String] = None,
                                   templateData: Option[Map[String, String]] = None,
                                   occurredAt: Instant = Instant.now()
                                 ) extends NotificationEvent derives JsonCodec
