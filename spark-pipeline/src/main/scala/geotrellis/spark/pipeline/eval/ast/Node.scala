package geotrellis.spark.pipeline.eval.ast

import geotrellis.spark.pipeline.json.PipelineExpr

import io.circe.Json
import org.apache.spark.SparkContext

trait Node[T] {
  val arg: PipelineExpr
  def asJson: List[Json]
}

trait Read[T] extends Node[T]
trait Transform[F, T] extends Node[F => T]
trait Write[T] extends Node[T]
