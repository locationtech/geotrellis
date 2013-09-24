package geotrellis.source

import geotrellis._

trait SeqDataSourceLike[A, +Repr <: DataSource[A,Seq[A]]] 
    extends DataSourceLike[A,Seq[A],Repr] 
    with DataSource[A,Seq[A]] { self: Repr =>
  type CBF[B] = ({type BF = CBSF[Repr, A, B]})#BF

  def seqOp:Op[Seq[Op[A]]]
  def get()(implicit mf:Manifest[Seq[A]]) = {
    logic.Collect(seqOp)
  }
}
