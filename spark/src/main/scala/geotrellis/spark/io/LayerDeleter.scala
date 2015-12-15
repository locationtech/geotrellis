package geotrellis.spark.io

trait LayerDeleter[ID] {
  def delete(id: ID): Unit
}
