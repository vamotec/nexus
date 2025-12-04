package app.mosia.nexus
package domain.model.outbox

/** Outbox 事件状态 */
enum OutboxStatus:
  case Pending     // 待处理
  case Processing  // 处理中
  case Published   // 已发布
  case Failed      // 失败（超过最大重试次数）

object OutboxStatus:
  def toString(status: OutboxStatus): String = status match
    case OutboxStatus.Pending => "pending"
    case OutboxStatus.Processing => "processing"
    case OutboxStatus.Published => "published"
    case OutboxStatus.Failed => "failed"

  def fromString(str: String): OutboxStatus = str.toLowerCase match
    case "pending" => OutboxStatus.Pending
    case "processing" => OutboxStatus.Processing
    case "published" => OutboxStatus.Published
    case "failed" => OutboxStatus.Failed
    case other => throw new IllegalArgumentException(s"Unknown OutboxStatus: $other")