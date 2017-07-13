package geotrellis.spark.pipeline.ast

import geotrellis.spark.pipeline.json.PipelineExpr
import org.apache.spark.SparkContext

trait Node[T] {
  def get(implicit sc: SparkContext): T
  def arg: PipelineExpr
  def validation = {
    if (arg == null) (false, s"null")
    else (true, "")
  }
}

trait Read[T] extends Node[T]
trait Transform[F, T] extends Node[T]
trait Write[T] extends Node[T]
