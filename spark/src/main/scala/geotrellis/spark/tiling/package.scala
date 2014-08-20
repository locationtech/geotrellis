package geotrellis.spark

import geotrellis.vector.Extent
import geotrellis.vector.reproject._
import geotrellis.proj4._

package object tiling {
  type TileCoord = (Int, Int)
  type GridCoord = (Int, Int)
  type MapCoord = (Double, Double)

  private final val WORLD_WSG84 = Extent(-180, -90, 179.99999, 89.99999)

  implicit class CRSWorldExtent(crs: CRS) {
    def worldExtent: Extent =
      if(crs != LatLng) 
        WORLD_WSG84.reproject(LatLng, crs)
      else 
        WORLD_WSG84
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

  // Provides implicits to delegate transform functions
  implicit def hasIndexGridTransformToDelegate(hasTransform: { val indexGridTransform: IndexGridTransform }): IndexGridTransformDelegate =
    new IndexGridTransformDelegate { val indexGridTransform = hasTransform.indexGridTransform }

  implicit def hasMapGridTransformToDelegate(hasTransform: { val mapGridTransform: MapGridTransform }): MapGridTransformDelegate =
    new MapGridTransformDelegate { val mapGridTransform = hasTransform.mapGridTransform }

}
