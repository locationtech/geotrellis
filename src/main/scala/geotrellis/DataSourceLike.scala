package geotrellis


case class TransformSequenceOfOperations[P,B](opSeq:Op[Seq[Op[P]]])
                                             (f:Op[P]=> Op[B]) extends Op1(opSeq) ({
  (opSeq) => {
    val newSeq:Seq[Op[B]] = opSeq.map(
      (op:Op[P]) => {
        val op2:Op[B] = f(op)
          op2
      }
    )
      Result(newSeq)
  }})


import SingleDataSource._

trait Unpartitioned

import scala.language.higherKinds
trait DataSourceLike[T,P,+Repr <: DataSource[T,P]] { self:Repr =>
  def partitions():Op[Seq[Op[P]]]
  def get:Op[T]

  def converge(implicit mf:Manifest[T]):SingleDataSource[T,T]  = {
    new SingleDataSource(Literal(Seq(get))) 
  }

  def map[B:Manifest,That](f:Op[P] => Op[B])(implicit bf:CanBuildSourceFrom[Repr,B,That]):That =  {
    val builder = bf.apply(this)
    val partitions:Op[Seq[Op[P]]] = this.partitions

    // Apply the provided op to the operations inside the
    // future sequence of operations.  For example,
    // if we have an Op that returns Seq(LoadRaster(foo)) and our
    // function is AddConstant(_, 3) we should end up with 
    // an op that returns Seq(AddConstant(LoadRaster(foo),3))
    // 
    val newOp = TransformSequenceOfOperations(partitions)(f)

    builder.setOp(newOp)
    val result = builder.result()
    result
  }
}
