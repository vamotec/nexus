package app.mosia.nexus.domain.model.resource

import app.mosia.nexus.domain.model.common.Bandwidth
import app.mosia.nexus.domain.model.network.PortRequirement
import zio.Duration

/** 网络需求 */
case class NetworkRequirements(
  bandwidth: Bandwidth,
  latency: Duration, // 最大延迟
  jitter: Duration, // 最大抖动
  packetLoss: Double, // 最大丢包率
  publicIp: Boolean = false,
  ports: List[PortRequirement],
  vpnRequired: Boolean = false
)
