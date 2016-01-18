package geotrellis.spark.merge

import geotrellis.raster._
import geotrellis.raster.merge._

import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

object TileRDDMerge {
  def apply[K: ClassTag, V: ClassTag: ? => TileMergeMethods[V]](rdd: RDD[(K, V)], other: RDD[(K, V)]): RDD[(K, V)] = {
    rdd
      .cogroup(other)
      .map { case (key, (myTiles, otherTiles)) =>
        if (myTiles.nonEmpty && otherTiles.nonEmpty) {
          val a = myTiles.reduce(_ merge _)
          val b = otherTiles.reduce(_ merge _)
          (key, a merge b)
        } else if (myTiles.nonEmpty) {
          (key, myTiles.reduce(_ merge _))
        } else {
          (key, otherTiles.reduce(_ merge _))
        }
      }
  }

  def apply[K: ClassTag, V: ClassTag: ? => TileMergeMethods[V]](rdd: RDD[(K, V)], partitioner: Option[Partitioner]): RDD[(K, V)] = {
    partitioner match {
      case Some(p) =>
        rdd
          .reduceByKey(p, _ merge _)
      case None =>
        rdd
          .reduceByKey(_ merge _)
    }
  }
}
