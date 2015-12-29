package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.reproject._
import geotrellis.proj4._

object MapKeyTransform {
  def apply(crs: CRS, level: LayoutLevel): MapKeyTransform =
    apply(crs.worldExtent, level.layout.layoutCols, level.layout.layoutRows)

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

  def apply(otherExtent: Extent): GridBounds = {
    val SpatialKey(colMin, rowMin) = apply(otherExtent.xmin, otherExtent.ymax)

    // Pay attention to the exclusitivity of the east and south extent border.
    val colMax = {
      val d = (otherExtent.xmax - extent.xmin) / extent.width

      if(d == math.floor(d)) { (d * layoutCols).toInt - 1 }
      else { (d * layoutCols).toInt }
    }

    val rowMax = {
      val d = (extent.ymax - otherExtent.ymin) / extent.height

      if(d == math.floor(d)) { (d * layoutRows).toInt - 1 }
      else { (d * layoutRows).toInt } 
    }

    GridBounds(colMin, rowMin, colMax, rowMax)
  }

  def apply(gridBounds: GridBounds): Extent = {
    val e1 = apply(gridBounds.colMin, gridBounds.rowMin)
    val e2 = apply(gridBounds.colMax, gridBounds.rowMax)
    e1.expandToInclude(e2)
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
