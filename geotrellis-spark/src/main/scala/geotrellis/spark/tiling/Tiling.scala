package geotrellis.spark.tiling

case class Tile(tx: Long, ty: Long) 

case class TileBounds(n: Long, s: Long, e: Long, w: Long) 

case class Bounds(n: Double, s: Double, e: Double, w: Double) 

case class Pixel(px: Long, py: Long)

object Bounds {
	val WORLD = new Bounds(-180, -90, 180, 90)
}