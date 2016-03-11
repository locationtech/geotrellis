package geotrellis.spark.io

/** Base trait for layer headers that store location information for a saved layer */
trait LayerHeader {
  def format: String
  def keyClass: String
  def valueClass: String
}