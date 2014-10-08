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

  implicit class SpatialKeyArrayToSpans(arr: Array[SpatialKey]) {
    def spans: Seq[(SpatialKey, SpatialKey)] = {
      val result = new scala.collection.mutable.ListBuffer[(SpatialKey, SpatialKey)]
      val sorted = arr.sorted
      val len = sorted.size
      var currMin = sorted(0)
      var currMax = sorted(0)
      var i = 1
      while(i < len) {
        val id = sorted(i)
        if(id != currMax + 1) {
          result += ((currMin, currMax))
          currMin = id
          currMax = id
        } else { 
          currMax = id 
        }
        i += 1
      }
      result += ((currMin, currMax))
      result
    }
  }



  class TileSpatialKeyMapTransformTuple(
      val tileGridTransform: TileGridTransform,
      val indexGridTransform: SpatialKeyGridTransform,
      val mapGridTransform: MapGridTransform)
    extends TileGridTransformDelegate
    with SpatialKeyGridTransformDelegate
    with MapGridTransformDelegate
    with MapSpatialKeyTransform
    with TileSpatialKeyTransform
    with TileMapTransform

  // Provides implicits to delegate transform functions
  implicit class TileSpatialKeyMapTransform1(tup: (TileGridTransform, SpatialKeyGridTransform, MapGridTransform))
    extends TileSpatialKeyMapTransformTuple(tup._1, tup._2, tup._3)
  implicit class TileSpatialKeyMapTransform2(tup: (TileGridTransform, MapGridTransform, SpatialKeyGridTransform))
    extends TileSpatialKeyMapTransformTuple(tup._1, tup._3, tup._2)
  implicit class TileSpatialKeyMapTransform3(tup: (SpatialKeyGridTransform, TileGridTransform, MapGridTransform))
    extends TileSpatialKeyMapTransformTuple(tup._2, tup._1, tup._3)
  implicit class TileSpatialKeyMapTransform4(tup: (MapGridTransform, TileGridTransform, SpatialKeyGridTransform))
    extends TileSpatialKeyMapTransformTuple(tup._2, tup._3, tup._1)
  implicit class TileSpatialKeyMapTransform5(tup: (SpatialKeyGridTransform, MapGridTransform, TileGridTransform))
    extends TileSpatialKeyMapTransformTuple(tup._3, tup._1, tup._2)
  implicit class TileSpatialKeyMapTransform6(tup: (MapGridTransform, SpatialKeyGridTransform, TileGridTransform ))
    extends TileSpatialKeyMapTransformTuple(tup._3, tup._2, tup._1)

  class TileSpatialKeyTransformTuple(
      val tileGridTransform: TileGridTransform,
      val indexGridTransform: SpatialKeyGridTransform)
    extends TileGridTransformDelegate
    with SpatialKeyGridTransformDelegate
    with TileSpatialKeyTransform

  implicit class TileSpatialKeyTransform1(tup: (TileGridTransform, SpatialKeyGridTransform))
    extends TileSpatialKeyTransformTuple(tup._1, tup._2)
  implicit class TileSpatialKeyTransform2(tup: (SpatialKeyGridTransform, TileGridTransform))
    extends TileSpatialKeyTransformTuple(tup._2, tup._1)
  
  
  class MapSpatialKeyTransformTuple(
      val indexGridTransform: SpatialKeyGridTransform,
      val mapGridTransform: MapGridTransform)
    extends SpatialKeyGridTransformDelegate
    with MapGridTransformDelegate
    with MapSpatialKeyTransform

  implicit class MapSpatialKeyTransform1(tup: (SpatialKeyGridTransform, MapGridTransform))
    extends MapSpatialKeyTransformTuple(tup._1, tup._2)
  implicit class MapSpatialKeyTransform2(tup: (MapGridTransform, SpatialKeyGridTransform))
    extends MapSpatialKeyTransformTuple(tup._2, tup._1)


  class TileMapTransformTuple(
      val tileGridTransform: TileGridTransform,
      val mapGridTransform: MapGridTransform)
    extends TileGridTransformDelegate
    with MapGridTransformDelegate
    with TileMapTransform

  implicit class TileMapTransform1(tup: (TileGridTransform, MapGridTransform))
    extends TileMapTransformTuple(tup._1, tup._2)
  implicit class TileMapTransform2(tup: (MapGridTransform, TileGridTransform))
    extends TileMapTransformTuple(tup._2, tup._1)

}
