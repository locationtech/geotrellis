package geotrellis

import geotrellis._
import geotrellis.geometry.Polygon
import geotrellis.raster.op.local._
import geotrellis.logic.{Do1,Do2}


object Implicits {
  import geotrellis._

  /**
   * Addition-operator implicits for Int, Raster and Op[Raster].
   */
  implicit def addOpRasterTo1(lhs:Op[Raster]) = new {
    def +(rhs:Int):Op[Raster] = new AddConstant(lhs, Literal(rhs))
    def +(rhs:Op[Int]):Op[Raster] = new AddConstant(lhs, rhs)
  }
  implicit def addOpRasterTo2(lhs:Op[Raster]) = new {
    def +(rhs:Raster):Op[Raster] = Add(lhs, Literal(rhs))
    def +(rhs:Op[Raster]):Op[Raster] = Add(lhs, rhs)
  }

  implicit def addRasterTo1(lhs:Raster) = new {
    def +(rhs:Int):Op[Raster] = new AddConstant(Literal(lhs), Literal(rhs))
    def +(rhs:Op[Int]):Op[Raster] = new AddConstant(Literal(lhs), rhs)
  }
  implicit def addRasterTo2(lhs:Raster) = new {
    def +(rhs:Raster):Op[Raster] = Add(Literal(lhs), Literal(rhs))
    def +(rhs:Op[Raster]):Op[Raster] = Add(Literal(lhs), rhs)
  }

  implicit def addOpIntTo1(lhs:Op[Int]) = new {
    def +(rhs:Int):Op[Int] = Do1(lhs)(_ + rhs)
    def +(rhs:Op[Int]):Op[Int] = Do2(lhs, rhs)(_ + _)
  }
  implicit def addOpIntTo2(lhs:Op[Int]) = new {
    def +(rhs:Raster):Op[Raster] = AddConstant(Literal(rhs), lhs)
    def +(rhs:Op[Raster]):Op[Raster] = AddConstant(rhs, lhs)
  }

  implicit def addIntTo1(lhs:Int) = new {
    def +(rhs:Op[Int]):Op[Int] = Do1(rhs)(_ + lhs)
  }
  implicit def addIntTo2(lhs:Int) = new {
    def +(rhs:Raster):Op[Raster] = AddConstant(Literal(rhs), Literal(lhs))
    def +(rhs:Op[Raster]):Op[Raster] = AddConstant(rhs, Literal(lhs))
  }

  /**
   * Multiplication-operator implicits for Int, Raster and Op[Raster].
   */
  implicit def multiplyOpRasterBy1(lhs:Op[Raster]) = new {
    def *(rhs:Int):Op[Raster] = MultiplyConstant(lhs, Literal(rhs))
    def *(rhs:Op[Int]):Op[Raster] = MultiplyConstant(lhs, rhs)
  }
  implicit def multiplyOpRasterBy2(lhs:Op[Raster]) = new {
    def *(rhs:Raster):Op[Raster] = Multiply(lhs, Literal(rhs))
    def *(rhs:Op[Raster]):Op[Raster] = Multiply(lhs, rhs)
  }

  implicit def multiplyRasterBy1(lhs:Raster) = new {
    def *(rhs:Int):Op[Raster] = MultiplyConstant(Literal(lhs), Literal(rhs))
    def *(rhs:Op[Int]):Op[Raster] = MultiplyConstant(Literal(lhs), rhs)
  }
  implicit def multiplyRasterBy2(lhs:Raster) = new {
    def *(rhs:Raster):Op[Raster] = Multiply(Literal(lhs), Literal(rhs))
    def *(rhs:Op[Raster]):Op[Raster] = Multiply(Literal(lhs), rhs)
  }

  implicit def multiplyOpIntBy1(lhs:Op[Int]) = new {
    def *(rhs:Int):Op[Int] = Do1(lhs)(_ * rhs)
    def *(rhs:Op[Int]):Op[Int] = Do2(lhs, rhs)(_ * _)
  }
  implicit def multiplyOpIntBy2(lhs:Op[Int]) = new {
    def *(rhs:Raster):Op[Raster] = MultiplyConstant(Literal(rhs), lhs)
    def *(rhs:Op[Raster]):Op[Raster] = MultiplyConstant(rhs, lhs)
  }

  implicit def multiplyIntBy1(lhs:Int) = new {
    def *(rhs:Op[Int]):Op[Int] = Do1(rhs)(_ * lhs)
  }
  implicit def multiplyIntBy2(lhs:Int) = new {
    def *(rhs:Raster):Op[Raster] = MultiplyConstant(Literal(rhs), Literal(lhs))
    def *(rhs:Op[Raster]):Op[Raster] = MultiplyConstant(rhs, Literal(lhs))
  }

