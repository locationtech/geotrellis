package geotrellis.spark.io

/** Names a layer and a zoom level */
case class LayerId(name: String, zoom: Int) {
  def asString = name
}
