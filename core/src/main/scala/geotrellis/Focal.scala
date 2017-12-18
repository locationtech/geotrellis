package geotrellis

import simulacrum._

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
@typeclass trait Focal[A] {

  /** @group minimal */
  def get(self: A, x: Int, y: Int): Int

  /** @group minimal */
  def imap(self: A, f: (Int, Int, Int) => Int): A

  /** The `.head` value of each `List` is the neighbourhood focus.
    *
    * @group focal
    */
  def focal(self: A, n: Stencil, f: List[Int] => Int): A = ???  // uses `imap` and `get`

  /** @group focal */
  def sum(self: A, s: Stencil): A = focal(self, s, { _.foldLeft(0)(_ + _) })

  /** @group focal */
  def mean(self: A, s: Stencil): A = focal(self, s, { n => n.foldLeft(0)(_ + _) / n.length })

  /* All other focal ops would be provided for free here */

}
