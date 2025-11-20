package app.mosia.nexus
package application.dto.mapper

import application.dto.model.scene.*
import domain.model.common.*
import domain.model.scene.*

import zio.json.ast.Json

/** Simulation DTO ↔ Domain 映射器
  *
  * 负责在 API 层 DTO 和 Domain 模型之间进行转换
  */
object SimulationMapper:
  /** 字符串 → Color (支持多种格式) */
  def parseColor(str: String): Option[Color] =
    str.trim.toLowerCase match
      // 预定义颜色名
      case "red" => Some(Color.Red)
      case "green" => Some(Color.Green)
      case "blue" => Some(Color.Blue)
      case "white" => Some(Color.White)
      case "black" => Some(Color.Black)

      // HEX 格式
      case hex if hex.startsWith("#") => parseHexColor(hex)

      // RGB/RGBA 格式
      case rgb if rgb.startsWith("rgb") => parseRgbColor(rgb)

      case _ => None

  /** 解析 HEX 颜色 */
  private def parseHexColor(hex: String): Option[Color] =
    val cleaned = hex.stripPrefix("#")

    cleaned.length match
      // #RGB → #RRGGBB
      case 3 =>
        for
          r <- parseHexByte(cleaned.substring(0, 1) * 2)
          g <- parseHexByte(cleaned.substring(1, 2) * 2)
          b <- parseHexByte(cleaned.substring(2, 3) * 2)
        yield Color(r / 255.0, g / 255.0, b / 255.0)

      // #RRGGBB
      case 6 =>
        for
          r <- parseHexByte(cleaned.substring(0, 2))
          g <- parseHexByte(cleaned.substring(2, 4))
          b <- parseHexByte(cleaned.substring(4, 6))
        yield Color(r / 255.0, g / 255.0, b / 255.0)

      // #RRGGBBAA
      case 8 =>
        for
          r <- parseHexByte(cleaned.substring(0, 2))
          g <- parseHexByte(cleaned.substring(2, 4))
          b <- parseHexByte(cleaned.substring(4, 6))
          a <- parseHexByte(cleaned.substring(6, 8))
        yield Color(r / 255.0, g / 255.0, b / 255.0, a / 255.0)

      case _ => None

  /** 解析 RGB/RGBA 颜色 */
  private def parseRgbColor(rgb: String): Option[Color] =
    // rgb(255, 0, 0) 或 rgba(255, 0, 0, 1.0)
    val pattern = """rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)""".r

    rgb match
      case pattern(r, g, b, a) =>
        for
          rVal <- r.toIntOption if rVal >= 0 && rVal <= 255
          gVal <- g.toIntOption if gVal >= 0 && gVal <= 255
          bVal <- b.toIntOption if bVal >= 0 && bVal <= 255
          aVal = Option(a).flatMap(_.toDoubleOption).getOrElse(1.0)
        yield Color(rVal / 255.0, gVal / 255.0, bVal / 255.0, aVal)
      case _ => None

  /** 解析两位 HEX 字节 */
  private def parseHexByte(hex: String): Option[Int] =
    try Some(Integer.parseInt(hex, 16))
    catch case _: NumberFormatException => None

  /** Color → 字符串 (HEX 格式) */
  def colorToString(color: Color): String =
    val r = (color.r * 255).toInt.min(255).max(0)
    val g = (color.g * 255).toInt.min(255).max(0)
    val b = (color.b * 255).toInt.min(255).max(0)

    if color.a < 1.0 then
      val a = (color.a * 255).toInt.min(255).max(0)
      f"#$r%02X$g%02X$b%02X$a%02X"
    else f"#$r%02X$g%02X$b%02X"
  // ============ DTO → Domain ============

  /** 场景配置 DTO → Domain */
  def toSceneConfig(dto: SceneConfigDto): Either[String, SceneConfig] =
    for
      robotType <- parseRobotType(dto.robotType)
      environment <- parseEnvironmentJson(dto.environment)
      obstacles <- dto.obstacles.traverse(toObstacle)
      sensors <- dto.sensors.traverse(toSensor)
    yield SceneConfig(
      name = dto.name,
      robotType = robotType,
      robotUrdf = dto.robotUrdf.getOrElse(s"${dto.robotType}.urdf"),
      environment = environment,
      obstacles = obstacles,
      startPosition = dto.startPosition,
      goalPosition = dto.goalPosition,
      sensors = sensors
    )

  /** 障碍物 DTO → Domain */
  def toObstacle(dto: ObstacleDto): Either[String, Obstacle] =
    for
      obstacleType <- parseObstacleType(dto.obstacleType)
      material <- dto.material.traverse(parseMaterial)
    yield Obstacle(
      id = java.util.UUID.randomUUID(),
      obstacleType = obstacleType,
      position = dto.position,
      rotation = dto.rotation.getOrElse(Quaternion(1.0, 0.0, 0.0, 0.0)),
      dimensions = dto.dimensions,
      material = material,
      dynamic = dto.dynamic
    )

  /** 传感器 DTO → Domain */
  def toSensor(dto: SensorDto): Either[String, Sensor] =
    for sensorType <- parseSensorType(dto.sensorType)
    yield Sensor(
      id = java.util.UUID.randomUUID(),
      sensorType = sensorType,
      position = dto.position,
      orientation = dto.orientation.getOrElse(Quaternion(1.0, 0.0, 0.0, 0.0)),
      config = SensorConfig(
        frequency = Some(30.0),
        range = Some(10.0),
        fov = Some(90.0),
        resolution = Some(640, 480)
      ) // 默认配置，可以从 dto.config JSON 中解析
    )

  // ============ Domain → DTO ============

  /** 场景配置 Domain → DTO */
  def toSceneConfigDto(domain: SceneConfig): SceneConfigDto =
    SceneConfigDto(
      name = domain.name,
      robotType = robotTypeToString(domain.robotType),
      robotUrdf = Some(domain.robotUrdf),
      environment = environmentToJson(domain.environment),
      startPosition = domain.startPosition,
      goalPosition = domain.goalPosition,
      obstacles = domain.obstacles.map(toObstacleDto),
      sensors = domain.sensors.map(toSensorDto),
      advancedConfig = None
    )

  /** 障碍物 Domain → DTO */
  def toObstacleDto(domain: Obstacle): ObstacleDto =
    ObstacleDto(
      obstacleType = obstacleTypeToString(domain.obstacleType),
      position = domain.position,
      rotation = Some(domain.rotation),
      dimensions = domain.dimensions,
      material = domain.material.map(materialToString),
      dynamic = domain.dynamic
    )

  /** 传感器 Domain → DTO */
  def toSensorDto(domain: Sensor): SensorDto =
    SensorDto(
      sensorType = sensorTypeToString(domain.sensorType),
      position = domain.position,
      orientation = Some(domain.orientation),
      config = None // 可以序列化 domain.config 为 JSON
    )

  // ============ 枚举转换辅助方法 ============

  /** JSON → Environment (支持简单字符串和完整对象) */
  def parseEnvironmentJson(json: Json): Either[String, Environment] =
    json match
      // 简单字符串格式: "warehouse"
      case Json.Str(typeStr) =>
        parseEnvironmentType(typeStr).map { envType =>
          Environment(
            environmentType = envType,
            lighting = LightingConfig(1.0, 1.0, 3000.0, true), // 默认光照
            physics = PhysicsConfig(-9.81, 1.0 / 60.0, 10, 6) // 默认物理
          )
        }

      // 完整对象格式: {"type": "warehouse", "lighting": {...}, "physics": {...}}
      case Json.Obj(fields) =>
        for
          typeStr <- fields
            .collectFirst { case ("type", Json.Str(s)) =>
              s
            }
            .toRight("Missing or invalid 'type' field in environment JSON")
          envType <- parseEnvironmentType(typeStr)

          // 解析 lighting (可选，使用默认值)
          lighting <- fields
            .find(_._1 == "lighting")
            .map(_._2)
            .map(parseLightingConfig)
            .getOrElse(Right(LightingConfig(1.0, 1.0, 3000.0, true)))

          // 解析 physics (可选，使用默认值)
          physics <- fields
            .find(_._1 == "physics")
            .map(_._2)
            .map(parsePhysicsConfig)
            .getOrElse(Right(PhysicsConfig(-9.81, 1.0 / 60.0, 10, 6)))
        yield Environment(
          environmentType = envType,
          lighting = lighting,
          physics = physics
        )

      case _ =>
        Left(s"Invalid environment JSON format: expected string or object")

  /** Environment → JSON */
  def environmentToJson(env: Environment): Json =
    Json.Obj(
      "type" -> Json.Str(environmentTypeToString(env.environmentType)),
      "lighting" -> Json.Obj(
        "ambientIntensity" -> Json.Num(env.lighting.ambientIntensity),
        "sunIntensity" -> Json.Num(env.lighting.sunIntensity),
        "colorTemperature" -> Json.Num(env.lighting.colorTemperature),
        "shadows" -> Json.Bool(env.lighting.shadows)
      ),
      "physics" -> Json.Obj(
        "gravity" -> Json.Num(env.physics.gravity),
        "timeStep" -> Json.Num(env.physics.timeStep),
        "subSteps" -> Json.Num(env.physics.subSteps),
        "solverIterations" -> Json.Num(env.physics.solverIterations)
      )
    )

  /** JSON → LightingConfig */
  private def parseLightingConfig(json: Json): Either[String, LightingConfig] =
    json match
      case Json.Obj(fields) =>
        for
          ambientIntensity <- fields
            .collectFirst { case ("ambientIntensity", Json.Num(n)) =>
              n.doubleValue()
            }
            .toRight("Missing or invalid 'ambientIntensity' in lighting config")
          sunIntensity <- fields
            .collectFirst { case ("sunIntensity", Json.Num(n)) =>
              n.doubleValue()
            }
            .toRight("Missing or invalid 'sunIntensity' in lighting config")
          colorTemp <- fields
            .collectFirst { case ("colorTemp", Json.Num(n)) =>
              n.doubleValue()
            }
            .toRight("Missing or invalid 'colorTemperature' in lighting config")
          shadows <- fields
            .collectFirst { case ("shadows", Json.Bool(b)) =>
              b
            }
            .toRight("Missing or invalid 'shadows' in lighting config")
        yield LightingConfig(ambientIntensity, sunIntensity, colorTemp, shadows)
      case _ =>
        Left("Invalid lighting config: expected JSON object")

  /** JSON → PhysicsConfig */
  private def parsePhysicsConfig(json: Json): Either[String, PhysicsConfig] =
    json match
      case Json.Obj(fields) =>
        for
          gravity <- fields
            .collectFirst { case ("gravity", Json.Num(n)) =>
              n.doubleValue()
            }
            .toRight("Missing or invalid 'gravity' in physics config")
          timeStep <- fields
            .collectFirst { case ("timeStep", Json.Num(n)) =>
              n.doubleValue()
            }
            .toRight("Missing or invalid 'timeStep' in physics config")
          subSteps <- fields
            .collectFirst { case ("subSteps", Json.Num(n)) =>
              n.intValue()
            }
            .toRight("Missing or invalid 'subSteps' in physics config")
          solverIterations <- fields
            .collectFirst { case ("solverIterations", Json.Num(n)) =>
              n.intValue()
            }
            .toRight("Missing or invalid 'solverIterations' in physics config")
        yield PhysicsConfig(gravity, timeStep, subSteps, solverIterations)
      case _ =>
        Left("Invalid physics config: expected JSON object")

  /** 字符串 → RobotType */
  def parseRobotType(str: String): Either[String, RobotType] =
    str.toLowerCase match
      case "franka_panda" | "franka" => Right(RobotType.FrankaPanda)
      case "ur5" => Right(RobotType.UR5)
      case "kuka" => Right(RobotType.Kuka)
      case s if s.startsWith("custom:") => Right(RobotType.Custom(s.drop(7)))
      case _ => Left(s"Unknown robot type: $str")

  /** RobotType → 字符串 */
  def robotTypeToString(robotType: RobotType): String =
    robotType match
      case RobotType.FrankaPanda => "franka_panda"
      case RobotType.UR5 => "ur5"
      case RobotType.Kuka => "kuka"
      case RobotType.Custom(name) => s"custom:$name"

  /** 字符串 → EnvironmentType */
  def parseEnvironmentType(str: String): Either[String, EnvironmentType] =
    str.toLowerCase match
      case "warehouse" => Right(EnvironmentType.Warehouse)
      case "factory" => Right(EnvironmentType.Factory)
      case "laboratory" | "lab" => Right(EnvironmentType.Laboratory)
      case "outdoor" => Right(EnvironmentType.Outdoor)
      case _ => Left(s"Unknown environment type: $str")

  /** EnvironmentType → 字符串 */
  def environmentTypeToString(envType: EnvironmentType): String =
    envType match
      case EnvironmentType.Warehouse => "warehouse"
      case EnvironmentType.Factory => "factory"
      case EnvironmentType.Laboratory => "laboratory"
      case EnvironmentType.Outdoor => "outdoor"

  /** 字符串 → ObstacleType */
  def parseObstacleType(str: String): Either[String, ObstacleType] =
    str.toLowerCase match
      case "box" => Right(ObstacleType.Box)
      case "sphere" => Right(ObstacleType.Sphere)
      case "cylinder" => Right(ObstacleType.Cylinder)
      case "mesh" => Right(ObstacleType.Mesh)
      case _ => Left(s"Unknown obstacle type: $str")

  /** ObstacleType → 字符串 */
  def obstacleTypeToString(obstacleType: ObstacleType): String =
    obstacleType match
      case ObstacleType.Box => "box"
      case ObstacleType.Sphere => "sphere"
      case ObstacleType.Cylinder => "cylinder"
      case ObstacleType.Mesh => "mesh"

  /** 字符串 → SensorType */
  def parseSensorType(str: String): Either[String, SensorType] =
    str.toLowerCase match
      case "camera" | "rgb" => Right(SensorType.Camera)
      case "lidar" => Right(SensorType.Lidar)
      case "depth" => Right(SensorType.DepthCamera)
      case "imu" => Right(SensorType.IMU)
      case "force_torque" | "ft" => Right(SensorType.ForceTorque)
      case _ => Left(s"Unknown sensor type: $str")

  /** SensorType → 字符串 */
  def sensorTypeToString(sensorType: SensorType): String =
    sensorType match
      case SensorType.Camera => "camera"
      case SensorType.Lidar => "lidar"
      case SensorType.DepthCamera => "depth"
      case SensorType.IMU => "imu"
      case SensorType.ForceTorque => "force_torque"

  /** 字符串 → Material */
  def parseMaterial(str: String): Either[String, Material] =
    parseMaterialType(str).map(MaterialPresets.fromType)

  def parseMaterialType(str: String): Either[String, MaterialType] =
    str.toLowerCase match
      case "wood" => Right(MaterialType.Wood)
      case "metal" => Right(MaterialType.Metal)
      case "plastic" => Right(MaterialType.Plastic)
      case "rubber" => Right(MaterialType.Rubber)
      case "glass" => Right(MaterialType.Glass)
      case "custom" => Right(MaterialType.Custom)
      case _ => Left(s"Unknown material: $str")

  /** Material → 字符串 */
  def materialToString(material: Material): String =
    material.name // 直接使用 name 字段

  /** MaterialDto → Material (高级用户) */
  def toMaterial(dto: MaterialDto): Material =
    Material(
      name = dto.name,
      color = dto.color.flatMap(parseColor),
      texture = None,
      friction = dto.friction,
      restitution = dto.restitution
    )

  // ============ 辅助方法 ============

  /** Traverse for Either (Scala 3 extension) */
  extension [A, B](list: List[A])
    def traverse(f: A => Either[String, B]): Either[String, List[B]] =
      list.foldRight[Either[String, List[B]]](Right(Nil)) { (a, acc) =>
        for
          b <- f(a)
          bs <- acc
        yield b :: bs
      }

  extension [A, B](opt: Option[A])
    def traverse(f: A => Either[String, B]): Either[String, Option[B]] =
      opt match
        case None => Right(None)
        case Some(a) => f(a).map(Some(_))
