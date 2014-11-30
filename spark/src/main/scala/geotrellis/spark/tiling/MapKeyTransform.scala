package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.vector.reproject._
import geotrellis.proj4._

object MapKeyTransform {
  def apply(crs: CRS, tileDimensions: Dimensions): MapKeyTransform =
    apply(crs.worldExtent, tileDimensions)

  def apply(crs: CRS, tileCols: Int, tileRows: Int): MapKeyTransform =
    apply(crs.worldExtent, tileCols, tileRows)

  def apply(extent: Extent, tileDimensions: Dimensions): MapKeyTransform =
    apply(extent, tileDimensions._1, tileDimensions._2)

  def apply(extent: Extent, tileCols: Int, tileRows: Int): MapKeyTransform =
    new MapKeyTransform(extent, tileCols, tileRows)
}

/**
  * Transforms between geographic map coordinates and spatial keys.
  * Since geographic point can only be mapped to a grid tile that contains that point,
  * transformation from [[Extent]] to [[GridBounds]] to [[Extent]] will likely not
  * produce the original geographic extent, but a larger one.
  */
class MapKeyTransform(extent: Extent, tileCols: Int, tileRows: Int) extends Serializable {
  lazy val tileWidth: Double = extent.width / tileCols
  lazy val tileHeight: Double = extent.height / tileRows

  def apply(extent: Extent): GridBounds = {
    val SpatialKey(colMin, rowMin) = apply(extent.xmin, extent.ymax)
    val SpatialKey(colMax, rowMax) = apply(extent.xmax, extent.ymin)

    GridBounds(colMin, rowMin, colMax, rowMax)
  }

  def apply(x: Double, y: Double): SpatialKey = {
    val tcol =
      ((x - extent.xmin) / extent.width) * tileCols

    val trow =
      ((extent.ymax - y) / extent.height) * tileRows

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
