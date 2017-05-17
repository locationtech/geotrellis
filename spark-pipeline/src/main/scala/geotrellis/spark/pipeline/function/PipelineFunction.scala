package geotrellis.spark.pipeline.function

trait PipelineFunction[V] {
  def apply(v: V): V
}
