package geotrellis.spark.op.zonal.summary

import geotrellis.spark._
import geotrellis.vector._
import org.apache.spark.rdd._
import scala.reflect._

object Implicits extends Implicits

trait Implicits {
  implicit class withZonalSummaryRasterRDDMethods[K](val self: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends ZonalSummaryRasterRDDMethods[K] with Serializable

  implicit class withZonalSummaryFeatureRDDMethods[G <: Geometry, D](val featureRdd: RDD[Feature[G, D]])
      extends ZonalSummaryFeatureRDDMethods[G, D]

  implicit class withZonalSummaryKeyedFeatureRDDMethods[K, G <: Geometry, D](val featureRdd: RDD[(K, Feature[G, D])])
    (implicit val keyClassTag: ClassTag[K])
      extends ZonalSummaryKeyedFeatureRDDMethods[K, G, D]

}