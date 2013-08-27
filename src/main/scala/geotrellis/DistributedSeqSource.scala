package geotrellis

object DistributedSeqSource {
  def apply[A](seq:Seq[A]) = {
  }

  implicit def canBuildSourceFromGen[T]:CanBuildSourceFrom[DataSource[_,_],T,DistributedSeqSource[T]] = 
    new CanBuildSourceFrom[DataSource[_,_],
                           T,
                           DistributedSeqSource[T]] {
    def apply() = new DistributedSeqSourceBuilder[T]
    def apply(src:DataSource[_,_]) = 
      new DistributedSeqSourceBuilder[T]
    }

}

class DistributedSeqSource[A](val seqOp:Op[Seq[Op[A]]]) extends SeqSource[A] with SeqSourceLike[A,SeqSource[A]] 
