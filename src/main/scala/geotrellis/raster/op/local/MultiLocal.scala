package geotrellis.raster.op.local

import scala.annotation.tailrec

import geotrellis._

trait MultiLocal extends Op[Raster] {
  def ops:Array[Op[Raster]]

  def _run(context:Context) = runAsync(ops.toList)

  val nextSteps:Steps = {
    case rasters:List[_] => handleRasters(rasters.asInstanceOf[List[Raster]])
  }

  def handle(z1:Int, z2:Int):Int
  def handleDouble(z1:Double, z2:Double):Double

  @tailrec final def reduce(d:RasterData, rasters:List[Raster]):RasterData = {
    rasters match {
      case Nil => d
      case r :: rs => if (r.isFloat) {
        reduceDouble(d.combineDouble(r.data)(handleDouble), rs)
      } else {
        reduce(d.combine(r.data)(handle), rs)
      }
    }
  }

  @tailrec final def reduceDouble(d:RasterData, rasters:List[Raster]):RasterData = {
    rasters match {
      case Nil => d
      case r :: rs => reduceDouble(d.combineDouble(r.data)(handleDouble), rs)
    }
  }

  def handleRasters(rasters:List[Raster]) = {
    val (r :: rs) = rasters
    if (r.isFloat) {
      Result(Raster(reduceDouble(r.data, rs), r.rasterExtent))
    } else {
      Result(Raster(reduce(r.data, rs), r.rasterExtent))
    }
  }
}

trait MultiLocalArray extends Op[Raster] {
  def op:Op[Array[Raster]]

  def _run(context:Context) = runAsync(op :: Nil)

  val nextSteps:Steps = {
    case (rasters:Array[Raster]) :: Nil => handleRasters(rasters)
  }

  def handle(z1:Int, z2:Int):Int
  def handleDouble(z1:Double, z2:Double):Double

  def handleRasters(rasters:Array[Raster]) = {
    if (rasters.length == 0) {
      StepError("can't add zero-length array of rasters", "")
    } else {
      val r = rasters(0)
      val data = if (r.isFloat) {
        reduceDouble(r.data, 1, rasters)
      } else {
        reduce(r.data, 1, rasters)
      }
      Result(Raster(data, r.rasterExtent))
    }
  }

  @tailrec final def reduce(d:RasterData, i:Int, rs:Array[Raster]):RasterData = {
    if (i >= rs.length) d
    else if (rs(i).isFloat) reduceDouble(d.combineDouble(rs(i).data)(handleDouble), i + 1, rs)
    else reduce(d.combine(rs(i).data)(handle), i + 1, rs)
  }

  @tailrec final def reduceDouble(d:RasterData, i:Int, rs:Array[Raster]):RasterData = {
    if (i >= rs.length) d
    else reduceDouble(d.combineDouble(rs(i).data)(handleDouble), i + 1, rs)
  }
}
