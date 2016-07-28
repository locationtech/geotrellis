package geotrellis.spark

object ContextCollection {
  def apply[K, V, M](sequence: Seq[(K, V)], metadata: M): Seq[(K, V)] with Metadata[M] =
    new ContextCollection(sequence, metadata)

  implicit def tupleToContextRDD[K, V, M](tup: (Seq[(K, V)], M)): ContextCollection[K, V, M] =
    new ContextCollection(tup._1, tup._2)
}

class ContextCollection[K, V, M](val sequence: Seq[(K, V)], val metadata: M) extends Seq[(K, V)] with Metadata[M] {
  def length: Int = sequence.length
  def apply(idx: Int): (K, V) = sequence(idx)
  def iterator: Iterator[(K, V)] = sequence.iterator
}
