package trellis.operation

import trellis._

import trellis.process._

import scala.math.{max, min, pow}

/**
 * Abstract class for all operations that are unary (operate on a single raster) and
 * are local (operate on each cell without knowledge of other cells).
 */
trait UnaryLocal extends Op[IntRaster] with LocalOp {
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

  def _run(context:Context) = {
    r match {
      case (child:UnaryLocal) => runAsync('merged :: child.mergeUnaryLocals(context, getCallback(context)) :: Nil)
      case child => runAsync('normal :: child :: context :: Nil) //.mapIfSet(f)
    }
  }
  
  val nextSteps:Steps = {
    case 'merged :: (raster:IntRaster) :: Nil => Result(raster)
    case 'normal :: (raster:IntRaster) :: (context:Context) :: Nil => Result(raster.mapIfSet(getCallback(context)))
  }
}
