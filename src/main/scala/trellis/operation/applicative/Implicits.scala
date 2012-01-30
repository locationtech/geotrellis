package trellis.operation.applicative

import trellis.operation._
import trellis.process._

/**
 * Some implicit operators to add some syntactic sugar. To use say:
 *
 * import trellis.operation.applicative.Implicits._
 */
object Implicits {
  implicit def applyOperator[A, Z:Manifest](lhs:Op[A => Z]) = new {
    def <*>(rhs:Op[A]) = Apply(rhs)(lhs)
  }

  // This coorrespond's to Haskell's <$> which isn't legal in Scala.
  implicit def fmapOperator[A, Z:Manifest](lhs:Function1[A, Z]) = new {
    def <@>(rhs:Op[A]) = Fmap(rhs)(lhs)
  }
}
