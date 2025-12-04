package app.mosia.nexus
package domain.model.grpc

import domain.config.cloud.ClustersConfig

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import zio.*

import java.util.concurrent.atomic.{AtomicInteger, AtomicReferenceArray}

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
  def make(clusterCfg: ClustersConfig, target: ClusterTarget): ZIO[Scope, Throwable, ChannelPool] =
    val conn = target match
      case ClusterTarget.Neuro => clusterCfg.grpc.connection.neuro
      case ClusterTarget.Nebula => clusterCfg.grpc.connection.nebula

    val n = if clusterCfg.grpc.poolSize > 0 then clusterCfg.grpc.poolSize else 4

    ZIO
      .foreach(0 until n) { i =>
        val builder = ManagedChannelBuilder
          .forAddress(conn.host, conn.port)

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
      // 确保 release 返回 ZIO[Scope, Nothing, Any]
      (ZIO.attempt(channel.shutdown()) *>
        ZIO.attemptBlocking(
          channel.awaitTermination(500, java.util.concurrent.TimeUnit.MILLISECONDS)
        ).timeout(1.seconds))
        .ignore // 吞掉所有错误，满足 Nothing 错误类型要求
    }
