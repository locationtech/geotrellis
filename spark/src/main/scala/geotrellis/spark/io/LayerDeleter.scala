package geotrellis.spark.io

trait LayerDeleter[K, ID] {
  def delete(id: ID): Unit
}
