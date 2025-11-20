package app.mosia.nexus
package domain.model.grpc

import domain.config.neuro.ClustersConfig

import java.util.concurrent.atomic.{AtomicInteger, AtomicReferenceArray}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import zio.json.*
import zio.*
import zio.json.ast.Json

final case class ChannelPool(
  channels: AtomicReferenceArray[ManagedChannel],
  pointer: AtomicInteger,
  size: Int
):
  /** 轮询选取 channel */
  def pick(): ManagedChannel =
    val idx = Math.abs(pointer.getAndIncrement()) % size
    channels.get(idx)

object ChannelPool:
  def make(clusterCfg: ClustersConfig): ZIO[Scope, Throwable, ChannelPool] =
    val n = if clusterCfg.grpc.poolSize > 0 then clusterCfg.grpc.poolSize else 4

    ZIO
      .foreach(0 until n) { i =>
        val builder = ManagedChannelBuilder
          .forAddress(clusterCfg.grpc.connection.endpoint.host, clusterCfg.grpc.connection.endpoint.port)

        createManagedChannel(builder, clusterCfg)
      }
      .map { channels =>
        val arr = new AtomicReferenceArray[ManagedChannel](n)
        channels.zipWithIndex.foreach { case (channel, i) =>
          arr.set(i, channel)
        }
        ChannelPool(arr, AtomicInteger(0), n)
      }

  private def createManagedChannel(
    builder: ManagedChannelBuilder[?],
    cfg: ClustersConfig
  ): ZIO[Scope, Throwable, ManagedChannel] =
    ZIO.acquireRelease {
      ZIO.attempt {
        if (cfg.grpc.connection.transport.usePlaintext) {
          builder.usePlaintext() // 明文
        } else {
          builder.useTransportSecurity() // TLS
        }

        builder.maxInboundMessageSize(cfg.grpc.connection.limits.maxInboundMessageSize)

        builder.keepAliveTime(
          cfg.grpc.connection.keepAlive.time.toSeconds,
          java.util.concurrent.TimeUnit.SECONDS
        )

        builder.keepAliveTimeout(
          cfg.grpc.connection.keepAlive.timeout.toSeconds,
          java.util.concurrent.TimeUnit.SECONDS
        )

        builder.idleTimeout(
          cfg.grpc.connection.transport.idleTimeout.toSeconds,
          java.util.concurrent.TimeUnit.SECONDS
        )

        builder.build()
      }
    } { channel =>
      ZIO.attempt(channel.shutdown()).ignore *>
        ZIO.attempt(channel.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).ignore
    }
