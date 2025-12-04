package app.mosia.nexus
package infrastructure.persistence

import domain.error.AppTask
import domain.model.outbox.OutboxStatus.*
import domain.model.outbox.{EventOutbox, OutboxStatus}
import domain.repository.OutboxRepository
import infrastructure.persistence.BaseSource.PostgresDataSource
import infrastructure.persistence.rows.EventOutboxRow

import io.getquill.*
import io.getquill.extras.InstantOps
import zio.*

import java.time.Instant
import java.util.UUID

final class OutboxRepositoryLive(ctx: DefaultDbContext, dataSource: PostgresDataSource) 
  extends BaseRepository(ctx, dataSource) 
    with OutboxRepository:

  import ctx.*
  
  private inline def outbox = querySchema[EventOutboxRow]("event_outbox")
  
  override def save(event: EventOutbox): AppTask[Unit] = transaction:
    run(
      quote {
        outbox
          .insertValue(lift(toRow(event)))
      }
    ).unit

  override def saveAll(events: List[EventOutbox]): AppTask[Unit] = transaction:
    val rowlist = events.map(v => toRow(v))
    run(
      quote {
        liftQuery(rowlist).foreach(e => outbox.insertValue(e))
      }
    ).unit

  override def findPendingEvents(limit: Int = 100): AppTask[List[EventOutbox]] = withTransaction: conn =>
    val sql =
      """
        SELECT id, event_type, aggregate_id, aggregate_type, payload,
               status, created_at, processed_at, published_at,
               retry_count, max_retries, next_retry_at, last_error, partition_key
        FROM event_outbox
        WHERE status = ?
          AND (next_retry_at IS NULL OR next_retry_at <= ?)
        ORDER BY created_at ASC
        LIMIT ?
        FOR UPDATE SKIP LOCKED
      """

    val stmt = conn.prepareStatement(sql)
    stmt.setString(1, OutboxStatus.toString(Pending))
    stmt.setTimestamp(2, java.sql.Timestamp.from(Instant.now()))
    stmt.setInt(3, limit)

    val rs = stmt.executeQuery()
    val events = new scala.collection.mutable.ListBuffer[EventOutboxRow]()

    while (rs.next()) {
      val payloadStr = rs.getString("payload")
      val payloadJson = zio.json.ast.Json.decoder.decodeJson(payloadStr) match {
        case Right(json) => json
        case Left(err) => throw new RuntimeException(s"Failed to parse JSON payload: $err")
      }

      events += EventOutboxRow(
        id = java.util.UUID.fromString(rs.getString("id")),
        eventType = rs.getString("event_type"),
        aggregateId = rs.getString("aggregate_id"),
        aggregateType = rs.getString("aggregate_type"),
        payload = io.getquill.JsonbValue(payloadJson),
        status = rs.getString("status"),
        createdAt = rs.getTimestamp("created_at").toInstant,
        processedAt = Option(rs.getTimestamp("processed_at")).map(_.toInstant),
        publishedAt = Option(rs.getTimestamp("published_at")).map(_.toInstant),
        retryCount = rs.getInt("retry_count"),
        maxRetries = rs.getInt("max_retries"),
        nextRetryAt = Option(rs.getTimestamp("next_retry_at")).map(_.toInstant),
        lastError = Option(rs.getString("last_error")),
        partitionKey = Option(rs.getString("partition_key"))
      )
    }

    events.toList.map(toDomain)

  override def update(event: EventOutbox): AppTask[Long] = transaction:
    run(
      quote {
        outbox
          .filter(_.id == lift(event.id))
          .updateValue(lift(toRow(event)))
      }
    )

  override def updateAll(events: List[EventOutbox]): AppTask[Long] = transaction:
    ZIO.foreach(events)(event => update(event)).map(_.sum)

  override def findById(id: UUID): AppTask[Option[EventOutbox]] = runQuery:
    run(
      quote {
        outbox
          .filter(_.id == lift(id))
      }
    ).map(_.headOption).map(_.map(toDomain))

  override def countByStatus: AppTask[Map[OutboxStatus, Long]] =
    for
      results <- runQuery:
        run(
          quote:
            outbox
              .groupBy(_.status)
              .map { case (status, events) =>
                (status, events.size) 
              }
        )
      mapped <- ZIO.foreach(results) { case (statusStr, count) =>
        ZIO.attempt(OutboxStatus.fromString(statusStr) -> count)
          .either // 转换为 Either，不会失败
      }
    yield mapped.collect { case Right(pair) => pair }.toMap

  override def deletePublishedOlderThan(olderThan: Instant): AppTask[Long] = transaction:
    run(
      quote {
        outbox
          .filter(e =>
            e.status == lift(OutboxStatus.toString(Published)) &&
              e.publishedAt.exists(_ < lift(olderThan))
          )
          .delete
      }
    )

  override def findFailedEvents(limit: Int = 100): AppTask[List[EventOutbox]] = runQuery:
    run(
      quote {
        outbox
          .filter(_.status == lift(OutboxStatus.toString(Failed)))
          .sortBy(_.createdAt)(using Ord.desc)
          .take(lift(limit))
      }
    ).map(_.map(toDomain))

  // **************** 辅助函数 ****************************
    
  private def toRow(domain: EventOutbox): EventOutboxRow =
    EventOutboxRow(
      id = domain.id, 
      eventType = domain.eventType, 
      aggregateId = domain.aggregateId,
      aggregateType = domain.aggregateType,
      payload = JsonbValue(domain.payload), 
      status = domain.status.toString, 
      createdAt = domain.createdAt, 
      processedAt = domain.processedAt, 
      publishedAt = domain.publishedAt,
      retryCount = domain.retryCount,
      maxRetries = domain.maxRetries, 
      nextRetryAt = domain.nextRetryAt, 
      lastError = domain.lastError, 
      partitionKey = domain.partitionKey
    )
    
  private def toDomain(row: EventOutboxRow): EventOutbox =
    EventOutbox(
      id = row.id,
      eventType = row.eventType,
      aggregateId = row.aggregateId,
      aggregateType = row.aggregateType,
      payload = row.payload.value,
      status = OutboxStatus.fromString(row.status),
      createdAt = row.createdAt,
      processedAt = row.processedAt,
      publishedAt = row.publishedAt,
      retryCount = row.retryCount,
      maxRetries = row.maxRetries,
      nextRetryAt = row.nextRetryAt,
      lastError = row.lastError,
      partitionKey = row.partitionKey
    )
    
object OutboxRepositoryLive:
  val live: ZLayer[DefaultDbContext & PostgresDataSource, Nothing, OutboxRepository] =
    ZLayer.fromFunction(OutboxRepositoryLive(_, _))
