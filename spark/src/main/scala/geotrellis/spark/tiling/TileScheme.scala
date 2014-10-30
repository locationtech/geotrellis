package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._

object TileScheme {
  def fromTag(tag: String): TileScheme = 
    tag match {
      case "tms" => TmsTileScheme
      case "grid" => GridTileScheme
      case _ => sys.error(s"Unknown index scheme $tag")
    }
}

/**
 * Tile Coordinate Scheme provides an object that is able to map from
 * a given Tile Coordinate Scheme to a Grid Coordinate, where the
 * origin is upper left corner and tuple (x, y) represents (col, row).
 */
trait TileScheme extends Serializable { 
  def tag: String 

  def apply(gridDimensions: Dimensions): TileKeyTransform =
    apply(gridDimensions._1, gridDimensions._2)

  def apply(tileCols: Int, tileRows: Int): TileKeyTransform
}

/** TODO: This does not seem right! Furthur research shows that TMS tiling actually
  * says that (0,0) is the top left. What gives?
  */
object TmsTileScheme extends TileScheme {
  def tag = "tms"

  def apply(tileCols: Int, tileRows: Int): TileKeyTransform =
    new TileKeyTransform {
      def tileToKey(x: Int, y: Int): SpatialKey =
        (x, tileRows - y - 1)

      def keyToTile(col: Int, row: Int): (Int, Int) =
        (col, tileRows - row - 1)
    }
}

object GridTileScheme extends TileScheme {
  def tag = "grid"

  def apply(tileCols: Int, tileRows: Int): TileKeyTransform =
    new TileKeyTransform {
      def tileToKey(x: Int, y: Int): SpatialKey = SpatialKey(x, y)
      def keyToTile(col: Int, row: Int): (Int, Int) = (col, row)
    }
}
