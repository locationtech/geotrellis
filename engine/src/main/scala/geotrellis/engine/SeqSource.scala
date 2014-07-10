package geotrellis.engine

import akka.actor.ActorRef

case class SeqSource[T](elements: Op[Seq[Op[T]]]) extends DataSource[T, Seq[T]] {
  type Self = SeqSource[T]

  def convergeOp: Op[Seq[T]] = logic.Collect(elements)

  def distribute(cluster: Option[ActorRef]) = SeqSource(distributeOps(cluster))
  def cached(implicit engine: Engine) = SeqSource(cachedOps(engine))
}
