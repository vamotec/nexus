package app.mosia.nexus.domain.model.common

import zio.Duration

import java.time.{LocalDate, LocalDateTime, LocalTime}

object Condition:
  /** 天气条件 */
  case class WeatherCondition(
    // 基本天气类型
    weatherType: WeatherType,

    // 强度参数
    intensity: Double = 1.0, // 0.0-1.0，表示天气强度

    // 温度参数
    temperature: Temperature, // 温度

    // 降水参数
    precipitation: Precipitation,

    // 能见度参数
    visibility: Visibility,

    // 风力参数
    wind: WindCondition,

    // 光照参数
    illumination: Illumination,

    // 路面条件
    roadSurface: RoadSurfaceCondition
  )

  /** 天气类型 */
  enum WeatherType:
    case Clear, Sunny, Cloudy, Overcast,
      Rain, HeavyRain, Storm,
      Snow, HeavySnow, Blizzard,
      Fog, Mist, Haze,
      Drizzle, Sleet, Hail,
      Thunderstorm, Hurricane, Tornado

  /** 温度 */
  case class Temperature(
    value: Double, // 摄氏度
    feelsLike: Double, // 体感温度
    trend: TemperatureTrend = TemperatureTrend.Stable
  )

  enum TemperatureTrend:
    case Rising, Falling, Stable

  /** 降水 */
  case class Precipitation(
    precipitationType: PrecipitationType,
    rate: Double, // 毫米/小时
    accumulation: Double = 0.0 // 累计降水量
  )

  enum PrecipitationType:
    case None, Rain, Snow, Sleet, Hail, FreezingRain

  /** 能见度 */
  case class Visibility(
    distance: Double, // 能见度距离（米）
    quality: VisibilityQuality
  )

  enum VisibilityQuality:
    case Excellent, Good, Moderate, Poor, VeryPoor, Zero

  /** 风力条件 */
  case class WindCondition(
    speed: Double, // 风速 m/s
    direction: Double, // 风向 0-360度
    gust: Double = 0.0 // 阵风速度
  )

  /** 光照条件 */
  case class Illumination(
    lightLevel: LightLevel,
    sunAngle: Double, // 太阳高度角 0-90度
    glare: Double = 0.0 // 眩光强度 0.0-1.0
  )

  enum LightLevel:
    case PitchDark, Night, Twilight, Dawn, Daylight, Dusk, Bright

  /** 路面条件 */
  case class RoadSurfaceCondition(
    wetness: Double = 0.0, // 湿润程度 0.0-1.0
    snowCover: Double = 0.0, // 积雪覆盖 0.0-1.0
    iceCover: Double = 0.0, // 结冰覆盖 0.0-1.0
    friction: Double = 1.0 // 摩擦系数 0.0-1.0
  )

  // ====================================== //

  /** 交通密度 */
  case class TrafficDensity(
    // 总体密度
    level: TrafficDensityLevel,

    // 车辆密度
    vehicleDensity: Double, // 车辆数/公里

    // 流量参数
    flow: TrafficFlow,

    // 组成分布
    composition: VehicleComposition,

    // 行为特征
    behavior: TrafficBehavior,

    // 时空变化
    variation: TrafficVariation
  )

  /** 交通密度等级 */
  enum TrafficDensityLevel:
    case Empty, VeryLight, Light, Moderate, Heavy, VeryHeavy, Gridlock

  /** 交通流量 */
  case class TrafficFlow(
    volume: Double, // 车流量 辆/小时
    speed: Double, // 平均速度 km/h
    occupancy: Double // 道路占有率 0.0-1.0
  )

  /** 车辆组成 */
  case class VehicleComposition(
    cars: Double = 0.7, // 轿车比例
    trucks: Double = 0.1, // 卡车比例
    buses: Double = 0.05, // 公交车比例
    motorcycles: Double = 0.05, // 摩托车比例
    bicycles: Double = 0.05, // 自行车比例
    others: Double = 0.05 // 其他车辆比例
  )

  /** 交通行为 */
  case class TrafficBehavior(
    aggressiveness: Double = 0.5, // 攻击性 0.0-1.0
    compliance: Double = 0.8, // 规则遵守程度 0.0-1.0
    laneChanging: LaneChangeBehavior,
    following: CarFollowingBehavior
  )

  /** 换道行为 */
  case class LaneChangeBehavior(
    frequency: Double = 0.1, // 换道频率
    gapAcceptance: Double = 2.0, // 可接受间隙（秒）
    mandatoryOnly: Boolean = false // 仅必要换道
  )

  /** 跟车行为 */
  case class CarFollowingBehavior(
    headway: Double = 2.0, // 车头时距（秒）
    reactionTime: Double = 1.0, // 反应时间（秒）
    smoothness: Double = 0.7 // 平滑程度 0.0-1.0
  )

  /** 交通变化 */
  case class TrafficVariation(
    pattern: TrafficPattern,
    peakHours: List[PeakHour],
    randomness: Double = 0.1 // 随机性 0.0-1.0
  )

  /** 交通模式 */
  enum TrafficPattern:
    case Uniform, PeakValley, Random, Oscillating, Wave, Custom

  /** 高峰时段 */
  case class PeakHour(
    start: LocalTime,
    end: LocalTime,
    multiplier: Double = 2.0 // 流量倍增系数
  )

  // ======================  //

  /** 行人密度 */
  case class PedestrianDensity(
    // 总体密度
    level: PedestrianDensityLevel,

    // 密度参数
    density: Double, // 行人/平方米

    // 流量参数
    flow: PedestrianFlow,

    // 人群组成
    composition: PedestrianComposition,

    // 行为特征
    behavior: PedestrianBehavior,

    // 分布模式
    distribution: PedestrianDistribution
  )

  /** 行人密度等级 */
  enum PedestrianDensityLevel:
    case None, VeryLow, Low, Medium, High, VeryHigh, Crowded

  /** 行人流量 */
  case class PedestrianFlow(
    volume: Double, // 人流量 人/小时
    speed: Double, // 平均速度 m/s
    directionality: Double = 0.5 // 方向性 0.0-1.0
  )

  /** 行人组成 */
  case class PedestrianComposition(
    adults: Double = 0.6, // 成年人比例
    children: Double = 0.1, // 儿童比例
    elderly: Double = 0.2, // 老年人比例
    groups: Double = 0.1, // 团体比例
    mobilityAids: Double = 0.05 // 使用辅助工具比例
  )

  /** 行人行为 */
  case class PedestrianBehavior(
    compliance: Double = 0.7, // 交通规则遵守程度
    attentiveness: Double = 0.6, // 注意力集中程度
    aggressiveness: Double = 0.3, // 攻击性
    socialBehavior: SocialBehavior,
    crossingBehavior: CrossingBehavior
  )

  /** 社会行为 */
  case class SocialBehavior(
    groupSize: Int = 1, // 平均团体大小
    interaction: Double = 0.2, // 社交互动频率
    phoneUsage: Double = 0.3 // 手机使用比例
  )

  /** 过街行为 */
  case class CrossingBehavior(
    jaywalking: Double = 0.2, // 乱穿马路概率
    waitingTime: Double = 10.0, // 平均等待时间（秒）
    gapAcceptance: Double = 3.0 // 可接受间隙（秒）
  )

  /** 行人分布 */
  case class PedestrianDistribution(
    pattern: DistributionPattern,
    hotspots: List[PedestrianHotspot],
    movement: MovementPattern
  )

  /** 分布模式 */
  enum DistributionPattern:
    case Uniform, Clustered, Linear, Random, Concentric

  /** 行人热点 */
  case class PedestrianHotspot(
    location: Position3D,
    radius: Double,
    intensity: Double // 热点强度
  )

  /** 移动模式 */
  enum MovementPattern:
    case Stationary, RandomWalk, GoalOriented, Flow, Mixed

  // ===================== //

  /** 时间设定 */
  case class TimeOfDay(
    // 时间信息
    time: LocalTime,

    // 日期信息
    date: LocalDate,

    // 光照条件
    lighting: LightingCondition,

    // 人类活动模式
    activity: HumanActivity,

    // 交通模式
    trafficPattern: TimeBasedTrafficPattern
  )

  /** 光照条件 */
  case class LightingCondition(
    naturalLight: NaturalLight,
//                                artificialLight: ArtificialLight,
    shadows: ShadowQuality,
    contrast: Double = 0.8 // 对比度 0.0-1.0
  )

  /** 自然光照 */
  case class NaturalLight(
    sunPosition: SunPosition,
    brightness: Double, // 亮度 0.0-1.0
    colorTemperature: Double = 5500.0 // 色温 Kelvin
  )

  /** 太阳位置 */
  case class SunPosition(
    altitude: Double, // 高度角 0-90度
    azimuth: Double, // 方位角 0-360度
    intensity: Double // 光照强度
  )

