package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline.json.PipelineExpr

import io.circe.Json
import org.apache.spark.SparkContext

trait Node[T] {
  def get(implicit sc: SparkContext): T
  def arg: PipelineExpr
  def asJson: List[Json]
}
