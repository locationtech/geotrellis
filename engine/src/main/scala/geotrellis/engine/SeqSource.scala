package geotrellis.engine

import akka.actor.ActorRef

@deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
case class SeqSource[T](elements: Op[Seq[Op[T]]]) extends DataSource[T, Seq[T]] {
  type Self = SeqSource[T]

  def convergeOp: Op[Seq[T]] = logic.Collect(elements)

  def distribute(cluster: Option[ActorRef]) = SeqSource(distributeOps(cluster))
  def cached(implicit engine: Engine) = SeqSource(cachedOps(engine))
}
