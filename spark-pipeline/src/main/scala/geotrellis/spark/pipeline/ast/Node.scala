package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline.json.PipelineExpr

import io.circe.Json
import org.apache.spark.SparkContext

trait Node[T] extends Serializable {
  def eval(implicit sc: SparkContext): T
  def arg: PipelineExpr
  def asJson: List[Json]
}
