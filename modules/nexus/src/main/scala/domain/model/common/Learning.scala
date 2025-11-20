package app.mosia.nexus
package domain.model.common

import domain.model.training.TrainingConfig

import zio.json.*
import zio.*
import zio.json.ast.Json

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

  /** 模型配置 */
  case class ModelConfig(
    architecture: ModelArchitecture,
    parameters: Map[String, Any]
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
