package geotrellis.raster.op.local

import geotrellis._
import geotrellis._
import geotrellis.process._

/**
 * BinaryLocal is an abstract class for all operations that are both local (operating
 * on each cell in a raster without knowledge of other cells) and binary, by which
 * we mean that the input includes two rasters (as opposed to 'unary' or 'multi').
 */
trait BinaryLocal extends LocalOperation {
  def r1:Op[Raster]
  def r2:Op[Raster]

  def handle(z1:Int, z2:Int):Int
  def handleDouble(z1:Double, z2:Double):Double

  def _run(context:Context) = runAsync(r1 :: r2 :: Nil)

  val nextSteps:Steps = {
    case (r1:Raster) :: (r2:Raster) :: Nil => {
      if (r1.isFloat || r2.isFloat) {
        Result(r1.combineDouble(r2)((z1, z2) => handleDouble(z1, z2)))
      } else {
        Result(r1.combine(r2)((z1, z2) => handle(z1, z2)))
      }
    }
  }
}
