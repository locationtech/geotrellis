package trellis.operation

import trellis.raster.IntRaster
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._
import trellis.geometry.Polygon
import trellis.geometry.MultiPolygon

/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
case class BurnPolygons(r:Op[IntRaster], ps:Array[Op[Polygon]])
extends SimpleOp[IntRaster] {
  def childOperations = r :: ps.toList
  def _value(context:Context) = {
    // TODO: profile/optimize
    val raster   = context.run(CopyRaster(r))
    val polygons = ps.map(context.run(_))
    Rasterizer.rasterize(raster, polygons)
    raster
  }
}

case class BurnMultiPolygon(rr: Op[IntRaster], p: Op[MultiPolygon])
extends BurnPolygons4Base(rr, Call(p, (_:MultiPolygon).polygons))

case class BurnMultiPolygons(rr: Op[IntRaster],
                             ps: Array[Op[MultiPolygon]])
extends Op[IntRaster] {

  def childOperations = rr :: ps.toList
  def _run(context:Context) = runAsync(rr :: ps.toList)

  val nextSteps:Steps = {
    case raster :: polygons => { 
      step2(raster.asInstanceOf[IntRaster], polygons.asInstanceOf[List[MultiPolygon]])
    }
  }

  def step2(raster:IntRaster, polygons:List[MultiPolygon]) = {
    val rasterCp = raster.copy()
    val allPolys = polygons.map(_.polygons).flatten.toArray
    Rasterizer.rasterize(rasterCp, allPolys)
    StepResult(rasterCp)
  }
}

/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
case class BurnPolygons2(r:Op[IntRaster], ps:Array[Op[Polygon]], fs:Array[Int => Int])
extends SimpleOp[IntRaster] {
  def childOperations = r :: ps.toList
  def _value(context:Context) = {
    // TODO: profile/optimize
    val raster   = context.run(CopyRaster(r))
    val polygons = ps.map(context.run(_))
    Rasterizer.rasterize(raster, polygons, fs)
    raster
  }
}

/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
case class BurnPolygons3(r:Op[IntRaster], ps:Op[List[Polygon]], value:Int)
extends SimpleOp[IntRaster] {

  def childOperations = List(r, ps)
  def _value(context:Context) = {
    // TODO: profile/optimize
    val raster   = context.run(CopyRaster(r))
    val polygons = context.run(ps).toArray
    val fs = polygons.map(p => (z:Int) => value)

    Rasterizer.rasterize(raster, polygons, fs)
    raster
  }
}

trait BurnPolygonsBase extends SimpleOp[IntRaster] {
  def r:Op[IntRaster]
  def ps:Op[Array[Polygon]]

  def childOperations = List(r, ps)
  def _value(context:Context) = {
    // TODO: profile/optimize
    val raster   = context.run(CopyRaster(r))
    val polygons = context.run(ps)

    Rasterizer.rasterize(raster, polygons)
    raster
  }
}

/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
abstract class BurnPolygons4Base(val r:Op[IntRaster], val ps:Op[Array[Polygon]])
extends BurnPolygonsBase

case class BurnPolygons4(_r:Op[IntRaster], _ps:Op[Array[Polygon]])
extends BurnPolygons4Base(_r, _ps)
