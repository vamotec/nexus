package app.mosia.nexus
package application.util

import zio.json.*
import zio.*
import zio.http.QueryParams
import zio.json.ast.Json

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

  // 新增：解析 JSON
  def parseOptionalJson(input: Option[String]): Either[String, Option[Json]] =
    import zio.json._
    import zio.json.ast.Json
  
    input match {
      case Some(jsonStr) if jsonStr.nonEmpty =>
        jsonStr.fromJson[Json] match {
          case Right(json) => Right(Some(json))
          case Left(err) => Left(s"Invalid JSON: $err")
        }
      case _ => Right(None)
    }