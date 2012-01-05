package trellis.operation

import trellis.constant.NODATA
import trellis.process._
import trellis.raster.IntRaster

import scala.math.{max, min, pow}

/**
  * Abstract class for all operations that are unary (operate on a single raster) and
  * are local (operate on each cell without knowledge of other cells).
  */
trait UnaryLocal extends SimpleOperation[IntRaster] with LocalOperation {
  val r:Op[IntRaster]

  def getCallback(context:Context): (Int) => Int

  def mergeUnaryLocals(context:Context, parentCallback:(Int) => Int): Op[IntRaster] = {
    val myCallback = getCallback(context)
    val f = (z:Int) => parentCallback(myCallback(z))
    r match {
      case child:UnaryLocal => { child.mergeUnaryLocals(context, f) }
      case child => DoCell(child, f)
    }
  }

  def _value(context:Context) = {
    val f = getCallback(context)
    r match {
      case child:UnaryLocal => {
        val op = child.mergeUnaryLocals(context, f)
        context.run(op)
      }
      case child => {
        context.run(child).mapIfSet(f)
      }
    }
  }

}

trait XYZ extends SimpleOperation[IntRaster] {
  val r:Op[IntRaster]
  def handleCell(z:Int): Int
  def _value(context:Context) = {
    val raster = context.run(r)
    val data   = raster.data
    val raster2 = raster.copy
    val data2  = raster2.data
    var i = 0
    val limit = raster.length
    while (i < limit) {
      val z = data(i)
      if (z != NODATA) {
        data2(i) = handleCell(z)
      }
      i += 1
    }
    raster2
  }
}