package geotrellis.spark.pointcloud

package object pipeline {
  implicit class withConstructor(that: PipelineExpr) {
    def ~(e: PipelineExpr) = PipelineConstructor(that :: e :: Nil)
  }
}
