package geotrellis

import scala.{specialized => sp}

import cats.Functor
import simulacrum._
import spire.algebra._
import spire.std.any._
import spire.syntax.field._
import spire.syntax.order._

// --- //

/**
  * Types which can have ''Local'' map algebra operations performed on them.
  *
  * '''LAW''': Identity
  * {{{
  * a.lmap(identity) == identity(a)
  * }}}
  *
  * '''LAW''': Composibility
  * {{{
  * a.lmap(f compose g) == a.lmap(g).lmap(f)
  * }}}
  *
  * '''LAW''': Right-laziness
  * {{{
  * Array.empty.zipWith(throw new Exception) == Array.empty
  * }}}
  *
  * @groupname minimal Minimal Complete Definition
  * @groupprio minimal 0
  *
  * @groupname local Local Operations
  * @groupprio local 1
  * @groupdesc local Per-"location" operations between one or more `A`.
  */
@typeclass trait Local[F[_]] {

  def lmap[@sp(Int, Double) A](self: F[A], f: A => A): F[A]

  def zipWith[@sp(Int, Double) A](self: F[A], other: => F[A], f: (A, A) => A): F[A]

  /** @group local */
  // def classify(self: A, f: Int => Int): A = map(self, f)

  @inline def +[@sp(Int, Double) A: Ring](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ + _ })

  @inline def -[@sp(Int, Double) A: Ring](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ - _ })

  @inline def *[@sp(Int, Double) A: Ring](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ * _ })

  @inline def /[@sp(Int, Double) A: Field](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ / _ })

  @inline def min[@sp(Int, Double) A: Order](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ min _ })

  @inline def max[@sp(Int, Double) A: Order](self: F[A], other: => F[A]): F[A] =
    zipWith(self, other, { _ max _ })

}

private[geotrellis] trait LocalInstances {

  implicit val arrayLocal: Local[Array] = new Local[Array] {

    def lmap[@sp(Int, Double) A](self: Array[A], f: A => A): Array[A] = {
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
        val len: Int = if (self.size < other.size) self.size else other.size
        val res: Array[A] = if (self.size < other.size) self.clone else other.clone
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
