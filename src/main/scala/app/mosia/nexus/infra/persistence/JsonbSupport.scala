package app.mosia.nexus.infra.persistence

import io.getquill.MappedEncoding
import zio.json.{DecoderOps, EncoderOps}

trait JsonbSupport:
  // Map[String, String] 支持
  given mapToJson: MappedEncoding[Map[String, String], String] =
    MappedEncoding(_.toJson)

  given jsonToMap: MappedEncoding[String, Map[String, String]] =
    MappedEncoding(str => str.fromJson[Map[String, String]].getOrElse(Map.empty))

  // Option[Map[String, String]] 支持
  given optMapToOptJson: MappedEncoding[Option[Map[String, String]], Option[String]] =
    MappedEncoding(_.map(_.toJson))

  given optJsonToOptMap: MappedEncoding[Option[String], Option[Map[String, String]]] =
    MappedEncoding(_.flatMap(_.fromJson[Map[String, String]].toOption))
