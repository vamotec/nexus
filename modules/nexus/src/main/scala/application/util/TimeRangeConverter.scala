package app.mosia.nexus
package application.util

import domain.model.common.*

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import scala.util.{Failure, Success, Try}

import zio.json.*
import zio.*

object TimeRangeConverter:
  // 支持的时间格式
  private val formats = List(
    DateTimeFormatter.ISO_INSTANT, // 2024-01-15T10:30:00Z
    DateTimeFormatter.ISO_OFFSET_DATE_TIME, // 2024-01-15T10:30:00+08:00
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"), // 2024-01-15T10:30:00
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 2024-01-15 10:30:00
    DateTimeFormatter.ofPattern("yyyy-MM-dd") // 2024-01-15
  )

  def parse(str: StringTimeRange): IO[String, AbsoluteTimeRange] =
    for
      start <- parseFlexible(str.start, "start")
      end <- parseFlexible(str.end, "end")
      _ <- validate(start, end)
    yield AbsoluteTimeRange(start, end)

  private def parseFlexible(str: String, field: String): IO[String, Instant] =
    // 尝试所有格式
    ZIO
      .attempt {
        formats
          .foldLeft[Option[Instant]](None) { (acc, formatter) =>
            acc.orElse {
              Try {
                formatter.parse(str) match
                  case temporal if temporal.isSupported(java.time.temporal.ChronoField.INSTANT_SECONDS) =>
                    Instant.from(temporal)
                  case temporal =>
                    // 如果只有日期,转为当天开始时间
                    java.time.LocalDateTime
                      .from(temporal)
                      .atZone(ZoneOffset.UTC)
                      .toInstant
              }.toOption
            }
          }
          .getOrElse(throw new IllegalArgumentException(s"Cannot parse: $str"))
      }
      .mapError(e => s"Invalid $field timestamp '$str': ${e.getMessage}")

  private def validate(start: Instant, end: Instant): IO[String, Unit] =
    if (start.isAfter(end))
      ZIO.fail("Start time must be before end time")
    else
      ZIO.unit
