package trellis.operation

import trellis.IntRaster
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._
import trellis.geometry.Polygon
import trellis.geometry.MultiPolygon

// TODO: Rasterizer.rasterize() should be split into a version that mutates
// and a version that returns a copy

// TODO: BurnPolygons should support many different apply() methods for
// different kinds of arguments.

object BurnPolygons {
  def apply(r:Op[IntRaster], ps:Array[Op[Polygon]]) = {
    BurnPolygons4(r, CollectArray(ps))
  }
}

/**
 * Rasterize an array of polygons and then draw them into the provided raster.
 */
case class BurnPolygons2(r:Op[IntRaster], ps:Array[Op[Polygon]], fs:Array[Int => Int])
extends Op[IntRaster] {
  def _run(context:Context) = runAsync(r :: CollectArray(ps) :: Nil)

  val nextSteps:Steps = {
    case (r:IntRaster) :: (ps:Array[Polygon]) :: Nil => {
      val copy = r.copy()
      Rasterizer.rasterize(copy, ps, fs)
      Result(copy)
    }
  }
}

/**
 * Rasterize an array of polygons and then draw them into the provided raster.
 */
case class BurnPolygons3(r:Op[IntRaster], ps:Op[List[Polygon]], v:Op[Int])
extends Op3(r, ps, v)({
  (r, ps, v) => {
    val copy = r.copy()
    val polygons = ps.toArray
    val fs = polygons.map(p => (z:Int) => v)
    Rasterizer.rasterize(copy, polygons, fs)
    Result(copy)
  }
})

/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
case class BurnPolygons4(r:Op[IntRaster], ps:Op[Array[Polygon]])
extends Op2(r, ps)({
  (r, ps) => {
    val copy = r.copy()
    Rasterizer.rasterize(copy, ps)
    Result(copy)
  }
})

object BurnMultiPolygon {
  def apply(r:Op[IntRaster], m:Op[MultiPolygon]) = {
    BurnPolygons4(r, SplitMultiPolygon(m))
  }
}

case class BurnMultiPolygons(r:Op[IntRaster], mps:Op[Array[MultiPolygon]])
extends Op2(r, mps)({
  (r, mps) => {
    val copy = r.copy()
    val ps = mps.flatMap(_.polygons)
    Rasterizer.rasterize(copy, ps)
    Result(copy)
  }
})

object BurnMultiPolygons {
  def apply(r:Op[IntRaster], mps:Array[Op[MultiPolygon]]):BurnMultiPolygons = {
    BurnMultiPolygons(r, CollectArray(mps))
  }
}
