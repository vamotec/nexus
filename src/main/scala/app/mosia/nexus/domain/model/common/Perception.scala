package app.mosia.nexus.domain.model.common

import zio.Duration

object Perception:
  /** 感知模块配置 */
  case class PerceptionConfig(
    // 传感器融合
    sensorFusion: SensorFusionConfig,

    // 目标检测
    objectDetection: ObjectDetectionConfig,

    // 跟踪算法
    tracking: TrackingConfig,

    // 场景理解
//                               sceneUnderstanding: SceneUnderstandingConfig,

    // 感知质量
    quality: PerceptionQualityConfig
  )

  /** 传感器融合配置 */
  case class SensorFusionConfig(
    algorithm: FusionAlgorithm,
    temporalAlignment: Boolean = true,
    spatialAlignment: Boolean = true,
//                                 confidenceFusion: ConfidenceFusionMethod,
//                                 outlierRejection: OutlierRejectionConfig
  )

  enum FusionAlgorithm:
    case KalmanFilter, ParticleFilter, Bayesian, DST, CNN

  /** 目标检测配置 */
  case class ObjectDetectionConfig(
    enabled: Boolean = true,
    classes: Set[String], // 检测类别
    minConfidence: Double = 0.5,
    maxRange: Double = 200.0, // 最大检测距离
//                                    roi: Option[RegionOfInterest] = None, // 感兴趣区域
//                                    postProcessing: PostProcessingConfig
  )

  /** 跟踪配置 */
  case class TrackingConfig(
    algorithm: TrackingAlgorithm,
    maxObjects: Int = 100,
    maxAge: Duration = Duration.fromSeconds(5), // 目标最大存活时间
    minHits: Int = 3, // 最小命中次数
    iouThreshold: Double = 0.3 // IoU阈值
  )

  enum TrackingAlgorithm:
    case KF, UKF, PF, SORT, DeepSORT

  /** 场景理解配置 */
//  case class SceneUnderstandingConfig(
//                                       roadDetection: RoadDetectionConfig,
//                                       laneDetection: LaneDetectionConfig,
//                                       trafficLightRecognition: TrafficLightConfig,
//                                       freeSpaceDetection: FreeSpaceConfig
//                                     )

  /** 感知质量配置 */
  case class PerceptionQualityConfig(
    accuracy: Double = 0.95,
    latency: Duration = Duration.fromMillis(100),
    reliability: Double = 0.99,
//                                      calibration: CalibrationConfig
  )
