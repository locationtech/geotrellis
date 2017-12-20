package geotrellis

import scala.{specialized => sp}

import simulacrum._
import spire.algebra._
import spire.std.any._
import spire.syntax.field._

// --- //

class Stencil       // stub

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

  def get[@sp(Int, Double) A](self: F[A], x: Int, y: Int): A

  def imap[@sp(Int, Double) A](self: F[A], f: ((Int, Int), A) => A): F[A]

  def focal[@sp(Int, Double) A](self: F[A], n: Stencil, f: List[A] => A): F[A] = ???  // uses `imap` and `get`

  @inline def sum[@sp(Int, Double) A: Ring](self: F[A], s: Stencil): F[A] =
    focal(self, s, { _.foldLeft(Ring[A].zero)(_ + _) })

  @inline def mean[@sp(Int, Double) A: Field](self: F[A], s: Stencil): F[A] =
    focal(self, s, { n => n.foldLeft(Field[A].zero)(_ + _) / n.length })

}
