package geotrellis.spark.knn

import geotrellis.spark._
import geotrellis.vector._
import com.vividsolutions.jts.geom.Envelope
import com.vividsolutions.jts.geom.Coordinate

import org.apache.spark.rdd.RDD

object KNearestRDD {
  def kNearest[T](rdd: RDD[T], x: Double, y: Double, k: Int)(f: T => Extent): List[T] =
    kNearest(rdd, new Envelope(new Coordinate(x, y)), k)(f)

  def kNearest[T](rdd: RDD[T], p: (Double, Double), k: Int)(f: T => Extent): List[T] =
    kNearest(rdd, new Envelope(new Coordinate(p._1, p._2)), k)(f)

  /**
   * Determines the k-nearest neighbors of an RDD of objects which can be coerced into Extents.
   */
  def kNearest[T](rdd: RDD[T], ex: Extent, k: Int)(f: T => Extent): List[T] = {
    val candidates = rdd.glom.map { 
        arr => SpatialIndex.fromExtents(arr)(f)
      }.map (_.kNearest (ex, k)).reduce( _ ++ _ )

    SpatialIndex.fromExtents(candidates)(f).kNearest(ex, k)
  }
}
