package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.op._

trait TileIntersectionHandler[T] extends ZonalSummaryHandler[Extent, Tile, T] {
  def handleContains(feature: Feature[Extent, Tile]): T = handleFullTile(feature.data)
  def handleIntersection(polygon: Polygon, feature: Feature[Extent, Tile]) = handlePartialTile(feature, polygon)

  def handlePartialTile(raster: Raster[Tile], intersection: Polygon): T
  def handleFullTile(tile: Tile): T

  def combineResults(values: Seq[T]): T

  def combineOp(v1: T, v2: T): T =
    combineResults(Seq(v1, v2))
}