  /**
   * Subtraction-operator implicits for Int, Raster and Op[Raster].
   */
  implicit def subtractOpRasterBy1(lhs:Op[Raster]) = new {
    def -(rhs:Int):Op[Raster] = SubtractConstant(lhs, Literal(rhs))
    def -(rhs:Op[Int]):Op[Raster] = SubtractConstant(lhs, rhs)
  }
  implicit def subtractOpRasterBy2(lhs:Op[Raster]) = new {
    def -(rhs:Raster):Op[Raster] = Subtract(lhs, Literal(rhs))
    def -(rhs:Op[Raster]):Op[Raster] = Subtract(lhs, rhs)
  }

  implicit def subtractRasterBy1(lhs:Raster) = new {
    def -(rhs:Int):Op[Raster] = SubtractConstant(Literal(lhs), Literal(rhs))
    def -(rhs:Op[Int]):Op[Raster] = SubtractConstant(Literal(lhs), rhs)
  }
  implicit def subtractRasterBy2(lhs:Raster) = new {
    def -(rhs:Raster):Op[Raster] = Subtract(Literal(lhs), Literal(rhs))
    def -(rhs:Op[Raster]):Op[Raster] = Subtract(Literal(lhs), rhs)
  }

  implicit def subtractOpIntBy1(lhs:Int) = new {
    def -(rhs:Int):Op[Int] = Do1(lhs)(_ - rhs)
    def -(rhs:Op[Int]):Op[Int] = Do2(lhs, rhs)(_ - _)
  }
  implicit def subtractOpIntBy2(lhs:Int) = new {
    def -(rhs:Raster):Op[Raster] = SubtractConstantBy(lhs, Literal(rhs))
    def -(rhs:Op[Raster]):Op[Raster] = SubtractConstantBy(lhs, rhs)
  }

  implicit def subtractIntBy1(lhs:Int) = new {
    def -(rhs:Op[Int]):Op[Int] = Do1(rhs)(lhs - _)
  }
  implicit def subtractIntBy2(lhs:Int) = new {
    def -(rhs:Raster):Op[Raster] = SubtractConstantBy(Literal(lhs), Literal(rhs))
    def -(rhs:Op[Raster]):Op[Raster] = SubtractConstantBy(Literal(lhs), rhs)
  }

  /**
   * Division-operator implicits for Int, Raster and Op[Raster].
   */
  implicit def divideOpRasterBy1(lhs:Op[Raster]) = new {
    def /(rhs:Int):Op[Raster] = DivideConstant(lhs, Literal(rhs))
    def /(rhs:Op[Int]):Op[Raster] = DivideConstant(lhs, rhs)
  }
  implicit def divideOpRasterBy2(lhs:Op[Raster]) = new {
    def /(rhs:Raster):Op[Raster] = Divide(lhs, Literal(rhs))
    def /(rhs:Op[Raster]):Op[Raster] = Divide(lhs, rhs)
  }

  implicit def divideRasterBy1(lhs:Raster) = new {
    def /(rhs:Int):Op[Raster] = DivideConstant(Literal(lhs), Literal(rhs))
    def /(rhs:Op[Int]):Op[Raster] = DivideConstant(Literal(lhs), rhs)
  }
  implicit def divideRasterBy2(lhs:Raster) = new {
    def /(rhs:Raster):Op[Raster] = Divide(Literal(lhs), Literal(rhs))
    def /(rhs:Op[Raster]):Op[Raster] = Divide(Literal(lhs), rhs)
  }

  implicit def divideOpIntBy1(lhs:Op[Int]) = new {
    def /(rhs:Int):Op[Int] = Do1(lhs)(_ / rhs)
    def /(rhs:Op[Int]):Op[Int] = Do2(lhs, rhs)(_ / _)
  }

  implicit def divideOpIntBy2(lhs:Op[Int]) = new {
    def /(rhs:Raster):Op[Raster] = DivideConstantBy(lhs, Literal(rhs))
    def /(rhs:Op[Raster]):Op[Raster] = DivideConstantBy(lhs, rhs)
  }

  implicit def divideIntBy1(lhs:Int) = new {
    def /(rhs:Op[Int]):Op[Int] = Do1(rhs)(lhs / _)
  }
  implicit def divideIntBy2(lhs:Int) = new {
    def /(rhs:Raster):Op[Raster] = DivideConstantBy(Literal(lhs), Literal(rhs))
    def /(rhs:Op[Raster]):Op[Raster] = DivideConstantBy(Literal(lhs), rhs)
  }

  type Do1[A,B] = logic.Do1[A,B]
  type Do2[A,B,C] = logic.Do2[A,B,C]
}