//  /** 人工光照 */
//  case class ArtificialLight(
//                              streetLights: StreetLightCondition,
//                              buildingLights: BuildingLightCondition,
//                              vehicleLights: VehicleLightCondition
//                            )

  /** 街道照明 */
  case class StreetLightCondition(
    enabled: Boolean,
    intensity: Double = 0.8,
    spacing: Double = 30.0, // 灯间距（米）
    coverage: Double = 0.9 // 覆盖率 0.0-1.0
  )

  /** 阴影质量 */
  enum ShadowQuality:
    case None, Soft, Sharp, Long, Distorted

  /** 人类活动 */
  case class HumanActivity(
    level: ActivityLevel,
    `type`: ActivityType,
//                            distribution: ActivityDistribution
  )

  enum ActivityLevel:
    case VeryLow, Low, Medium, High, VeryHigh

  enum ActivityType:
    case Commute, Leisure, Work, School, Shopping, Emergency

  /** 基于时间的交通模式 */
  case class TimeBasedTrafficPattern(
    period: TimePeriod,
    typicalFlow: Double, // 典型流量
    variation: Double = 0.2 // 变化幅度
  )

  /** 时间段 */
  enum TimePeriod:
    case Midnight, EarlyMorning, MorningRush, Midday,
      Afternoon, EveningRush, Night, LateNight

  // ====================================== //

  /** 道路条件 */
  case class RoadCondition(
    // 道路类型
    roadType: RoadType,

    // 路面状况
    surface: RoadSurface,

    // 几何特征
    geometry: RoadGeometry,

    // 标记和标志
    markings: RoadMarkings,

    // 障碍和施工
//                            obstacles: RoadObstacles,

    // 维护状态
    maintenance: MaintenanceCondition
  )

  /** 道路类型 */
  enum RoadType:
    case Highway, Expressway, Arterial, Collector, Local,
      Residential, Rural, Alley, ParkingLot, OffRoad

  /** 路面状况 */
  case class RoadSurface(
    material: RoadMaterial,
    condition: SurfaceCondition,
    friction: Double, // 摩擦系数 0.0-1.0
    roughness: Double, // 粗糙度 0.0-1.0
//                          damage: List[RoadDamage]
  )

  enum RoadMaterial:
    case Asphalt, Concrete, Gravel, Dirt, Cobblestone, Brick

  enum SurfaceCondition:
    case Dry, Wet, Icy, Snowy, Muddy, Oily, Flooded

  /** 道路几何 */
  case class RoadGeometry(
    lanes: List[LaneGeometry],
    curvature: Double, // 曲率
    gradient: Double, // 坡度 %
    superelevation: Double // 超高 %
  )

  /** 车道几何 */
  case class LaneGeometry(
    width: Double, // 车道宽度（米）
//                           markings: LaneMarkings,
    `type`: LaneType
  )

  enum LaneType:
    case Driving, Turning, Bus, Bicycle, Emergency, Shoulder

  /** 道路标记 */
  case class RoadMarkings(
    laneMarkings: LaneMarkingQuality,
//                           crosswalks: CrosswalkCondition,
//                           signs: TrafficSignCondition,
//                           signals: TrafficSignalCondition
  )

  enum LaneMarkingQuality:
    case Excellent, Good, Fair, Poor, Missing

