package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._

object TileSpatialKeyScheme {
  def fromTag(tag: String): TileSpatialKeyScheme = 
    tag match {
      case "row_index" => RowSpatialKeyScheme
      case _ => sys.error(s"Unknown index scheme $tag")
    }
}

/**
 * Tile SpatialKey Scheme provides an object that is able to map from
 * a given Grid Coordinate, where the origin is upper left corner
 * and tuple (x, y) represents (col, row), to a linear index.
 */
trait TileSpatialKeyScheme extends Serializable {
  def tag: String

  def apply(tileDimensions: Dimensions): SpatialKeyGridTransform =
    apply(tileDimensions._1, tileDimensions._2)

  def apply(tileCols: Int, tileRows: Int): SpatialKeyGridTransform
}

object RowSpatialKeyScheme extends TileSpatialKeyScheme {
  def tag = "row_index"

  def apply(tileCols: Int, tileRows: Int): SpatialKeyGridTransform =
    new SpatialKeyGridTransform {
      def keyToGrid(key: SpatialKey): GridCoord = {
        val row = key / tileCols
        val col = key - (row * tileCols)
        (col.toInt, row.toInt)
      }

      def gridToSpatialKey(col: Int, row: Int): SpatialKey =
        (row * tileCols) + col
    }
}
