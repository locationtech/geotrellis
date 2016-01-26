package geotrellis.spark.op

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withCombineMethods[K: ClassTag, V: ClassTag](val self: RDD[(K, V)])
    extends CombineMethods[K, V]

  implicit class withCombineTraversableMethods[K: ClassTag, V: ClassTag](rs: Traversable[RDD[(K, V)]]) {
    /*def combineValues[R: ClassTag](f: Traversable[V] => R): RDD[(K, R)] = combineValues(f, None)*/
    def combineValues[R: ClassTag](f: Traversable[V] => R, partitioner: Option[Partitioner]): RDD[(K, R)] =
      rs.head.combineValues(rs.tail, partitioner)(f)
  }
}