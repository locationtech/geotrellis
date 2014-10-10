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
 *  @see [[geotrellis.spark.tiling.IndexGridTransform]]
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
        case LatLng =>
          WORLD_WSG84
        case WebMercator =>
          Extent(-180, -85.05, 179.99999, 85.05).reproject(LatLng, WebMercator)
        case _ =>
          WORLD_WSG84.reproject(LatLng, crs)
      }
  }

  implicit class TileIdArrayToSpans(arr: Array[TileId]) {
    def spans: Seq[(TileId, TileId)] = {
      val result = new scala.collection.mutable.ListBuffer[(TileId, TileId)]
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



  class TileIndexMapTransformTuple(
      val tileGridTransform: TileGridTransform,
      val indexGridTransform: IndexGridTransform,
      val mapGridTransform: MapGridTransform)
    extends TileGridTransformDelegate
    with IndexGridTransformDelegate
    with MapGridTransformDelegate
    with MapIndexTransform
    with TileIndexTransform
    with TileMapTransform

  // Provides implicits to delegate transform functions
  implicit class TileIndexMapTransform1(tup: (TileGridTransform, IndexGridTransform, MapGridTransform))
    extends TileIndexMapTransformTuple(tup._1, tup._2, tup._3)
  implicit class TileIndexMapTransform2(tup: (TileGridTransform, MapGridTransform, IndexGridTransform))
    extends TileIndexMapTransformTuple(tup._1, tup._3, tup._2)
  implicit class TileIndexMapTransform3(tup: (IndexGridTransform, TileGridTransform, MapGridTransform))
    extends TileIndexMapTransformTuple(tup._2, tup._1, tup._3)
  implicit class TileIndexMapTransform4(tup: (MapGridTransform, TileGridTransform, IndexGridTransform))
    extends TileIndexMapTransformTuple(tup._2, tup._3, tup._1)
  implicit class TileIndexMapTransform5(tup: (IndexGridTransform, MapGridTransform, TileGridTransform))
    extends TileIndexMapTransformTuple(tup._3, tup._1, tup._2)
  implicit class TileIndexMapTransform6(tup: (MapGridTransform, IndexGridTransform, TileGridTransform ))
    extends TileIndexMapTransformTuple(tup._3, tup._2, tup._1)

  class TileIndexTransformTuple(
      val tileGridTransform: TileGridTransform,
      val indexGridTransform: IndexGridTransform)
    extends TileGridTransformDelegate
    with IndexGridTransformDelegate
    with TileIndexTransform

  implicit class TileIndexTransform1(tup: (TileGridTransform, IndexGridTransform))
    extends TileIndexTransformTuple(tup._1, tup._2)
  implicit class TileIndexTransform2(tup: (IndexGridTransform, TileGridTransform))
    extends TileIndexTransformTuple(tup._2, tup._1)
  
  
  class MapIndexTransformTuple(
      val indexGridTransform: IndexGridTransform,
      val mapGridTransform: MapGridTransform)
    extends IndexGridTransformDelegate
    with MapGridTransformDelegate
    with MapIndexTransform

  implicit class MapIndexTransform1(tup: (IndexGridTransform, MapGridTransform))
    extends MapIndexTransformTuple(tup._1, tup._2)
  implicit class MapIndexTransform2(tup: (MapGridTransform, IndexGridTransform))
    extends MapIndexTransformTuple(tup._2, tup._1)


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
