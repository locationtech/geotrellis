package geotrellis.tiling

import geotrellis.raster.CellSize
import geotrellis.vector.Extent

/** Layout scheme for building a local power of 2 pyramid.
  * Zooming out will reduce raster pixel resolution by 2, while using minimum number of tiles.
  * Layouts produced by this scheme will not be power of 2 however.
  * Uneven layouts will pyramid up until they are reduced to a single tile.
  */
class LocalLayoutScheme extends LayoutScheme {
  import LocalLayoutScheme._
  import math._

  def zoomOut(level: LayoutLevel): LayoutLevel = {
    val LayoutLevel(zoom, LayoutDefinition(extent, tileLayout)) = level
    require(zoom > 0)
    // layouts may be uneven, don't let the short dimension go to 0
    val outCols = max(1, pyramid(tileLayout.layoutCols))
    val outRows = max(1, pyramid(tileLayout.layoutRows))
    val outLayout = LayoutDefinition(extent, tileLayout.copy(layoutCols = outCols, layoutRows = outRows))
    LayoutLevel(zoom - 1, outLayout)
  }

  // not used in Pyramiding
  def zoomIn(level: LayoutLevel): LayoutLevel = ???
  def levelFor(extent: Extent, cellSize: CellSize): LayoutLevel = ???
}

object LocalLayoutScheme {
  import math._
  private def pow2(x: Int) = ceil(log(x)/log(2)).toInt
  private def pad2(x: Int) = pow(2, ceil(log(x)/log(2))).toInt

  /** Tiles needed if we reduced resolution by power of 2 */
  def pyramid(tiles: Int): Int = {
    val pad = pad2(tiles)
    val reducedPadded = pad / 2
    ceil(reducedPadded * (tiles.toDouble/pad.toDouble)).toInt
  }

  /** Infer zoom level by how many times this layout can be reduced before becoming a single tile */
  def inferLayoutLevel(ld: LayoutDefinition): Int =
    max(pow2(ld.tileLayout.layoutCols), pow2(ld.tileLayout.layoutRows))
}
