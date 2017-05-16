package geotrellis.spark.pipeline

trait PipelineExpr {
  def ~(other: PipelineExpr): PipelineConstructor = this :: other :: Nil

  def ~(other: Option[PipelineExpr]): PipelineConstructor =
    other.fold(this :: Nil)(o => this :: o :: Nil)

  val `type`: String
}

