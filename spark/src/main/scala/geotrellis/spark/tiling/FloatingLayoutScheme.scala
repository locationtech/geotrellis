package geotrellis.spark.tiling


import geotrellis.raster._
import geotrellis.vector.{Extent, ProjectedExtent}

object FloatingLayoutScheme {
  val DEFAULT_TILE_SIZE = 256

  def apply(): FloatingLayoutScheme =
    apply(DEFAULT_TILE_SIZE)

  def apply(tileSize: Int): FloatingLayoutScheme =
    apply(tileSize, tileSize)

  def apply(tileCols: Int, tileRows: Int): FloatingLayoutScheme =
    new FloatingLayoutScheme(tileCols, tileRows)
}

class FloatingLayoutScheme(tileCols: Int, tileRows: Int) extends LayoutScheme {
  def levelFor(extent: Extent, cellSize: CellSize) =
    0 -> LayoutDefinition(GridExtent(extent, cellSize), tileCols, tileRows)

  // TODO: Fix type system so that there is no runtime error
  def zoomOut(level: LayoutLevel) =
    throw new UnsupportedOperationException("zoomOut not supported for FloatingLayoutScheme")

  def zoomIn(level: LayoutLevel) =
    throw new UnsupportedOperationException("zoomIn not supported for FloatingLayoutScheme")
}
