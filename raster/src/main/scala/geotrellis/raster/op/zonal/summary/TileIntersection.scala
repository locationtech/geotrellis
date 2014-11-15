package geotrellis.raster.op.zonal.summary

import geotrellis.raster._
import geotrellis.vector.{Extent, Polygon}

abstract sealed trait TileIntersection

case class PartialTileIntersection(tile: Tile, extent: Extent, intersection: Polygon)
    extends TileIntersection {
  lazy val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)
}

case class FullTileIntersection(tile: Tile) extends TileIntersection
object FullTileIntersection {
  implicit def tileToFullTileIntersection(tile: Tile): FullTileIntersection =
    FullTileIntersection(tile)
}

trait TileIntersectionHandler[T,U] extends Function[TileIntersection, T]
    with Serializable {
  def apply(ti: TileIntersection): T =
    ti match {
      case pt: PartialTileIntersection => handlePartialTile(pt)
      case ft: FullTileIntersection => handleFullTile(ft)
    }

  def handlePartialTile(pt: PartialTileIntersection): T
  def handleFullTile(ft: FullTileIntersection): T
  def combineResults(results: Seq[T]): U
}
