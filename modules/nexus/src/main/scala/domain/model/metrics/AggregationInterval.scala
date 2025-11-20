package app.mosia.nexus
package domain.model.metrics

enum AggregationInterval:
  case Minute, Hour, Day

object AggregationInterval:
  def fromString(str: String): AggregationInterval =
    str.toLowerCase match
      case "minite" => AggregationInterval.Minute
      case "hour" => AggregationInterval.Hour
      case "day" => AggregationInterval.Day
      case _ => throw new IllegalArgumentException(s"Unknown interval: $str")

  def toString(interval: AggregationInterval): String =
    interval match
      case AggregationInterval.Minute => "minite"
      case AggregationInterval.Hour => "hour"
      case AggregationInterval.Day => "day"
