package trellis.operation

import trellis._
import trellis.process._


/**
 *
 */
trait MultiLocalArray extends Op[IntRaster] {
  def op:Op[Array[IntRaster]]

  def _run(context:Context) = runAsync(op :: Nil)

  val nextSteps:Steps = {
    case (rasters:Array[IntRaster]) :: Nil => handleRasters(rasters)
  }

  def handle(z1:Int, z2:Int):Int

  def handleRasters(rasters:Array[IntRaster]) = {
    val output = rasters(0).copy()
    val outdata = output.data
    
    val rlen = rasters.length
    val dlen = outdata.length

    val datas = rasters.map(_.data)

    var i = 0
    var j = 1
    while (j < rlen) {
      val data = rasters(j).data
      i = 0
      while (i < dlen) {
        outdata(i) = handle(outdata(i), data(i))
        i += 1
      }
      j += 1
    }

    Result(output)
  }
}


/**
 *
 */
trait MultiLocal extends LocalOperation {
  def ops:Array[Op[IntRaster]]

  def _run(context:Context) = runAsync(ops.toList)

  val nextSteps:Steps = {
    case rasters:List[_] => {
      handleRasters(rasters.asInstanceOf[List[IntRaster]].toArray)
    }
  }

  def handle(z1:Int, z2:Int):Int

  def handleRaster(outdata:RasterData, data:RasterData) {
    var i = 0
    while (i < outdata.length) {
      outdata(i) = handle(outdata(i), data(i))
      i += 1
    }
  }
  
  def handleRasters(rasters:Array[IntRaster]) = {
    val output = rasters(0).copy()
    val outdata = output.data
    var j = 1
    while (j < rasters.length) {
      handleRaster(outdata, rasters(j).data)
      j += 1
    }
    Result(output)
  }
}
