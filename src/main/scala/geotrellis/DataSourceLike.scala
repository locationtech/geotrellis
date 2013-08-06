package geotrellis

trait DataSourceLike[+T,+Repr <: DataSource[T]] { self:Repr =>
  def map[B,That](f:Op[T] => Op[B])(implicit bf:CanBuildSourceFrom[Repr,B,That]):That =  {
    val builder = bf.apply()
	  val newOp = this.partitions.map( seq =>
	      seq.map {
	        op => f(op)
	      }
	)
	builder.op = newOp
    builder.result()
  }
}
