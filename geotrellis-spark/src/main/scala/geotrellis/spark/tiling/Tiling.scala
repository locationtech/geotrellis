package geotrellis.spark.tiling

case class Tile(tx: Long, ty: Long) 

case class TileBounds(w: Long, s: Long, e: Long, n: Long) {
  def width = e - w + 1
  def height = n - s + 1
} 

case class PixelBounds(w: Long, s: Long, e: Long, n: Long) {
  def width = e - w + 1
  def height = n - s + 1
}

case class Bounds(w: Double, s: Double, e: Double, n: Double) 

case class Pixel(px: Long, py: Long)

object Bounds {
	val WORLD = new Bounds(-180, -90, 180, 90)
}