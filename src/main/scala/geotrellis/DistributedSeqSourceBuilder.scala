package geotrellis

class DistributedSeqSourceBuilder[A] extends SeqSourceBuilder[A,DistributedSeqSource[A]] {
  def result = new DistributedSeqSource(_dataDefinition)
}

object DistributedSeqSourceBuilder {
  def apply[A](source:DataSource[A,_]) = {
    val builder = new DistributedSeqSourceBuilder[A]()
    builder
  }
}
