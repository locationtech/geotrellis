package geotrellis.spark.partitioner

import geotrellis.spark.{SpaceTimeKey, KeyBounds, Boundable}
import org.apache.spark._
import org.apache.spark.rdd._

object SpatialJoin {
  def innerJoin[K, V](rdds: RDD[(K, V)]*) = ???

  def leftJoin[K, V](left: RDD[(K, V)], right: RDD[(K, V)]): RDD[(K, V)] = {
    val sc = left.sparkContext
    left.partitioner match {
      case Some(leftPart: SpacePartitioner[K]) =>
        right.partitioner match {
          case Some(rightPart: SpacePartitioner[K]) =>
            //bingo, we can do pipelined join
            new SpatialJoinRDD[K, V](sc, List(left, right), leftPart)
          case _ =>
            //Right side partitioning is unknown. We know which partition they're going to,
            // but not where they're coming from, we have to filter and shuffle right side to match our partitions
            val rightPreJoin: RDD[(K,V)] = new ShuffledRDD(right.filter(r => leftPart.containsKey(r._1)), leftPart)
            new SpatialJoinRDD[K, V](sc, List(left, rightPreJoin), leftPart)
        }

      case None =>
        /*
        Left side has no partitioner so god knows where it came from.
        We can, of course, iterate over left rdd to collect KeyBounds and use them to create target SpacePartitioner.
        It's not clear how to avoid this and keep the lazy nature of RDD. TBD
        */
        sys.error(s"leftJoins into RDD without SpacePartitioner not supported")
    }
  }

  def innerJoin[K, V](rdds: SpaceRDD[K, V]*)(implicit boundable: Boundable[KeyBounds[K]]) = {
    val sc = rdds.head.sparkContext
    rdds.map(_.bounds)



    match {
      case Some(bounds) =>
        new SpatialJoinRDD[K, V](sc, rdds, new SpacePartitioner[K](bounds))
      case None =>
        sc.emptyRDD[(K, V)]
    }
  }

  def leftJoin[K, V](left: SpaceRDD[K, V], right: SpaceRDD[K, V]) = {
    val sc = left.sparkContext
    new SpatialJoinRDD(sc, List(left, right), left.getSpacePartitioner)
  }
}
