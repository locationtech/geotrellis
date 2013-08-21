package geotrellis


case class TransformSequenceOfOperations[T,B](opSeq:Op[Seq[Op[T]]])(f:Op[T]=> Op[B]) extends Op1(opSeq) ({
  (opSeq) => {
    val newSeq:Seq[Op[B]] = opSeq.map(
      (op:Op[T]) => {
        val op2:Op[B] = f(op)
          op2
      }
    )
      Result(newSeq)
  }})


trait DataSourceLike[+T,+Repr <: DataSource[T]] { self:Repr =>
  def map[B:Manifest,That](f:Op[T] => Op[B])(implicit bf:CanBuildSourceFrom[Repr,B,That]):That =  {
    val builder = bf.apply(this)
    val partitions:Op[Seq[Op[T]]] = this.partitions

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
