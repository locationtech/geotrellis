package geotrellis.spark.io

trait LayerReindexer[ID] {
  def reindex(id: ID): Unit
}
