package app.mosia.nexus
package domain.model.resource

import domain.model.common.Bandwidth
import domain.model.network.PortRequirement

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
