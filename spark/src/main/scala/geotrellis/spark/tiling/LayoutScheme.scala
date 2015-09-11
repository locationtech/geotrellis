package geotrellis.spark.tiling

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.vector._

/** A LayoutScheme is something that provides LayoutLevels based on an integer id or 
  * an extent and cellsize. The resolution of the tiles for the LayoutLevel returned
  * will not necessarily match the CellSize provided, but an appropriately close
  * selection will be made.
  * 
  * It also provides methods for next zoomed out tile layout level.
  */
trait LayoutScheme {
  def levelFor(extent: Extent, cellSize: CellSize): LayoutLevel
  def zoomOut(level: LayoutLevel): LayoutLevel
  def zoomIn(level: LayoutLevel): LayoutLevel
}

case class LayoutLevel(zoom: Int, layout: LayoutDefinition)

object LayoutLevel {
  implicit def fromTuple(tup: (Int, LayoutDefinition)): LayoutLevel = LayoutLevel(tup._1, tup._2)
}