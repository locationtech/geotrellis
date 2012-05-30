package geotrellis

import geotrellis._
import geotrellis.geometry.Polygon

package object operation {
  // Operation is such a long word :(
  type Op[A] = Operation[A]
  type LocalOp = LocalOperation
  type DispatchedOp[T] = DispatchedOperation[T]

  // TODO: consider adding things like type PNG = Array[Byte]?

  // TODO: Doubles, **, unary_-, min, max, &, |, ^, %

  // TODO maybe: some kind of mask operator?
}


object Implicits {
  import geotrellis.operation._

  /**
   * Addition-operator implicits for Int, IntRaster and Op[IntRaster].
   */
  implicit def addOpIntRasterTo1(lhs:Op[IntRaster]) = new {
    def +(rhs:Int):Op[IntRaster] = new AddConstant(lhs, Literal(rhs))
    def +(rhs:Op[Int]):Op[IntRaster] = new AddConstant(lhs, rhs)
  }
  implicit def addOpIntRasterTo2(lhs:Op[IntRaster]) = new {
    def +(rhs:IntRaster):Op[IntRaster] = Add(lhs, Literal(rhs))
    def +(rhs:Op[IntRaster]):Op[IntRaster] = Add(lhs, rhs)
  }

  implicit def addIntRasterTo1(lhs:IntRaster) = new {
    def +(rhs:Int):Op[IntRaster] = new AddConstant(Literal(lhs), Literal(rhs))
    def +(rhs:Op[Int]):Op[IntRaster] = new AddConstant(Literal(lhs), rhs)
  }
  implicit def addIntRasterTo2(lhs:IntRaster) = new {
    def +(rhs:IntRaster):Op[IntRaster] = Add(Literal(lhs), Literal(rhs))
    def +(rhs:Op[IntRaster]):Op[IntRaster] = Add(Literal(lhs), rhs)
  }

  implicit def addOpIntTo1(lhs:Op[Int]) = new {
    def +(rhs:Int):Op[Int] = Map1(lhs)(_ + rhs)
    def +(rhs:Op[Int]):Op[Int] = Map2(lhs, rhs)(_ + _)
  }
  implicit def addOpIntTo2(lhs:Op[Int]) = new {
    def +(rhs:IntRaster):Op[IntRaster] = AddConstant(Literal(rhs), lhs)
    def +(rhs:Op[IntRaster]):Op[IntRaster] = AddConstant(rhs, lhs)
  }

  implicit def addIntTo1(lhs:Int) = new {
    def +(rhs:Op[Int]):Op[Int] = Map1(rhs)(_ + lhs)
  }
  implicit def addIntTo2(lhs:Int) = new {
    def +(rhs:IntRaster):Op[IntRaster] = AddConstant(Literal(rhs), Literal(lhs))
    def +(rhs:Op[IntRaster]):Op[IntRaster] = AddConstant(rhs, Literal(lhs))
  }

  /**
   * Multiplication-operator implicits for Int, IntRaster and Op[IntRaster].
   */
  implicit def multiplyOpIntRasterBy1(lhs:Op[IntRaster]) = new {
    def *(rhs:Int):Op[IntRaster] = MultiplyConstant(lhs, Literal(rhs))
    def *(rhs:Op[Int]):Op[IntRaster] = MultiplyConstant(lhs, rhs)
  }
  implicit def multiplyOpIntRasterBy2(lhs:Op[IntRaster]) = new {
    def *(rhs:IntRaster):Op[IntRaster] = Multiply(lhs, Literal(rhs))
    def *(rhs:Op[IntRaster]):Op[IntRaster] = Multiply(lhs, rhs)
  }

  implicit def multiplyIntRasterBy1(lhs:IntRaster) = new {
    def *(rhs:Int):Op[IntRaster] = MultiplyConstant(Literal(lhs), Literal(rhs))
    def *(rhs:Op[Int]):Op[IntRaster] = MultiplyConstant(Literal(lhs), rhs)
  }
  implicit def multiplyIntRasterBy2(lhs:IntRaster) = new {
    def *(rhs:IntRaster):Op[IntRaster] = Multiply(Literal(lhs), Literal(rhs))
    def *(rhs:Op[IntRaster]):Op[IntRaster] = Multiply(Literal(lhs), rhs)
  }

  implicit def multiplyOpIntBy1(lhs:Op[Int]) = new {
    def *(rhs:Int):Op[Int] = Map1(lhs)(_ * rhs)
    def *(rhs:Op[Int]):Op[Int] = Map2(lhs, rhs)(_ * _)
  }
  implicit def multiplyOpIntBy2(lhs:Op[Int]) = new {
    def *(rhs:IntRaster):Op[IntRaster] = MultiplyConstant(Literal(rhs), lhs)
    def *(rhs:Op[IntRaster]):Op[IntRaster] = MultiplyConstant(rhs, lhs)
  }

