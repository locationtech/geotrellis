package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline.json.PipelineExpr

import io.circe.Json
import org.apache.spark.SparkContext

trait Node[T] {
  def get(implicit sc: SparkContext): T
  def arg: PipelineExpr
  def validation: (Boolean, String) = {
    if (arg == null) (false, s"null")
    else (true, "")
  }
  def asJson: List[Json]
}

trait Read[T] extends Node[T]
trait Transform[F, T] extends Node[T]
trait Write[T] extends Node[T]
