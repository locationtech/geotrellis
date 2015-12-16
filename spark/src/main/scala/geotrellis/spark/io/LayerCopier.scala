package geotrellis.spark.io

trait LayerCopier[ID] {
  def copy(from: ID, to: ID): Unit
}
