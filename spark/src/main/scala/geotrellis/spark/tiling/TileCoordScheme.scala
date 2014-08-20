package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._

object TileCoordScheme {
  def fromTag(tag: String): TileCoordScheme = 
    tag match {
      case "tms" => TmsCoordScheme
      case "grid" => GridCoordScheme
      case _ => sys.error(s"Unknown index scheme $tag")
    }
}

trait TileCoordScheme extends Serializable { 
  def tag: String 

  def apply(tileDimensions: Dimensions): TileGridTransform = 
    apply(tileDimensions._1, tileDimensions._2)

  def apply(tileCols: Int, tileRows: Int): TileGridTransform
}

object TmsCoordScheme extends TileCoordScheme {
  def tag = "tms"

  def apply(tileCols: Int, tileRows: Int): TileGridTransform =
    new TileGridTransform {
      def tileToGrid(x: Int, y: Int): GridCoord =
        (x, tileRows - y - 1)

      def gridToTile(col: Int, row: Int): TileCoord =
        (col, tileRows - row - 1)
    }
}

object GridCoordScheme extends TileCoordScheme {
  def tag = "grid"

  def apply(tileCols: Int, tileRows: Int): TileGridTransform =
    new TileGridTransform {
      def tileToGrid(x: Int, y: Int) = (x, y)
      def gridToTile(col: Int, row: Int) = (col, row)
    }
}
