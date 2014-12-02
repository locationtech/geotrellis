package geotrellis.spark.tiling

import geotrellis.raster._
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
  def levelFor(levelId: Int): LayoutLevel
  def zoomOut(level: LayoutLevel): LayoutLevel
  def zoomIn(level: LayoutLevel): LayoutLevel
}

case class LayoutLevel(zoom: Int, tileLayout: TileLayout)

/** Layout scheme for dealing with no zoom levels */
object SingleLayoutScheme {
  def apply(tileLayout: TileLayout): LayoutScheme = 
    new LayoutScheme {
      val layoutLevel = LayoutLevel(0, tileLayout)

      def levelFor(extent: Extent, cellSize: CellSize): LayoutLevel = layoutLevel
      def levelFor(levelId: Int): LayoutLevel = layoutLevel
      def zoomOut(level: LayoutLevel): LayoutLevel = layoutLevel
      def zoomIn(level: LayoutLevel): LayoutLevel = layoutLevel
    }
}
