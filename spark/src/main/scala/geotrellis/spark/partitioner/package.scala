package geotrellis.spark

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

package object partitioner {
  implicit class withSpatialRDDMethods[K: ClassTag, V](rdd: RDD[(K, V)]) extends SpatialRDDMethods(rdd)
}
