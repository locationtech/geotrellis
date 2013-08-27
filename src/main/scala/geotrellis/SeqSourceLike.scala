package geotrellis

import geotrellis._

trait SeqSourceLike[A, +Repr <: DataSource[Seq[A],A]] 
    extends DataSourceLike[Seq[A],A,Repr] 
    with DataSource[Seq[A],A] { self: Repr =>
  type CBF[B] = ({type BF = CBSF[Repr, A, B]})#BF

  def seqOp:Op[Seq[Op[A]]]
  def get = {
    logic.Collect(seqOp)
  }
}
