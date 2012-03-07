package geotrellis.operation

import geotrellis._
import geotrellis.process._

/**
 * BinaryLocal is an abstract class for all operations that are both local (operating
 * on each cell in a raster without knowledge of other cells) and binary, by which
 * we mean that the input includes two rasters (as opposed to 'unary' or 'multi').
 */
trait BinaryLocal extends LocalOperation {
  def r1:Op[IntRaster]
  def r2:Op[IntRaster]

  def handleCells(z1:Int, z2:Int):Int

  def _run(context:Context) = runAsync(r1 :: r2 :: Nil)

  val nextSteps:Steps = {
    case (r1:IntRaster) :: (r2:IntRaster) :: Nil => {
      Result(r1.combine2(r2)(handleCells _))
    }
  }
}
