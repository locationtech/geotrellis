package geotrellis

object DistributedSeqSource {
  def apply[A](seq:Seq[A]) = {

  }
}


class DistributedSeqSource[A](val seqOp:Op[Seq[Op[A]]]) extends SeqSource[A] with SeqSourceLike[A,SeqSource[A]] {
  def partitions = seqOp.map(_.map(_.map(Seq(_))))

}
