package geotrellis.spark.pipeline.json

import geotrellis.spark.pipeline.PipelineConstructor
import geotrellis.util.LazyLogging

trait PipelineExpr extends LazyLogging {
  def ~(other: PipelineExpr): PipelineConstructor = this :: other :: Nil

  def ~(other: Option[PipelineExpr]): PipelineConstructor =
    other.fold(this :: Nil)(o => this :: o :: Nil)

  val `type`: String
}

