package trellis.operation

import trellis._
import trellis.process._

import scala.math.{max, min, pow}

/**
 * Multiply each cell in a raster by a constant value.
 */
trait WithIntConstant extends UnaryLocal {
  def c:Op[Int]

  def functionOps = c :: Nil

  def handleCell(z:Int, n:Int):Int

  val functionNextSteps:UnaryLocal.Steps = {
    case UnaryF(f) :: (n:Int) :: Nil => Result(UnaryF((z:Int) => handleCell(f(z), n)))
  }
}

/**
 * Multiply each cell in a raster by a constant value.
 */
trait WithDoubleConstant extends UnaryLocal {
  def c:Op[Double]

  def functionOps = c :: Nil

  def handleCell(z:Int, n:Double):Int

  val functionNextSteps:UnaryLocal.Steps = {
    case UnaryF(f) :: (n:Double) :: Nil => Result(UnaryF((z:Int) => handleCell(f(z), n)))
  }
}

/**
 * Add a constant value to each cell.
 */
case class AddConstant(r:Op[IntRaster], c:Op[Int]) extends WithIntConstant {
  //def functionOps = c :: Nil
  //val functionNextSteps:UnaryLocal.Steps = {
  //  case UnaryF(f) :: (n:Int) :: Nil => Result(UnaryF((z:Int) => f(z) + n))
  //}
  def handleCell(z:Int, c:Int) = z + c
}

case class AddLiteralConstant(r:Op[IntRaster], c:Int) extends SimpleUnaryLocal {
  def handleCell(z:Int) = z + c
}

case class AddInlinedConstant(r:Op[IntRaster], c:Int) extends Op[IntRaster] {
  def _run(context:Context) = runAsync(r :: Nil)
  val nextSteps:Steps = {
    case (raster:IntRaster) :: Nil => {
      val data   = raster.data
      val raster2 = raster.copy
      val data2  = raster2.data
      var i = 0
      val limit = raster.length
      while (i < limit) {
        val z = data(i)
        if (z != NODATA) {
          data2(i) = z + c
        }
        i += 1
      }
      Result(raster2)
    }
  }
}


/**
 * Subtract a constant value from each cell.
 */
case class SubtractConstant(r:Op[IntRaster], c:Op[Int]) extends WithIntConstant {
  def handleCell(z:Int, c:Int) = z - c
}
case class SubtractConstantBy(c:Op[Int], r:Op[IntRaster]) extends WithIntConstant {
  def handleCell(z:Int, c:Int) = c - z
}


/**
 * Multiply each cell by a constant.
 */
case class MultiplyConstant(r:Op[IntRaster], c:Op[Int]) extends WithIntConstant {
  def handleCell(z:Int, c:Int) = z * c
}
case class MultiplyDoubleConstant(r:Op[IntRaster], c:Op[Double]) extends WithDoubleConstant {
  def handleCell(z:Int, c:Double) = (z * c).toInt
}


/**
 * Divide each cell by a constant value.
 */
case class DivideConstant(r:Op[IntRaster], c:Op[Int]) extends WithIntConstant {
  def handleCell(z:Int, c:Int) = z / c
}
case class DivideDoubleConstant(r:Op[IntRaster], c:Op[Double]) extends WithDoubleConstant {
  def handleCell(z:Int, c:Double) = (z / c).toInt
}


/**
 * For each cell, divide a constant value by that cell's value.
 */
case class DivideConstantBy(c:Op[Int], r:Op[IntRaster]) extends WithIntConstant {
  def handleCell(z:Int, c:Int) = c / z
}
case class DivideDoubleConstantBy(c:Op[Double], r:Op[IntRaster]) extends WithDoubleConstant {
  def handleCell(z:Int, c:Double) = (c / z).toInt
}


/**
 * Bitmask each cell by a constant value.
 */
case class Bitmask(r:Op[IntRaster], c:Op[Int]) extends WithIntConstant {
  def handleCell(z:Int, c:Int) = z & c
}

/**
  * Set each cell to a constant number or the corresponding cell value, whichever is highest.
  */
case class MaxConstant(r:Op[IntRaster], c:Op[Int]) extends WithIntConstant {
  def handleCell(z:Int, c:Int) = max(z, c)
}

/**
  * Set each cell to a constant or its existing value, whichever is lowest.
  */
case class MinConstant(r:Op[IntRaster], c:Op[Int]) extends WithIntConstant {
  def handleCell(z:Int, c:Int) = min(z, c)
}

/**
 * Raise each cell to the cth power.
 */
case class PowConstant(r:Op[IntRaster], c:Op[Int]) extends WithIntConstant {
  def handleCell(z:Int, c:Int) = pow(z, c).toInt
}
case class PowDoubleConstant(r:Op[IntRaster], c:Op[Double]) extends WithDoubleConstant {
  def handleCell(z:Int, c:Double) = pow(z, c).toInt
}
