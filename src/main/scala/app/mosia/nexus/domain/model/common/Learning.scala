package app.mosia.nexus.domain.model.common

import zio.Duration
import zio.json.JsonCodec

object Learning:
  /** 学习模块配置 */
  case class LearningConfig(
    // 算法选择
    algorithm: LearningAlgorithm,

    // 训练配置
    training: TrainingConfig,

    // 模型配置
    model: ModelConfig,

    // 数据配置
//                             data: DataConfig,

    // 评估配置
    evaluation: EvaluationConfig
  )

  /** 学习算法 */
  enum LearningAlgorithm:
    case ReinforcementLearning, ImitationLearning,
      SupervisedLearning, UnsupervisedLearning, TransferLearning

  /** 训练配置 */
  case class TrainingConfig(
    episodes: Int = 1000, // 训练回合数
    stepsPerEpisode: Int = 1000, // 每回合步数
    batchSize: Int = 32,
    learningRate: Double = 0.001,
    discountFactor: Double = 0.99,
//                             exploration: ExplorationConfig,
  ) extends ValueObject derives JsonCodec:
    def validate: Either[String, TrainingConfig] =
      if (episodes <= 0) Left("episodes must be positive")
      else if (batchSize <= 0) Left("Batch size must be positive")
      else if (learningRate <= 0) Left("Learning rate must be positive")
      else Right(this)

  /** 模型配置 */
  case class ModelConfig(
    architecture: ModelArchitecture,
    parameters: Map[String, Any],
//                          initialization: InitializationMethod,
//                          regularization: RegularizationConfig
  )

  enum ModelArchitecture:
    case MLP, CNN, RNN, Transformer, Custom

  /** 数据配置 */
//  case class DataConfig(
//                         collection: DataCollectionConfig,
//                         preprocessing: PreprocessingConfig,
//                         augmentation: AugmentationConfig
//                       )

  /** 评估配置 */
  case class EvaluationConfig(
//                               metrics: Set[EvaluationMetric],
    frequency: Duration = Duration.fromSeconds(5 * 60),
    validationSplit: Double = 0.2
  )
