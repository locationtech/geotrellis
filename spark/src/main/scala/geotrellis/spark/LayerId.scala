package geotrellis.spark

/** Identifies a Layer by it's name and zoom level */
case class LayerId(name: String, zoom: Int) {
  override
  def toString: String = 
    s"""Layer(name = "$name", zoom = $zoom)"""
}
