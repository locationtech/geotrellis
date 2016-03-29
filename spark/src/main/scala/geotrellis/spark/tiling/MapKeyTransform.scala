package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.proj4._
import geotrellis.util._

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
  * transformation from Extent to GridBounds to Extent will likely not
  * produce the original geographic extent, but a larger one.
  */
class MapKeyTransform(val extent: Extent, val layoutCols: Int, val layoutRows: Int) extends Serializable {
  lazy val tileWidth: Double = extent.width / layoutCols
  lazy val tileHeight: Double = extent.height / layoutRows

  def apply(otherExtent: Extent): GridBounds = {
    val SpatialKey(colMin, rowMin) = apply(otherExtent.xmin, otherExtent.ymax)

    // For calculating GridBounds, the extent parameter is considered
    // inclusive on it's north and west borders, and execlusive on
    // it's east and south borders.
    // If the Extent has xmin == xmax and/or ymin == ymax, then consider
    // those zero length dimensions to represent the west and/or east
    // borders (so they are inclusive). In this case, the tiles returned
    // will be south and/or east of the line or point.
    val colMax = {
      val d = (otherExtent.xmax - extent.xmin) / (extent.width / layoutCols)

      if(d == math.floor(d) && d != colMin) { d.toInt - 1 }
      else { d.toInt }
    }

    val rowMax = {
      val d = (extent.ymax - otherExtent.ymin) / (extent.height / layoutRows)

      if(d == math.floor(d) && d != rowMin) { d.toInt - 1 }
      else { d.toInt }
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
    apply(key.getComponent[SpatialKey])
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
