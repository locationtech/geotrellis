package geotrellis


case class TransformSequenceOfOperations[T,B](opSeq:Op[Seq[Op[T]]])(f:Op[T]=> Op[B]) extends Op1(opSeq) ({
	(opSeq) => {
	  println("Transforming sequence of operations.")
	  println(s"Length of sequnce is: ${opSeq.length}")
	  val newSeq:Seq[Op[B]] = opSeq.map(
         (op:Op[T]) => {
            println(s"Old op was $op")
            val op2:Op[B] = f(op)
            println(s"New op is $op2")
            op2
         }
        )
        Result(newSeq)
	}})
    

trait DataSourceLike[+T,+Repr <: DataSource[T]] { self:Repr =>
  def map[B:Manifest,That](f:Op[T] => Op[B])(implicit bf:CanBuildSourceFrom[Repr,B,That]):That =  {
    println("in map")
    println(s"this is: $this")
    val builder = bf.apply(this)
    val partitions:Op[Seq[Op[T]]] = this.partitions

    // Apply the provided op to the operations inside the
    // future sequence of operations.  For example,
    // if we have an Op that returns Seq(LoadRaster(foo)) and our
    // function is AddConstant(_, 3) we should end up with 
    // an op that returns Seq(AddConstant(LoadRaster(foo),3))

    // 
    println(s"Partitions before map is: $partitions")
    val newOp = TransformSequenceOfOperations(partitions)(f)
    /*
    val newOp:Op[Seq[Op[B]]] = partitions.map( 
      // map, so output Seq[Op[B]]
      (seq:Seq[Op[T]]) => {
        println("hi,s in partition flatmap")
        println(s"length of seq: ${ seq.length }")
        // replace each element of the sequence ...
        seq.map {
	  op:Op[T] => {
            println(s"Old op was $op")
            val op2:Op[B] = f(op)
            println(s"New op is $op2")
            op2
          }
        }
      }
    )*/
    println(s"New op (partitions) after map is: $newOp")
    builder.setOp(newOp)
    val result = builder.result()
    println(s"result partitions is "+result.asInstanceOf[raster.RasterSource].partitions)
    result
  }
}
