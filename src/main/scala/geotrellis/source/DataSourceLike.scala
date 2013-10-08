package geotrellis.source

import geotrellis._

import scala.language.higherKinds

trait DataSourceLike[+T,V,+Repr <: DataSource[T,V]] { self:Repr =>
  def elements():Op[Seq[Op[T]]]
  def get()(implicit mf:Manifest[V]):Op[V]
  def converge()(implicit mf:Manifest[V]) = ValueDataSource(get)

  def map[B:Manifest,That](f:T => B)(implicit bf:CanBuildSourceFrom[Repr,B,That]):That = {
    val builder = bf.apply(this)
    val fOp = op(f(_)) 
    val newOp = TransformSequenceOfOperations(elements)(fOp)
    builder.setOp(newOp)
    val result = builder.result()
    result
  }

  /** apply a function to elements, and return the appropriate datasource **/
  def mapOp[B:Manifest,That](f:Op[T] => Op[B])(implicit bf:CanBuildSourceFrom[Repr,B,That]):That =  {
    val builder = bf.apply(this)
    
    // Apply the provided op to the operations inside the
    // future sequence of operations.  For example,
    // if we have an Op that returns Seq(LoadRaster(foo)) and our
    // function is AddConstant(_, 3) we should end up with 
    // an op that returns Seq(AddConstant(LoadRaster(foo),3))
    // 
    val newOp = TransformSequenceOfOperations(elements)(f)

    builder.setOp(newOp)
    val result = builder.result()
    result
  }

  def reduce[T1 >: T](reducer:(T1,T1) => T1)(implicit mf:Manifest[T1]):ValueDataSource[T1] = 
    ValueDataSource( logic.Collect(elements) map (_.reduce(reducer)) )
  

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

}
