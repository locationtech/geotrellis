package geotrellis.spark.io

trait LayerCopier[ID] {
  type Header

  def headerUpdate(id: ID, header: Header): Header

  def copy(from: ID, to: ID): Unit
}
