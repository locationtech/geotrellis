package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.reproject._
import geotrellis.proj4._

object MapKeyTransform {
  def apply(crs: CRS, layoutDimensions: (Int, Int)): MapKeyTransform =
    apply(crs.worldExtent, layoutDimensions)

  def apply(crs: CRS, layoutCols: Int, layoutRows: Int): MapKeyTransform =
    apply(crs.worldExtent, layoutCols, layoutRows)

  def apply(extent: Extent, layoutDimensions: (Int, Int)): MapKeyTransform =
    apply(extent, layoutDimensions._1, layoutDimensions._2)

  def apply(extent: Extent, layoutCols: Int, layoutRows: Int): MapKeyTransform =
    new MapKeyTransform(extent, layoutCols, layoutRows)
}

/**
  * Transforms between geographic map coordinates and spatial keys.
  * Since geographic point can only be mapped to a grid tile that contains that point,
  * transformation from [[Extent]] to [[GridBounds]] to [[Extent]] will likely not
  * produce the original geographic extent, but a larger one.
  */
class MapKeyTransform(extent: Extent, layoutCols: Int, layoutRows: Int) extends Serializable {
  lazy val tileWidth: Double = extent.width / layoutCols
  lazy val tileHeight: Double = extent.height / layoutRows

  def apply(extent: Extent): GridBounds = {
    val SpatialKey(colMin, rowMin) = apply(extent.xmin, extent.ymax)
    val SpatialKey(colMax, rowMax) = apply(extent.xmax, extent.ymin)

    GridBounds(colMin, rowMin, colMax, rowMax)
  }

  def apply(p: Point): SpatialKey =
    apply(p.x, p.y)

  def apply(x: Double, y: Double): SpatialKey = {
    val tcol =
      ((x - extent.xmin) / extent.width) * layoutCols

    val trow =
      ((extent.ymax - y) / extent.height) * layoutRows

    (tcol.toInt, trow.toInt)
  }

  def apply[K: SpatialComponent](key: K): Extent = {
    apply(key.spatialComponent)
  }

  def apply(key: SpatialKey): Extent =
    apply(key.col, key.row)

  def apply(col: Int, row: Int): Extent =
    Extent(
      extent.xmin + col * tileWidth,
      extent.ymax - (row + 1) * tileHeight,
      extent.xmin + (col + 1) * tileWidth,
      extent.ymax - row * tileHeight
    )
}
