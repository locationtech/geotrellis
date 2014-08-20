package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._

trait IndexGridTransformDelegate extends IndexGridTransform {
  val indexGridTransform: IndexGridTransform

  def indexToGrid(index: TileId): GridCoord =
    indexGridTransform.indexToGrid(index)

  def gridToIndex(col: Int, row: Int): TileId =
    indexGridTransform.gridToIndex(col, row)
}

trait IndexGridTransform {
  def indexToGrid(tileId: TileId): GridCoord
  def gridToIndex(col: Int, row: Int): TileId
  def gridToIndex(coord: GridCoord): TileId = 
    gridToIndex(coord._1, coord._2)

  def gridToIndex(gridBounds: GridBounds): Array[TileId] = {
    gridBounds.coords.map(gridToIndex(_))
  }
}

object TileIndexScheme {
  def fromTag(tag: String): TileIndexScheme = 
    tag match {
      case "row_index" => RowIndexScheme
      case _ => sys.error(s"Unknown index scheme $tag")
    }
}

trait TileIndexScheme { 
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
