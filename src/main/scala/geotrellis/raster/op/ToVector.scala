package geotrellis.raster.op

import geotrellis._
import geotrellis.feature._
import geotrellis.raster.op.focal.{RegionGroup,RegionGroupResult}
import geotrellis.raster.CroppedRaster

import com.vividsolutions.jts.geom

import scala.collection.mutable

import spire.syntax._

object ToVector {
  def apply(r:Op[Raster]):Op[List[Geometry[Int]]] = {
    RegionGroup(r).flatMap { rgr =>
      ToVector(rgr.raster, { i => rgr.regionMap(i) })
    }
  }
}

/**
 * Converts a raster to a vector.
 * Currently does not work with polygons with holes.
 */
case class ToVector(r:Op[Raster], f:Int=>Int) extends Op2(r,f)({
  (r,f) =>
    val re = r.rasterExtent
    val cols = re.cols
    val rows = re.rows

    val pointMap = mutable.Map[Int,mutable.ArrayBuffer[geom.Coordinate]]()

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        val v = r.get(col,row)
        if(v != NODATA) {
          if(!pointMap.contains(v)) {
            pointMap(v) = mutable.ArrayBuffer[geom.Coordinate]()
          }
          pointMap(v) += new geom.Coordinate(re.gridColToMap(col),re.gridRowToMap(row))
        }
      }
    }

    Result(pointMap.keys
                   .map { v =>
                     val jtsMP = Feature.factory.createMultiPoint( pointMap(v).toArray )
                     val geom = jtsMP.convexHull
                     Feature(geom, f(v))
                    }
                   .toList)
})
