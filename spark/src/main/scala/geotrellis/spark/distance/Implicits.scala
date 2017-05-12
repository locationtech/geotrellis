package geotrellis.spark.distance

import com.vividsolutions.jts.geom.Coordinate
import org.apache.spark.rdd.RDD

import geotrellis.spark._

object Implicits extends Implicits

trait Implicits {
  implicit class withEuclideanDistanceRDDMethods(val self: RDD[(SpatialKey, Array[Coordinate])]) extends EuclideanDistanceRDDMethods
}
