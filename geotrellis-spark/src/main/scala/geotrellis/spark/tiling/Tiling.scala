package geotrellis.spark.tiling

import geotrellis.Extent

case class Tile(tx: Long, ty: Long) 

case class TileExtent(xmin: Long, ymin: Long, xmax: Long, ymax: Long) {
  def width = xmax - xmin + 1
  def height = ymax - ymin + 1
} 

// width/height is non-inclusive 
case class PixelExtent(xmin: Long, ymin: Long, xmax: Long, ymax: Long) {
  def width = xmax - xmin
  def height = ymax - ymin
}

case class Pixel(px: Long, py: Long)

object Bounds {
	final val World = Extent(-180, -90, 180, 90)
}