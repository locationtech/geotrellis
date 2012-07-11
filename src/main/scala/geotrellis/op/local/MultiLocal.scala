package geotrellis.op.local

import annotation.tailrec

import geotrellis._
import geotrellis.op._
import geotrellis.process._


/**
 *
 */
trait MultiLocalArray extends Op[Raster] {
  def op:Op[Array[Raster]]

  def _run(context:Context) = runAsync(op :: Nil)

  val nextSteps:Steps = {
    case (rasters:Array[Raster]) :: Nil => handleRasters(rasters)
  }

  def handle(z1:Int, z2:Int):Int

  def handleRasters(rasters:Array[Raster]) = {
    val re = rasters(0).rasterExtent
    val datas = rasters.map(_.data)
    var d:RasterData = datas(0)
    var i = 1
    while (i < datas.length) {
      d = d.combine2(datas(i))(handle)
      i += 1
    }
    Result(Raster(d, re))
  }
}


/**
 *
 */
trait MultiLocal extends LocalOperation {
  def ops:Array[Op[Raster]]

  def _run(context:Context) = runAsync(ops.toList)

  val nextSteps:Steps = {
    case rasters:List[_] => handleRasters(rasters.asInstanceOf[List[Raster]])
  }

  def handle(z1:Int, z2:Int):Int
  
  @tailrec final def reduce(d:RasterData, rasters:List[Raster]):RasterData = {
    rasters match {
      case Nil => d
      case r :: rs => reduce(d.combine2(r.data)(handle), rs)
    }
  }

  def handleRasters(rasters:List[Raster]) = {
    val (r :: rs) = rasters
    Result(Raster(reduce(r.data, rs), r.rasterExtent))
  }
}
