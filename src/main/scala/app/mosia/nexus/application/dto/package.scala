package app.mosia.nexus.application

import caliban.CalibanError.ExecutionError
import caliban.InputValue
import caliban.schema.{ArgBuilder, Schema}
import zio.json.{DecoderOps, EncoderOps}
import zio.json.ast.Json

package object dto:
  given Schema[Any, Json] =
    Schema.stringSchema.contramap[Json](_.toJson)

  given ArgBuilder[Json] =
    value =>
      // 将 InputValue 转换为字符串，然后解析为 Json
      val jsonString = value.toInputString
      jsonString.fromJson[Json].left.map(err => ExecutionError(s"Invalid JSON: $err"))
