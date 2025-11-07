package app.mosia.nexus.infra.persistence.redis

import zio.redis.CodecSupplier
import zio.schema.Schema
import zio.schema.codec.{BinaryCodec, ProtobufCodec}

object ProtobufCodecSupplier extends CodecSupplier:
  override def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec
