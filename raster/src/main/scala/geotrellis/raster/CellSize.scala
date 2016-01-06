package geotrellis.raster

import geotrellis.vector.Extent

case class CellSize(width: Double, height: Double) {
  def resolution: Double = math.sqrt(width*height)
}

object CellSize {
  def apply(extent: Extent, cols: Int, rows: Int): CellSize =
    CellSize(extent.width / cols, extent.height / rows)

  def apply(extent: Extent, dims: (Int, Int)): CellSize = {
    val (cols, rows) = dims
    apply(extent, cols, rows)
  }

  def fromString(s:String) = {
    val Array(width, height) = s.split(",").map(_.toDouble)
    CellSize(width, height)
  }
}
