package geotrellis

import scala.{specialized => sp}

import cats.implicits._
import simulacrum._
import spire.algebra._
import spire.syntax.field._

// --- //

/** A strategy for handling locations outside the usual legal range of [[Focal.get]]. */
sealed trait Boundary[@sp(Int, Double) A]

/* Reference:
 http://hackage.haskell.org/package/repa-3.4.1.3/docs/Data-Array-Repa-Stencil.html#t:Boundary
 */
object Boundary {
  /** Locations outside the legal range of [[Focal.get]] will be completely ignored.
   * Any neighbourhood that involves them will simply have less values.
   */
  case object Ignore extends Boundary[Nothing]

  /** Locations outside the legal range of [[Focal.get]] will all return the
   * same [[value]].
   */
  case class Constant[@sp(Int, Double) A](value: A) extends Boundary[A]

  /** Locations outside the legal range of [[Focal.get]] will be given the
   * value of the nearest legal location.
   */
  // case object Edge extends Boundary[Nothing]
}

/** A description of deltas from the "current location" as occurs during
  * [[Focal]] operations. [[boundary]] specifies how to handle locations
  * outside the legal boundaries of [[Focal.get]].
  */
case class Stencil[@sp(Int, Double) A](deltas: List[(Int, Int)], boundary: Boundary[A]) {
  /* Assumes symmetric stencil shapes. */
  val topLeft: (Int, Int) = deltas.foldLeft((0,0)) {
    case ((minx, miny), (x, y)) => (minx.min(x), miny.min(y))
  }
  val bottomRight: (Int, Int) = deltas.foldLeft((0,0)) {
    case ((maxx, maxy), (x, y)) => (maxx.max(x), maxy.max(y))
  }
}

object Stencil {
  def square[@sp(Int, Double) A](boundary: Boundary[A]) =
    Stencil(List((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1,1)), boundary)
}

/**
  * Types which can have ''Focal'' operations performed on them.
  *
  * '''LAW''': Existance
  * {{{
  *  `get(0,0)` must return a value.
  * }}}
  *
  * '''LAW''': Solidity
  * {{{
  *   for (y > 0), if get(x,y) then get(x, y-1)
  *   for (x > 0), if get(x,y) then get(x-1, y)
  * }}}
  *
  * @groupname minimal Minimal Complete Definition
  * @groupprio minimal 0
  *
  * @groupname focal Focal Operations
  * @groupprio focal 1
  * @groupdesc focal Operations over "neighbourhoods" of one `A`.
  */
@typeclass trait Focal[F[_]] {

  def isBorder[@sp(Int, Double) A](self: F[A], s: Stencil[A], xy: (Int, Int)): Boolean

  def isLegal[@sp(Int, Double) A](self: F[A], xy: (Int, Int)): Boolean

  def get[@sp(Int, Double) A](self: F[A], xy: (Int, Int)): A

  def imap[@sp(Int, Double) A](self: F[A], f: ((Int, Int), A) => A): F[A]

  def focal[@sp(Int, Double) A](self: F[A], s: Stencil[A], f: List[A] => A): F[A] = {

    // TODO Yield the correct out-of-bounds values! (use `Boundary`)
    def work(ix: (Int, Int), a: A): A = ix match {
      // Special checks must be done on each pixel in the neighbourhood
      case _ if isBorder(self, s, ix) =>
        val ps: List[A] = s.deltas.foldLeft(Nil: List[A]) { (acc, d) =>
          val p: (Int, Int) = d |+| ix
          if (isLegal(self, p)) get(self, p) :: acc else acc
        }
        f(a :: ps)

      // By definition, each pixel in the neighbourhood is a legal location
      case _ => f(a :: s.deltas.map(d => get(self, d |+| ix)))
    }

    imap(self, work)
  }

  @inline def fsum[@sp(Int, Double) A: Ring](self: F[A], s: Stencil[A]): F[A] =
    focal(self, s, { _.foldLeft(Ring[A].zero)(_ + _) })

  @inline def fmean[@sp(Int, Double) A: Field](self: F[A], s: Stencil[A]): F[A] =
    focal(self, s, { n => n.foldLeft(Field[A].zero)(_ + _) / n.length })

}

private[geotrellis] trait FocalInstances {

  /** Assumes a square Tile, which is not representative of reality. */
  implicit val arrayFocal: Focal[Array] = new Focal[Array] {

    /** Convert 2D row-major coordinates into a 1D index. */
    private[this] def oneD(size: Int, x: Int, y: Int): Int = {
      val dim: Int = Math.sqrt(size).toInt

      x + (y * dim)
    }

    def isBorder[@sp(Int, Double) A](self: Array[A], s: Stencil[A], xy: (Int, Int)): Boolean =
      !isLegal(self, xy |+| s.topLeft) || !isLegal(self, xy |+| s.bottomRight)

    def isLegal[@sp(Int, Double) A](self: Array[A], xy: (Int, Int)): Boolean = {
      // TODO Fix. Assumes a square tile.
      // This would be solved if this function also took the argument: F[A] => (Int, Int)
      // where the two Ints are the xmax and ymax respectively.
      val dim: Int = Math.sqrt(self.size).toInt

      xy._1 >= 0 && xy._1 < dim && xy._2 >= 0 && xy._2 < dim
    }

    def get[@sp(Int, Double) A](self: Array[A], xy: (Int, Int)): A =
      self(oneD(self.size, xy._1, xy._2))

    def imap[@sp(Int, Double) A](self: Array[A], f: ((Int, Int), A) => A): Array[A] = {

      // TODO Fix. This is evil, since it assumes a square tile.
      // Giving accurate dimensions either requires a wrapper around `Array`
      // (potentially killing the specialization) or passing it in as another
      // argument to `imap`.
      val res: Array[A] = self.clone
      val dim: Int = Math.sqrt(self.size).toInt
      var x: Int = 0
      var y: Int = 0

      while(y < dim) {
        while(x < dim) {
          val i: Int = oneD(self.size, x, y)
          res(i) = f((x, y), self(i))

          x += 1
        }

        x = 0
        y += 1
      }

      res
    }
  }
}
