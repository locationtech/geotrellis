package geotrellis

import scala.reflect.ClassTag
import scala.{specialized => sp}

import simulacrum._
import spire.algebra._
import spire.std.any._
import spire.syntax.ring._

// --- //

@typeclass trait Local1[F[_]] {

  /** @group minimal */
  def map[@sp(Int, Double) A](self: F[A], f: A => A): F[A]

  /** @group minimal */
  def zipWith[@sp(Int, Double) A](self: F[A], other: => F[A], f: (A, A) => A): F[A]

  /** @group local */
  // def classify(self: A, f: Int => Int): A = map(self, f)

  /** @group local */
  def +[@sp(Int, Double) A: Ring](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ + _ })

  /** @group local */
  // def -(self: A, other: => A): A = zipWith(self, other, (_ - _))

  /** @group local */
  // def *(self: A, other: => A): A = zipWith(self, other, (_ * _))

  /** @group local */
  // def /(self: A, other: => A): A = zipWith(self, other, (_ / _))

  /** @group local */
  // def min(self: A, other: => A): A = zipWith(self, other, (_ min _))

  /** @group local */
  // def max(self: A, other: => A): A = zipWith(self, other, (_ max _))

}

private[geotrellis] trait LocalInstances {

  implicit val arrayLocal: Local1[Array] = new Local1[Array] {

    def map[@sp(Int, Double) A](self: Array[A], f: A => A): Array[A] = {
      val len: Int = self.size
      val res: Array[A] = self.clone
      var i: Int = 0

      while (i < len) {
        res(i) = f(self(i))
        i += 1
      }

      res
    }

    def zipWith[@sp(Int, Double) A](self: Array[A], other: => Array[A], f: (A, A) => A): Array[A] = {

      if (self.isEmpty) self else {
        val len: Int = self.size.min(other.size)
        val res: Array[A] = self.clone
        var i: Int = 0

        while(i < len) {
          res(i) = f(self(i), other(i))

          i += 1
        }

        res
      }
    }

  }
}
