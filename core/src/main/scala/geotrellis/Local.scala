package geotrellis

import simulacrum._

/**
  * Types which can have ''Local'' map algebra operations performed on them.
  *
  * '''LAW''': Identity
  * {{{
  * a.map(identity) == identity(a)
  * }}}
  *
  * '''LAW''': Composibility
  * {{{
  * a.map(f compose g) == a.map(g).map(f)
  * }}}
  *
  * '''LAW''': Right-laziness
  * {{{
  * val a: Tile = ArrayTile.empty(IntCellType, 0, 0)
  * a.zipWith(throw new Exception) == a
  * }}}
  *
  * @groupname minimal Minimal Complete Definition
  * @groupprio minimal 0
  *
  * @groupname local Local Operations
  * @groupprio local 1
  * @groupdesc local Per-"location" operations between one or more `A`.
  */
@typeclass trait Local[A] {

  /** @group minimal */
  def map(self: A, f: Int => Int): A

  /** @group minimal */
  def zipWith(self: A, other: => A, f: (Int, Int) => Int): A  // Could also be called `parmap`.

  /** @group local */
  // def classify(self: A, f: Int => Int): A = map(self, f)

  /** @group local */
  def +(self: A, other: => A): A = zipWith(self, other, (_ + _))

  /** @group local */
  def -(self: A, other: => A): A = zipWith(self, other, (_ - _))

  /** @group local */
  def *(self: A, other: => A): A = zipWith(self, other, (_ * _))

  /** @group local */
  def /(self: A, other: => A): A = zipWith(self, other, (_ / _))

  /** @group local */
  def min(self: A, other: => A): A = zipWith(self, other, (_ min _))

  /** @group local */
  def max(self: A, other: => A): A = zipWith(self, other, (_ max _))

}
