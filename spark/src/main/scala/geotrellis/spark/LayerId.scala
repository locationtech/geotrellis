package geotrellis.spark

/** Identifies a Layer by it's name and zoom level */
case class LayerId(name: String, zoom: Int)

object LayerId {
  val rx = """LayerId\((\w+),(\d+)\)""".r
  def fromString(s: String): LayerId = {
    val rx(name, zoom) = s
    LayerId(name, zoom.toInt)
  }
}
