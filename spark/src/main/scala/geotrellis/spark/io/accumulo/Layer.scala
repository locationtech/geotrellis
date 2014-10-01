package geotrellis.spark.io.accumulo

/** Names a layer and a zoom level */
case class Layer(name: String, zoom: Int) {
  def asString = name
}