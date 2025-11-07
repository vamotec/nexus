package app.mosia.nexus.application.util

import zio.http.*
import zio.*

object QueryParser:
  def getFirst(params: QueryParams, key: String): Option[String] =
    params.getAll(key).headOption

  def getInt(params: QueryParams, key: String, default: Int, min: Int, max: Int): Either[String, Int] =
    getFirst(params, key) match {
      case Some(str) =>
        str.toIntOption match {
          case Some(n) =>
            val clamped = n.max(min).min(max)
            Right(clamped)
          case None => Left(s"$key must be integer")
        }
      case None => Right(default)
    }

  def getString(params: QueryParams, key: String, default: String, allowed: Set[String]): Either[String, String] =
    getFirst(params, key) match {
      case Some(str) if allowed.contains(str) => Right(str)
      case Some(_) => Left(s"$key must be one of: ${allowed.mkString(", ")}")
      case None => Right(default)
    }

  def getOptionalString(params: QueryParams, key: String): Option[String] =
    getFirst(params, key).filter(_.nonEmpty)
