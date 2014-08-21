package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._

object TileIndexScheme {
  def fromTag(tag: String): TileIndexScheme = 
    tag match {
      case "row_index" => RowIndexScheme
      case _ => sys.error(s"Unknown index scheme $tag")
    }
}

/**
 * Tile Index Scheme provides an object that is able to map from
 * a given Grid Coordinate, where the origin is upper left corner
 * and tuple (x, y) represents (col, row), to a linear index.
 */
trait TileIndexScheme extends Serializable {
  def tag: String

  def apply(tileDimensions: Dimensions): IndexGridTransform =
    apply(tileDimensions._1, tileDimensions._2)

  def apply(tileCols: Int, tileRows: Int): IndexGridTransform
}

object RowIndexScheme extends TileIndexScheme {
  def tag = "row_index"

  def apply(tileCols: Int, tileRows: Int): IndexGridTransform =
    new IndexGridTransform {
      def indexToGrid(tileId: TileId): GridCoord = {
        val row = tileId / tileCols
        val col = tileId - (row * tileCols)
        (col.toInt, row.toInt)
      }

      def gridToIndex(col: Int, row: Int): TileId =
        (row * tileCols) + col
    }
}
