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

object RasterizePolygons {
  /**
   * Rasterize an array of operations and then draw them into the provided raster.
   *
   */
  def apply(r:Op[Raster], ps:Array[Op[Polygon]]):RasterizePolygons = {
    RasterizePolygons(r, logic.CollectArray(ps))
  }

  /**
   * Rasterize an array of polygons and then draw them into the provided raster.
   *
   * Use an array of transform functions to determine the value of cells.
   */
  def apply(r:Op[Raster], ps:Array[Op[Polygon]], fs:Array[Int => Int]) = 
    RasterizePolygonsWithTransform(r, ps, fs)
}

/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
case class RasterizePolygons(r:Op[Raster], ps:Op[Array[Polygon]])
extends Op2(r, ps)({
  (r, ps) => Util.rasterize(r, ps)
})


/**
 * Rasterize an array of polygons and then draw them into the provided raster.
 */
case class RasterizePolygonsWithTransform(r:Op[Raster], ps:Array[Op[Polygon]], fs:Array[Int => Int])
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
case class RasterizePolygonsWithValue(r:Op[Raster], ps:Op[Array[Polygon]], v:Op[Int])
extends Op3(r, ps, v)({
  (r, ps, v) => Util.rasterize(r, ps, ps.map(p => (z:Int) => v))
})

object RasterizeMultiPolygon {
  def apply(r:Op[Raster], m:Op[MultiPolygon]) = {
    RasterizePolygons(r, SplitMultiPolygon(m))
  }
}

object RasterizeMultiPolygons {
  def apply(r:Op[Raster], mps:Array[Op[MultiPolygon]]):RasterizeMultiPolygons = {
    RasterizeMultiPolygons(r, logic.CollectArray(mps))
  }
}

case class RasterizeMultiPolygons(r:Op[Raster], mps:Op[Array[MultiPolygon]])
extends Op2(r, mps)({
  (r, mps) => Util.rasterize(r, mps.flatMap(_.polygons))
})
