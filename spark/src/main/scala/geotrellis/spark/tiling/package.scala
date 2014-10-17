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
      if(crs != LatLng) 
        WORLD_WSG84.reproject(LatLng, crs)
      else 
        WORLD_WSG84
  }

  // implicit class SpatialKeyArrayToSpans(arr: Array[SpatialKey]) {
  //   def spans: Seq[(SpatialKey, SpatialKey)] = {
  //     val result = new scala.collection.mutable.ListBuffer[(SpatialKey, SpatialKey)]
  //     val sorted = arr.sorted
  //     val len = sorted.size
  //     var currMin = sorted(0)
  //     var currMax = sorted(0)
  //     var i = 1
  //     while(i < len) {
  //       val id = sorted(i)
  //       if(id != currMax + 1) {
  //         result += ((currMin, currMax))
  //         currMin = id
  //         currMax = id
  //       } else { 
  //         currMax = id 
  //       }
  //       i += 1
  //     }
  //     result += ((currMin, currMax))
  //     result
  //   }
  // }

}
