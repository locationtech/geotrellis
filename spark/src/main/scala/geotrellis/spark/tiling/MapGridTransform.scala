package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._
import geotrellis.proj4._

trait MapGridTransformDelegate extends MapGridTransform {
  val mapGridTransform: MapGridTransform

  def mapToGrid(extent: Extent): GridBounds =
    mapGridTransform.mapToGrid(extent)

  def mapToGrid(x: Double, y: Double): GridCoord =
    mapGridTransform.mapToGrid(x, y)

  def gridToMap(col: Int, row: Int): Extent =
    mapGridTransform.gridToMap(col, row)
}

object MapGridTransform {
  def apply(crs: CRS, tileDimensions: Dimensions): MapGridTransform =
    apply(crs.worldExtent, tileDimensions)

  def apply(crs: CRS, tileCols: Int, tileRows: Int): MapGridTransform =
    apply(crs.worldExtent, tileCols, tileRows)

  def apply(extent: Extent, tileDimensions: Dimensions): MapGridTransform =
    apply(extent, tileDimensions._1, tileDimensions._2)

  def apply(extent: Extent, tileCols: Int, tileRows: Int): MapGridTransform =
    new DefaultMapGridTransform(extent, tileCols, tileRows)
}

trait MapGridTransform extends Serializable {
  def mapToGrid(extent: Extent): GridBounds
  def mapToGrid(coord: MapCoord): GridCoord =
    mapToGrid(coord._1, coord._2)

  def mapToGrid(x: Double, y: Double): GridCoord

  def gridToMap(coord: GridCoord): Extent =
    gridToMap(coord._1, coord._2)

  def gridToMap(col: Int, row: Int): Extent

  def gridToMap(gridBounds: GridBounds): Extent = {
    val northWest = gridToMap(gridBounds.colMin, gridBounds.rowMin)
    val southEast = gridToMap(gridBounds.colMax, gridBounds.rowMax)
    northWest.combine(southEast)
  }
}

class DefaultMapGridTransform(extent: Extent, tileCols: Int, tileRows: Int) extends MapGridTransform  {
  lazy val tileWidth: Double = extent.width / tileCols
  lazy val tileHeight: Double = extent.height / tileRows

  def mapToGrid(extent: Extent): GridBounds = {
    val (colMin, rowMin) = mapToGrid(extent.xmin, extent.ymax)
    val (colMax, rowMax) = mapToGrid(extent.xmax, extent.ymin)
    GridBounds(colMin, rowMin, colMax, rowMax)
  }

  def mapToGrid(x: Double, y: Double): GridCoord = {
    val tcol =
      ((x - extent.xmin) / extent.width) * tileCols

    val trow =
      ((extent.ymax - y) / extent.height) * tileRows

    (tcol.toInt, trow.toInt)
  }

  def gridToMap(col: Int, row: Int): Extent =
    Extent(
      extent.xmin + col * tileWidth,
      extent.ymax - (row + 1) * tileHeight,
      extent.xmin + (col + 1) * tileWidth,
      extent.ymax - row * tileHeight
    )
}
