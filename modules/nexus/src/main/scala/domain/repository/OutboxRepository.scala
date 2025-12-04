package app.mosia.nexus
package domain.repository

import domain.error.AppTask
import domain.model.outbox.{EventOutbox, OutboxStatus}

import java.util.UUID

/** Outbox 事件仓储接口 */
trait OutboxRepository:

  /** 保存 Outbox 事件（事务中调用）
    *
    * @param event
    *   Outbox 事件
    * @return
    *   保存的事件
    */
  def save(event: EventOutbox): AppTask[Unit]

  /** 批量保存 Outbox 事件（事务中调用）
    *
    * @param events
    *   Outbox 事件列表
    * @return
    *   保存的事件列表
    */
  def saveAll(events: List[EventOutbox]): AppTask[Unit]

  /** 查找待处理的事件（轮询器使用）
    *
    * 查询条件:
    *   - status = PENDING
    *   - nextRetryAt IS NULL OR nextRetryAt <= NOW()
    *
    * 使用 FOR UPDATE SKIP LOCKED 防止并发冲突
    *
    * @param limit
    *   最多返回的事件数量
    * @return
    *   待处理的事件列表
    */
  def findPendingEvents(limit: Int = 100): AppTask[List[EventOutbox]]

  /** 更新事件状态
    *
    * @param event
    *   更新后的事件
    * @return
    *   更新后的事件
    */
  def update(event: EventOutbox): AppTask[Long]

  /** 批量更新事件
    *
    * @param events
    *   更新后的事件列表
    * @return
    *   更新后的事件列表
    */
  def updateAll(events: List[EventOutbox]): AppTask[Long]

  /** 根据 ID 查找事件
    *
    * @param id
    *   事件 ID
    * @return
    *   事件（如果存在）
    */
  def findById(id: UUID): AppTask[Option[EventOutbox]]

  /** 按状态统计事件数量（监控用）
    *
    * @return
    *   Map(状态 -> 数量)
    */
  def countByStatus: AppTask[Map[OutboxStatus, Long]]

  /** 删除已发布的旧事件（清理任务）
    *
    * @param olderThan
    *   早于此时间的事件
    * @return
    *   删除的事件数量
    */
  def deletePublishedOlderThan(olderThan: java.time.Instant): AppTask[Long]

  /** 查找失败的事件（用于告警和人工干预）
    *
    * @param limit
    *   最多返回的事件数量
    * @return
    *   失败的事件列表
    */
  def findFailedEvents(limit: Int = 100): AppTask[List[EventOutbox]]
