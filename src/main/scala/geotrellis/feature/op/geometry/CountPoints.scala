package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.geom

/**
 * Gives a raster that represents the number of occuring points per cell.
 * 
 *  @param points               Sequence of points to be counted.
 *  @param rasterExtent         RasterExtent of the resulting raster.
 * 
 */
case class CountPoints(points:Op[Seq[Point[_]]], rasterExtent:Op[RasterExtent]) extends Op2(points,rasterExtent) ({
  (points,re) =>
    val array = Array.fill[Int](re.cols * re.rows)(0)
    for(point <- points.map { p => p.geom }) {
      val x = point.getX()
      val y = point.getY()
      if(re.extent.containsPoint(x,y)) {
        val index = re.mapXToGrid(x)*re.cols + re.mapYToGrid(y)
        array(index) = array(index) + 1
      }
    }
    Result(Raster(array,re))
})
