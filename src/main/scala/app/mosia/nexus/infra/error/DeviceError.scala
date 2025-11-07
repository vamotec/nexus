package app.mosia.nexus.infra.error

import sttp.model.StatusCode

object DeviceError:
  case object DeviceNotFound extends ClientError:
    override def message: String = "Device not found"

    override def code: String = "DEVICE_NOT_FOUND"

    override def httpStatus: StatusCode = StatusCode.Forbidden

  case class InvalidDeviceData(data: String) extends ClientError:
    override def message: String = s"Invalid device data: $data"

    override def code: String = "DEVICE_INVALID_DATA"

    override def httpStatus: StatusCode = StatusCode.Forbidden

  case class DeviceLimitExceeded(limit: Int) extends ClientError:
    override def message: String = s"Device limit exceeded: $limit"

    override def code: String = "DEVICE_LIMIT_EXCEEDED"

    override def httpStatus: StatusCode = StatusCode.Forbidden
