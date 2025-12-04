package app.mosia.nexus
package domain.services.infra

import domain.error.AppTask

/** 邮件发送服务接口 */
trait EmailService:

  /** 发送文本邮件
    *
    * @param to
    *   收件人邮箱
    * @param subject
    *   邮件主题
    * @param body
    *   邮件正文（文本格式）
    * @return
    *   Task[Unit]
    */
  def sendText(to: String, subject: String, body: String): AppTask[Unit]

  /** 发送 HTML 邮件
    *
    * @param to
    *   收件人邮箱
    * @param subject
    *   邮件主题
    * @param htmlBody
    *   邮件正文（HTML 格式）
    * @return
    *   Task[Unit]
    */
  def sendHtml(to: String, subject: String, htmlBody: String): AppTask[Unit]

  /** 发送验证码邮件
    *
    * @param to
    *   收件人邮箱
    * @param code
    *   验证码
    * @param purpose
    *   用途（如"注册"、"登录"、"重置密码"）
    * @return
    *   Task[Unit]
    */
  def sendVerificationCode(to: String, code: String, purpose: String = "验证"): AppTask[Unit]
