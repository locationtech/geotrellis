package geotrellis


// object SeqSource {
//   implicit def canConvergeTo:CanConvergeTo[Histogram,Histogram] =
//     new CanConvergeTo[Histogram,Histogram] {
//       def converge(ops:Op[Seq[Op[Histogram]]]):Op[Histogram] = ???
//     }
// }

trait SeqSource[A] extends DataSource[Seq[A],A] {
  val seqOp:Op[Seq[Op[A]]]

  def partitions = seqOp
}
