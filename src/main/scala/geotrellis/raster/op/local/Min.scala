package geotrellis.raster.op.local

import scala.math.min

import geotrellis._
import geotrellis.process._

/**
 * Takes the Min of Ints and integer typed Rasters.
 * 
 */
object Min {
  /** Takes the min value of two Int values */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((z1, z2) => min(z1, z2))

  /** Takes the min value of two Double values */
  def apply(x:Op[Double], y:Op[Double])(implicit d:DummyImplicit) = 
    logic.Do2(x, y)((z1, z2) => min(z1, z2))

  /** Takes a Raster and an Int, and gives a raster with each cell being
   * the min value of the original raster and the integer. See [MinConstant]]*/
  def apply(r:Op[Raster], c:Op[Int]) = MinConstant(r, c)

  /** Takes a Raster and an Int, and gives a raster with each cell being
   * the min value of the original raster and the integer. See [MinConstant]]*/
  def apply(c:Op[Int], r:Op[Raster])(implicit d:DummyImplicit) = MinConstant(r, c)

  /** Takes a Raster and an Double, and gives a raster with each cell being
   * the min value of the original raster and the integer. See [MinDoubleConstant]]*/
  def apply(r:Op[Raster], c:Op[Double]) = MinDoubleConstant(r, c)

  /** Takes a Raster and an Double, and gives a raster with each cell being
   * the min value of the original raster and the integer. See [MinDoubleConstant]]*/
  def apply(c:Op[Double], r:Op[Raster])(implicit d:DummyImplicit) = MinDoubleConstant(r, c)

  /** Takes two Rasters and gives a raster with the min values of the two at each cell.
   * See [[MinRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = MinRaster(r1, r2)
}

/**
 * Takes a Raster and an Int, and gives a raster with each cell being
 * the min value of the original raster and the integer.
 */
case class MinConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.dualMapIfSet(min(_, c))(min(_,c)))
})

/**
 * Takes a Raster and an Double, and gives a raster with each cell being
 * the min value of the original raster and the integer.
 */
case class MinDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r, c) ({
  (r, c) => Result(r.dualMapIfSet({z:Int => min(z, c).toInt})(min(_,c)))
})

/**
 * Takes two Rasters and gives a raster with the min values of the two at each cell.
 */
case class MinRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.dualCombine(r2)
                                   ((z1, z2) => min(z1, z2))
                                   ((z1,z2) => min(z1,z2)))
})
