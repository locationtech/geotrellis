package trellis

import trellis._
import trellis.geometry.Polygon

package object operation {
  // Operation is such a long word :(
  type Op[A] = Operation[A]
  //type SimpleOp[A] = SimpleOperation[A]
  type LocalOp = LocalOperation

  // TODO: consider adding things like type PNG = Array[Byte]?

  import trellis.operation.Operation.implicitLiteral

  /**
   * Addition-operator implicits for Int, IntRaster and Op[IntRaster].
   */
  implicit def addOpTo(lhs:Op[IntRaster]) = new {
    def +(rhs:Int):Op[IntRaster] = AddConstant(lhs, Literal(rhs))
    def +(rhs:IntRaster):Op[IntRaster] = Add(lhs, Literal(rhs))
    def +(rhs:Op[IntRaster]):Op[IntRaster] = Add(lhs, rhs)
  }
  implicit def addIntRasterTo(lhs:IntRaster) = new {
    def +(rhs:Int):Op[IntRaster] = AddConstant(Literal(lhs), Literal(rhs))
    def +(rhs:IntRaster):Op[IntRaster] = Add(Literal(lhs), Literal(rhs))
    def +(rhs:Op[IntRaster]):Op[IntRaster] = Add(Literal(lhs), rhs)
  }
  implicit def addIntRasterTo(lhs:Int) = new {
    def +(rhs:IntRaster):Op[IntRaster] = AddConstant(Literal(rhs), Literal(lhs))
    def +(rhs:Op[IntRaster]):Op[IntRaster] = AddConstant(rhs, Literal(lhs))
  }

  /**
   * Multiplication-operator implicits for Int, IntRaster and Op[IntRaster].
   */
  implicit def multiplyOpBy(lhs:Op[IntRaster]) = new {
    def *(rhs:Int):Op[IntRaster] = MultiplyConstant(lhs, Literal(rhs))
    def *(rhs:IntRaster):Op[IntRaster] = Multiply(lhs, Literal(rhs))
    def *(rhs:Op[IntRaster]):Op[IntRaster] = Multiply(lhs, rhs)
  }
  implicit def multiplyIntRasterBy(lhs:IntRaster) = new {
    def *(rhs:Int):Op[IntRaster] = MultiplyConstant(Literal(lhs), Literal(rhs))
    def *(rhs:IntRaster):Op[IntRaster] = Multiply(Literal(lhs), Literal(rhs))
    def *(rhs:Op[IntRaster]):Op[IntRaster] = Multiply(Literal(lhs), rhs)
  }
  implicit def multiplyIntRasterBy(lhs:Int) = new {
    def *(rhs:IntRaster):Op[IntRaster] = MultiplyConstant(Literal(rhs), Literal(lhs))
    def *(rhs:Op[IntRaster]):Op[IntRaster] = MultiplyConstant(rhs, Literal(lhs))
  }

  /**
   * Subtraction-operator implicits for Int, IntRaster and Op[IntRaster].
   */
  implicit def subtractOpBy(lhs:Op[IntRaster]) = new {
    def -(rhs:Int):Op[IntRaster] = SubtractConstant(lhs, Literal(rhs))
    def -(rhs:IntRaster):Op[IntRaster] = Subtract(lhs, Literal(rhs))
    def -(rhs:Op[IntRaster]):Op[IntRaster] = Subtract(lhs, rhs)
  }
  implicit def subtractIntRasterBy(lhs:IntRaster) = new {
    def -(rhs:Int):Op[IntRaster] = SubtractConstant(Literal(lhs), Literal(rhs))
    def -(rhs:IntRaster):Op[IntRaster] = Subtract(Literal(lhs), Literal(rhs))
    def -(rhs:Op[IntRaster]):Op[IntRaster] = Subtract(Literal(lhs), rhs)
  }
  implicit def subtractIntRasterBy(lhs:Int) = new {
    def -(rhs:IntRaster):Op[IntRaster] = SubtractConstantBy(Literal(lhs), Literal(rhs))
    def -(rhs:Op[IntRaster]):Op[IntRaster] = SubtractConstantBy(Literal(lhs), rhs)
  }

  /**
   * Division-operator implicits for Int, IntRaster and Op[IntRaster].
   */
  implicit def divideOpBy(lhs:Op[IntRaster]) = new {
    def /(rhs:Int):Op[IntRaster] = DivideConstant(lhs, Literal(rhs))
    def /(rhs:IntRaster):Op[IntRaster] = Divide(lhs, Literal(rhs))
    def /(rhs:Op[IntRaster]):Op[IntRaster] = Divide(lhs, rhs)
  }
  implicit def divideIntRasterBy(lhs:IntRaster) = new {
    def /(rhs:Int):Op[IntRaster] = DivideConstant(Literal(lhs), Literal(rhs))
    def /(rhs:IntRaster):Op[IntRaster] = Divide(Literal(lhs), Literal(rhs))
    def /(rhs:Op[IntRaster]):Op[IntRaster] = Divide(Literal(lhs), rhs)
  }
  implicit def divideIntRasterBy(lhs:Int) = new {
    def /(rhs:IntRaster):Op[IntRaster] = DivideConstantBy(Literal(lhs), Literal(rhs))
    def /(rhs:Op[IntRaster]):Op[IntRaster] = DivideConstantBy(Literal(lhs), rhs)
  }

  // TODO: Doubles, **, unary_-, min, max, &, |, ^, %

  // TODO maybe: some kind of mask operator?
}
