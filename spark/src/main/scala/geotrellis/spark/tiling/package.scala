package geotrellis.spark

import geotrellis.vector.Extent
import geotrellis.vector.reproject._
import geotrellis.proj4._

/**
 * This package is concerned with translation of coordinates or extents between:
 *  - geographic extents
 *  - arbitrary tiling scheme, which defines origin and and axis
 *  - arbitrary indexing scheme which linearize two dimensional tiling space
 *
 *  In order to facilitate these transformations each of the above spaces maps to
 *  a "Grid Space" which is a special case of a tiling scheme with the origin
 *  defined as upper left and (x, y) coordinate representing (col, row), Java array order.
 *
 *  @see [[geotrellis.spark.tiling.MapGridTransform]]
 *  @see [[geotrellis.spark.tiling.TileGridTransform]]
 *  @see [[geotrellis.spark.tiling.SpatialKeyGridTransform]]
 */
package object tiling {
  /**
   * Tile Coordinate in a tiling scheme. The origin and axes
   * are defined by the scheme.
   */
  type TileCoord = (Int, Int)
  /**
   * Grid Coordinate always has the upper left as the origin.
   * The tuple always interpreted a (col, row)
   */
  type GridCoord = (Int, Int)
  /**
   * Geographic Map Coordinate, uses left is the origin
   */
  type MapCoord = (Double, Double)

  private final val WORLD_WSG84 = Extent(-180, -89.99999, 179.99999, 89.99999)

  implicit class CRSWorldExtent(crs: CRS) {
    def worldExtent: Extent =
      crs match {
        case x if x == LatLng =>
          WORLD_WSG84
        case x if x == WebMercator =>
          Extent(-20037508.342789244, -20037508.342789244, 20037508.342789244, 20037508.342789244)
        case _ =>
          WORLD_WSG84.reproject(LatLng, crs)
      }
  }
}
