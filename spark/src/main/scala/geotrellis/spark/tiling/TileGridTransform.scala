package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._

trait TileGridTransformDelegate extends TileGridTransform {
  val tileGridTransform: TileGridTransform

  def tileToGrid(x: Int, y: Int): GridCoord =
    tileGridTransform.tileToGrid(x, y)

  def gridToTile(col: Int, row: Int): TileCoord =
    tileGridTransform.gridToTile(col, row)
}

trait TileGridTransform {
  def tileToGrid(coord: TileCoord): GridCoord =
    tileToGrid(coord._1, coord._2)
  def tileToGrid(x: Int, y: Int): GridCoord

  def gridToTile(coord: GridCoord): TileCoord =
    gridToTile(coord._1, coord._2)
  def gridToTile(col: Int, row: Int): TileCoord
}

object TileCoordScheme {
  def fromTag(tag: String): TileCoordScheme = 
    tag match {
      case "tms" => TmsCoordScheme
      case "grid" => GridCoordScheme
      case _ => sys.error(s"Unknown index scheme $tag")
    }
}

trait TileCoordScheme { 
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