//  /** 道路障碍 */
//  case class RoadObstacles(
//                            construction: List[ConstructionZone],
//                            incidents: List[TrafficIncident],
//                            parkedVehicles: List[ParkedVehicle],
//                            debris: List[Debris]
//                          )

  /** 维护状态 */
  case class MaintenanceCondition(
    lastMaintenance: LocalDate,
    condition: MaintenanceLevel,
//                                   scheduledWork: List[ScheduledMaintenance]
  )

  enum MaintenanceLevel:
    case Excellent, Good, Fair, Poor, Critical

  // ======================== //

  /** 特殊事件 */
  case class SpecialEvent(
    // 事件基本信息
    eventType: EventType,
    name: String,
    description: String,

    // 时间安排
    schedule: EventSchedule,

    // 空间影响
    location: EventLocation,

    // 影响范围
    impact: EventImpact,

    // 参与要素
    participants: EventParticipants,

    // 管理措施
    management: EventManagement
  )

  /** 事件类型 */
  enum EventType:
    case SportsEvent, Concert, Festival, Parade,
      Protest, Accident, Construction, WeatherEmergency,
      PoliticalEvent, ReligiousEvent, Market, Exhibition

  /** 事件安排 */
  case class EventSchedule(
    startTime: LocalDateTime,
    endTime: LocalDateTime,
    recurrence: RecurrencePattern,
    setupTime: Duration = Duration.fromSeconds(2 * 60 * 60),
    teardownTime: Duration = Duration.fromSeconds(1 * 60 * 60)
  )

  /** 重复模式 */
  enum RecurrencePattern:
    case OneTime, Daily, Weekly, Monthly, Yearly, Custom

  /** 事件位置 */
  case class EventLocation(
    center: Position3D,
    radius: Double, // 影响半径（米）
    affectedRoads: List[String], // 受影响道路
    venues: List[Venue]
  )

  /** 场地信息 */
  case class Venue(
    name: String,
    location: Position3D,
    capacity: Int,
//                    accessPoints: List[AccessPoint]
  )

  /** 事件影响 */
  case class EventImpact(
    trafficMultiplier: Double = 1.5, // 交通流量倍增
    pedestrianMultiplier: Double = 3.0, // 行人流量倍增
    roadClosures: List[RoadClosure],
//                          detours: List[Detour],
//                          parking: ParkingImpact
  )

  /** 道路封闭 */
  case class RoadClosure(
    road: String,
    start: Position3D,
    end: Position3D,
    duration: Duration
  )

  /** 事件参与者 */
  case class EventParticipants(
    expectedAttendance: Int,
    demographics: ParticipantDemographics,
    arrivalPattern: ArrivalPattern,
    transportation: TransportationMode
  )

  /** 参与者人口统计 */
  case class ParticipantDemographics(
    ageDistribution: Map[String, Double], // 年龄分布
    groupSize: Double = 2.5, // 平均团体大小
    localVsVisitor: Double = 0.7 // 本地人比例
  )

  /** 到达模式 */
  enum ArrivalPattern:
    case Uniform, PeakArrival, Staggered, Random

  /** 交通方式 */
  case class TransportationMode(
    car: Double = 0.4, // 汽车比例
    publicTransit: Double = 0.3, // 公共交通比例
    walking: Double = 0.2, // 步行比例
    bicycle: Double = 0.05, // 自行车比例
    other: Double = 0.05 // 其他方式比例
  )

  /** 事件管理 */
  case class EventManagement(
    security: SecurityPresence,
//                              trafficControl: TrafficControl,
//                              emergencyServices: EmergencyServices,
//                              communication: EventCommunication
  )

  /** 安保配置 */
  case class SecurityPresence(
    level: SecurityLevel,
    personnel: Int,
//                               checkpoints: List[Checkpoint]
  )

  enum SecurityLevel:
    case Normal, Enhanced, High, Maximum
