package geotrellis.spark.pipeline.json

import geotrellis.spark.pipeline.PipelineConstructor

trait PipelineExpr {
  def ~(other: PipelineExpr): PipelineConstructor = this :: other :: Nil

  def ~(other: Option[PipelineExpr]): PipelineConstructor =
    other.fold(this :: Nil)(o => this :: o :: Nil)

  val `type`: String
}

