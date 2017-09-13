package geotrellis.raster.summary.polygonal

import geotrellis.raster._


object Implicits extends Implicits

trait Implicits {
  implicit class withSinglebandTilePolygonalSummaryMethods(
    val self: Tile
  ) extends SinglebandTilePolygonalSummaryMethods

  implicit class withMultibandTilePolygonalSummaryMethods(
    val self: MultibandTile
  ) extends MultibandTilePolygonalSummaryMethods
}
