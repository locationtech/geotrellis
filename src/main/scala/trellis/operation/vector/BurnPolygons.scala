package trellis.operation

import trellis.IntRaster
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._
import trellis.geometry.Polygon
import trellis.geometry.MultiPolygon

// TODO: Rasterizer.rasterize() should be split into a version that mutates
// and a version that returns a copy
//
// Until then, we'll use Util.rasterize().
object Util {
  def rasterize(raster:IntRaster, polygons:Array[Polygon]) = {
    val copy = raster.copy()
    Rasterizer.rasterize(copy, polygons)
    Result(copy)
  }

  def rasterize(raster:IntRaster, polygons:Array[Polygon], fs:Array[Int => Int]) = {
    val copy = raster.copy()
    Rasterizer.rasterize(copy, polygons, fs)
    Result(copy)
  }
}

// TODO: BurnPolygons should support many different apply() methods for
// different kinds of arguments.
object BurnPolygons {
  def apply(r:Op[IntRaster], ps:Array[Op[Polygon]]):BurnPolygons = {
    BurnPolygons(r, CollectArray(ps))
  }
}

/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
case class BurnPolygons(r:Op[IntRaster], ps:Op[Array[Polygon]])
extends Op2(r, ps)({
  (r, ps) => Util.rasterize(r, ps)
})


/**
 * Rasterize an array of polygons and then draw them into the provided raster.
 */
case class BurnPolygonsWithTransform(r:Op[IntRaster], ps:Array[Op[Polygon]], fs:Array[Int => Int])
extends Op[IntRaster] {
  def _run(context:Context) = runAsync(r :: CollectArray(ps) :: Nil)

  val nextSteps:Steps = {
    case (r:IntRaster) :: (ps:Array[Polygon]) :: Nil => Util.rasterize(r, ps, fs)
  }
}

//TODO: review these operations
/**
 * Rasterize an array of polygons and then draw them into the provided raster.
 */
case class BurnPolygonsWithValue(r:Op[IntRaster], ps:Op[Array[Polygon]], v:Op[Int])
extends Op3(r, ps, v)({
  (r, ps, v) => Util.rasterize(r, ps, ps.map(p => (z:Int) => v))
})

object BurnMultiPolygon {
  def apply(r:Op[IntRaster], m:Op[MultiPolygon]) = {
    BurnPolygons(r, SplitMultiPolygon(m))
  }
}

object BurnMultiPolygons {
  def apply(r:Op[IntRaster], mps:Array[Op[MultiPolygon]]):BurnMultiPolygons = {
    BurnMultiPolygons(r, CollectArray(mps))
  }
}

case class BurnMultiPolygons(r:Op[IntRaster], mps:Op[Array[MultiPolygon]])
extends Op2(r, mps)({
  (r, mps) => Util.rasterize(r, mps.flatMap(_.polygons))
})
