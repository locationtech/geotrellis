package geotrellis

import geotrellis._

trait SeqSourceLike[A, +Repr <: DataSource[Seq[A]]] extends DataSourceLike[Seq[A],Repr] with DataSource[Seq[A]] { self: Repr =>
  type CBF[B] = ({type BF = CBSF[Repr, A, B]})#BF

  def seqOp:Op[Seq[Op[A]]]
  def get = {
    logic.Collect(seqOp)
  }
}
