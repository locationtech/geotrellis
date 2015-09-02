package geotrellis.spark.tiling


import geotrellis.raster.{RasterExtent, CellSize}
import geotrellis.vector.{Extent, ProjectedExtent}

object FloatingLayoutScheme {
  val DEFAULT_TILE_SIZE = 256

  def apply(tileSize: Int = DEFAULT_TILE_SIZE) =
    new FloatingLayoutScheme(tileSize)
}

class FloatingLayoutScheme(tileSize: Int) extends LayoutScheme {
  def levelFor(extent: Extent, cellSize: CellSize) =
    0 -> LayoutDefinition(RasterExtent(extent, cellSize), tileSize)

  def zoomOut(level: LayoutLevel) =
    throw new UnsupportedOperationException("zoomOut not supported for FloatingLayoutScheme")

  def zoomIn(level: LayoutLevel) =
    throw new UnsupportedOperationException("zoomIn not supported for FloatingLayoutScheme")
}
