package geotrellis.operation

import geotrellis.Raster
import geotrellis.geometry.rasterizer.Rasterizer
import geotrellis.process._
import geotrellis.geometry.Polygon
import geotrellis.geometry.MultiPolygon

// TODO: Rasterizer.rasterize() should be split into a version that mutates
// and a version that returns a copy
//
// Until then, we'll use Util.rasterize().
object Util {
  def rasterize(raster:Raster, polygons:Array[Polygon]) = {
    val copy = raster.copy()
    Rasterizer.rasterize(copy, polygons)
    Result(copy)
  }

  def rasterize(raster:Raster, polygons:Array[Polygon], fs:Array[Int => Int]) = {
    val copy = raster.copy()
    Rasterizer.rasterize(copy, polygons, fs)
    Result(copy)
  }
}

// TODO: BurnPolygons should support many different apply() methods for
// different kinds of arguments.
object BurnPolygons {
  def apply(r:Op[Raster], ps:Array[Op[Polygon]]):BurnPolygons = {
    BurnPolygons(r, logic.CollectArray(ps))
  }
}

/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
case class BurnPolygons(r:Op[Raster], ps:Op[Array[Polygon]])
extends Op2(r, ps)({
  (r, ps) => Util.rasterize(r, ps)
})


/**
 * Rasterize an array of polygons and then draw them into the provided raster.
 */
case class BurnPolygonsWithTransform(r:Op[Raster], ps:Array[Op[Polygon]], fs:Array[Int => Int])
extends Op[Raster] {
  def _run(context:Context) = runAsync(r :: logic.CollectArray(ps) :: Nil)

  val nextSteps:Steps = {
    case (r:Raster) :: (ps:Array[Polygon]) :: Nil => Util.rasterize(r, ps, fs)
  }
}

//TODO: review these operations
/**
 * Rasterize an array of polygons and then draw them into the provided raster.
 */
case class BurnPolygonsWithValue(r:Op[Raster], ps:Op[Array[Polygon]], v:Op[Int])
extends Op3(r, ps, v)({
  (r, ps, v) => Util.rasterize(r, ps, ps.map(p => (z:Int) => v))
})

object BurnMultiPolygon {
  def apply(r:Op[Raster], m:Op[MultiPolygon]) = {
    BurnPolygons(r, SplitMultiPolygon(m))
  }
}

object BurnMultiPolygons {
  def apply(r:Op[Raster], mps:Array[Op[MultiPolygon]]):BurnMultiPolygons = {
    BurnMultiPolygons(r, logic.CollectArray(mps))
  }
}

case class BurnMultiPolygons(r:Op[Raster], mps:Op[Array[MultiPolygon]])
extends Op2(r, mps)({
  (r, mps) => Util.rasterize(r, mps.flatMap(_.polygons))
})
