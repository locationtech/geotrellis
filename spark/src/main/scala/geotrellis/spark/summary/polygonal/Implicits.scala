package geotrellis.spark.summary.polygonal

import geotrellis.spark._
import geotrellis.vector._
import org.apache.spark.rdd._
import scala.reflect._

object Implicits extends Implicits

trait Implicits {
  implicit class withZonalSummaryTileLayerRDDMethods[K](val self: TileLayerRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends PolygonalSummaryTileLayerRDDMethods[K] with Serializable

  implicit class withZonalSummaryFeatureRDDMethods[G <: Geometry, D](val featureRdd: RDD[Feature[G, D]])
      extends PolygonalSummaryFeatureRDDMethods[G, D]

  implicit class withZonalSummaryKeyedFeatureRDDMethods[K, G <: Geometry, D](val featureRdd: RDD[(K, Feature[G, D])])
    (implicit val keyClassTag: ClassTag[K])
      extends PolygonalSummaryKeyedFeatureRDDMethods[K, G, D]

}