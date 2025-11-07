package app.mosia.nexus.domain.model.resource

/** 特殊硬件需求 */
case class SpecialHardware(
  deviceType: String, // e.g., "LIDAR", "RADAR", "IMU"
  vendor: Option[String] = None,
  model: Option[String] = None,
  interface: String = "USB", // e.g., "USB", "Ethernet", "PCIe"
  driverRequirements: List[String] = List.empty
)