  implicit def multiplyIntBy1(lhs:Int) = new {
    def *(rhs:Op[Int]):Op[Int] = Map1(rhs)(_ * lhs)
  }
  implicit def multiplyIntBy2(lhs:Int) = new {
    def *(rhs:IntRaster):Op[IntRaster] = MultiplyConstant(Literal(rhs), Literal(lhs))
    def *(rhs:Op[IntRaster]):Op[IntRaster] = MultiplyConstant(rhs, Literal(lhs))
  }

  /**
   * Subtraction-operator implicits for Int, IntRaster and Op[IntRaster].
   */
  implicit def subtractOpIntRasterBy1(lhs:Op[IntRaster]) = new {
    def -(rhs:Int):Op[IntRaster] = SubtractConstant(lhs, Literal(rhs))
    def -(rhs:Op[Int]):Op[IntRaster] = SubtractConstant(lhs, rhs)
  }
  implicit def subtractOpIntRasterBy2(lhs:Op[IntRaster]) = new {
    def -(rhs:IntRaster):Op[IntRaster] = Subtract(lhs, Literal(rhs))
    def -(rhs:Op[IntRaster]):Op[IntRaster] = Subtract(lhs, rhs)
  }

  implicit def subtractIntRasterBy1(lhs:IntRaster) = new {
    def -(rhs:Int):Op[IntRaster] = SubtractConstant(Literal(lhs), Literal(rhs))
    def -(rhs:Op[Int]):Op[IntRaster] = SubtractConstant(Literal(lhs), rhs)
  }
  implicit def subtractIntRasterBy2(lhs:IntRaster) = new {
    def -(rhs:IntRaster):Op[IntRaster] = Subtract(Literal(lhs), Literal(rhs))
    def -(rhs:Op[IntRaster]):Op[IntRaster] = Subtract(Literal(lhs), rhs)
  }

  implicit def subtractOpIntBy1(lhs:Int) = new {
    def -(rhs:Int):Op[Int] = Map1(lhs)(_ - rhs)
    def -(rhs:Op[Int]):Op[Int] = Map2(lhs, rhs)(_ - _)
  }
  implicit def subtractOpIntBy2(lhs:Int) = new {
    def -(rhs:IntRaster):Op[IntRaster] = SubtractConstantBy(lhs, Literal(rhs))
    def -(rhs:Op[IntRaster]):Op[IntRaster] = SubtractConstantBy(lhs, rhs)
  }

  implicit def subtractIntBy1(lhs:Int) = new {
    def -(rhs:Op[Int]):Op[Int] = Map1(rhs)(lhs - _)
  }
  implicit def subtractIntBy2(lhs:Int) = new {
    def -(rhs:IntRaster):Op[IntRaster] = SubtractConstantBy(Literal(lhs), Literal(rhs))
    def -(rhs:Op[IntRaster]):Op[IntRaster] = SubtractConstantBy(Literal(lhs), rhs)
  }

  /**
   * Division-operator implicits for Int, IntRaster and Op[IntRaster].
   */
  implicit def divideOpIntRasterBy1(lhs:Op[IntRaster]) = new {
    def /(rhs:Int):Op[IntRaster] = DivideConstant(lhs, Literal(rhs))
    def /(rhs:Op[Int]):Op[IntRaster] = DivideConstant(lhs, rhs)
  }
  implicit def divideOpIntRasterBy2(lhs:Op[IntRaster]) = new {
    def /(rhs:IntRaster):Op[IntRaster] = Divide(lhs, Literal(rhs))
    def /(rhs:Op[IntRaster]):Op[IntRaster] = Divide(lhs, rhs)
  }

  implicit def divideIntRasterBy1(lhs:IntRaster) = new {
    def /(rhs:Int):Op[IntRaster] = DivideConstant(Literal(lhs), Literal(rhs))
    def /(rhs:Op[Int]):Op[IntRaster] = DivideConstant(Literal(lhs), rhs)
  }
  implicit def divideIntRasterBy2(lhs:IntRaster) = new {
    def /(rhs:IntRaster):Op[IntRaster] = Divide(Literal(lhs), Literal(rhs))
    def /(rhs:Op[IntRaster]):Op[IntRaster] = Divide(Literal(lhs), rhs)
  }

  implicit def divideOpIntBy1(lhs:Op[Int]) = new {
    def /(rhs:Int):Op[Int] = Map1(lhs)(_ / rhs)
    def /(rhs:Op[Int]):Op[Int] = Map2(lhs, rhs)(_ / _)
  }

  implicit def divideOpIntBy2(lhs:Op[Int]) = new {
    def /(rhs:IntRaster):Op[IntRaster] = DivideConstantBy(lhs, Literal(rhs))
    def /(rhs:Op[IntRaster]):Op[IntRaster] = DivideConstantBy(lhs, rhs)
  }

  implicit def divideIntBy1(lhs:Int) = new {
    def /(rhs:Op[Int]):Op[Int] = Map1(rhs)(lhs / _)
  }
  implicit def divideIntBy2(lhs:Int) = new {
    def /(rhs:IntRaster):Op[IntRaster] = DivideConstantBy(Literal(lhs), Literal(rhs))
    def /(rhs:Op[IntRaster]):Op[IntRaster] = DivideConstantBy(Literal(lhs), rhs)
  }
}

