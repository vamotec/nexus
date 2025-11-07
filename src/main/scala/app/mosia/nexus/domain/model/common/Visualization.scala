package app.mosia.nexus.domain.model.common

object Visualization:
  /** 可视化配置 */
  case class VisualizationConfig(
    // 显示设置
    display: DisplayConfig,

    // 图层配置
    layers: List[VisualizationLayer],

    // 交互配置
//                                  interaction: InteractionConfig,

    // 性能配置
    performance: PerformanceConfig
  )

  /** 显示配置 */
  case class DisplayConfig(
    resolution: Resolution,
    frameRate: Int = 60,
    backgroundColor: Color = Color.Black,
//                            coordinateSystem: CoordinateSystem = CoordinateSystem.Cartesian
  )

  case class Resolution(width: Int, height: Int)

  /** 可视化图层 */
  case class VisualizationLayer(
    name: String,
    layerType: LayerType,
    enabled: Boolean = true,
    opacity: Double = 1.0,
    zIndex: Int = 0,
    style: VisualStyle
  )

  enum LayerType:
    case Trajectory, SensorData, Detection, Prediction,
      OccupancyGrid, Heatmap, Debug

  /** 视觉样式 */
  case class VisualStyle(
    color: Color,
    lineWidth: Double = 1.0,
    pointSize: Double = 2.0,
    fill: Boolean = false,
    texture: Option[String] = None
  )

  /** 交互配置 */
//  case class InteractionConfig(
//                                cameraControl: CameraControlConfig,
//                                selection: SelectionConfig,
//                                annotations: AnnotationConfig
//                              )

  /** 性能配置 */
  case class PerformanceConfig(
    maxElements: Int = 10000,
//                                lod: LevelOfDetailConfig,
//                                caching: CachingConfig
  )
