package trellis.operation

import trellis.constant.NODATA
import trellis.process._
import trellis.raster.IntRaster

import scala.math.{max, min, pow}

/**
 * Abstract class for all operations that are unary (operate on a single raster) and
 * are local (operate on each cell without knowledge of other cells).
 */
trait UnaryLocal extends SimpleOp[IntRaster] with LocalOp {
  val r:Op[IntRaster]

  def getCallback(context:Context): (Int) => Int

  def mergeUnaryLocals(context:Context, parentCallback:(Int) => Int): Op[IntRaster] = {
    val myCallback = getCallback(context)
    val f = (z:Int) => parentCallback(myCallback(z))
    r match {
      case (child:UnaryLocal) => child.mergeUnaryLocals(context, f)
      case child => DoCell(child, f)
    }
  }

  def _value(context:Context) = {
    val f = getCallback(context)
    r match {
      case (child:UnaryLocal) => context.run(child.mergeUnaryLocals(context, f))
      case child => context.run(child).mapIfSet(f)
    }
  }
}
