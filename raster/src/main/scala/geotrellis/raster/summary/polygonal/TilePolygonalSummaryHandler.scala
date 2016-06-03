package geotrellis.raster.summary.polygonal

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.vector.summary.polygonal._


/**
  * Base trait for tile polygonal summary handlers.
  */
trait TilePolygonalSummaryHandler[T] extends PolygonalSummaryHandler[Polygon, Tile, T] {

  /**
    * Given a PolygonFeature, "handle" the case of an
    * entirly-contained tile.  This falls through to the
    * 'handleFullTile' handler.
    */
  def handleContains(feature: PolygonFeature[Tile]): T = handleFullTile(feature.data)

  /**
    * Given a Polygon and a PolygonFeature, "handle" the case of an
    * intersection.  This falls through to the 'handlePartialTile'
    * handler.
    */
  def handleIntersection(polygon: Polygon, feature: PolygonFeature[Tile]) = handlePartialTile(feature, polygon)

  /**
    * Given a [[Raster]] and an intersection polygon, "handle" the
    * case where there is an intersection between the raster and some
    * polygon.
    */
  def handlePartialTile(raster: Raster[Tile], intersection: Polygon): T

  /**
    * Given a tile, "handle" the case were the tile is fully
    * enveloped.
    */
  def handleFullTile(tile: Tile): T

  def combineResults(values: Seq[T]): T

  def combineOp(v1: T, v2: T): T =
    combineResults(Seq(v1, v2))
}
