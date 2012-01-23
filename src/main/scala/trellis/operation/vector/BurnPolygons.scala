package trellis.operation

import trellis.IntRaster
import trellis.geometry.rasterizer.Rasterizer
import trellis.process._
import trellis.geometry.Polygon
import trellis.geometry.MultiPolygon

//TODO: refactor
/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
case class BurnPolygons(r:Op[IntRaster], ps:Array[Op[Polygon]])
extends Operation[IntRaster]  {
  def _run (context:Context) = runAsync( r :: ps.toList)
  val nextSteps:Steps = {
    case (r:IntRaster) :: ps => {
      val raster = r.copy
      Rasterizer.rasterize(raster, ps.asInstanceOf[List[Polygon]].toArray)
      Result(raster)
    }
  }
}

case class BurnMultiPolygon(rr: Op[IntRaster], p: Op[MultiPolygon])
extends BurnPolygons4Base(rr, p.call(_.polygons))

case class BurnMultiPolygons(rr: Op[IntRaster],
                             ps: Array[Op[MultiPolygon]])
extends Op[IntRaster] {

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
    Result(rasterCp)
  }
}

//TODO: review these operations
/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
case class BurnPolygons2(r:Op[IntRaster], ps:Array[Op[Polygon]], fs:Array[Int => Int])
extends Operation[IntRaster] {
  def _run(context:Context) = {
    runAsync(List(r,fs) :: ps.toList)
  }

  val nextSteps:Steps = {
    case (r:IntRaster) :: fs :: p => {
      val polygons = p.asInstanceOf[Array[Polygon]] 
      val raster = r.copy
      Rasterizer.rasterize(raster, polygons, fs.asInstanceOf[Array[Int=>Int]])
      Result(raster)
    }
  }
}

/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
case class BurnPolygons3(r:Op[IntRaster], ps:Op[List[Polygon]], value:Op[Int])
extends Operation[IntRaster] {
  def _run(context:Context) = {
    runAsync( List(r, value, ps) )
  }

  val nextSteps:Steps = {
    case r :: value :: (ps:List[_]) :: Nil => runAsync(r :: value :: ps)
    case (r:IntRaster) :: (value:Int) :: ps => {
      val polygons = ps.toArray.asInstanceOf[Array[Polygon]]
      val raster = r.copy
      val fs = polygons.map(p => (z:Int) => value)
      Rasterizer.rasterize(raster, polygons, fs)
      Result(raster)
    }
  }
}


abstract class BurnPolygonsBase extends Operation[IntRaster] {
  def r:Op[IntRaster]
  def ps:Op[Array[Polygon]]

  def _run(context:Context) = runAsync(List(r,ps))

  val nextSteps:Steps = {
    case (r:IntRaster) :: (ps:Array[_]) :: Nil => runAsync(r :: ps.toList)
    case (r:IntRaster) :: pList => {
      val raster = r.copy
      val polygons = pList.toArray.asInstanceOf[Array[Polygon]]
      Rasterizer.rasterize(raster, polygons)
      Result(raster)
    }
  }
}

/**
  * Rasterize an array of polygons and then draw them into the provided raster.
  */
abstract class BurnPolygons4Base(val r:Op[IntRaster], val ps:Op[Array[Polygon]])
extends BurnPolygonsBase

case class BurnPolygons4(_r:Op[IntRaster], _ps:Op[Array[Polygon]])
extends BurnPolygons4Base(_r, _ps)
