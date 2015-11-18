package geotrellis.spark.op.zonal

import geotrellis.spark._
import geotrellis.vector._
import org.apache.spark.rdd._
import scala.reflect._

package object summary {
  implicit class ZonalSummaryRasterRDDMethodExtensions[K](val rasterRDD: RasterRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends ZonalSummaryRasterRDDMethods[K] with Serializable

  implicit class ZonalSummaryFeatureRDDMethodExtensions[G <: Geometry, D](val featureRdd: RDD[Feature[G, D]]) 
      extends ZonalSummaryFeatureRDDMethods[G, D]

  implicit class ZonalSummaryKeyedFeatureRDDMethodExtensions[K, G <: Geometry, D](val featureRdd: RDD[(K, Feature[G, D])]) 
    (implicit val keyClassTag: ClassTag[K])
      extends ZonalSummaryKeyedFeatureRDDMethods[K, G, D]

}
