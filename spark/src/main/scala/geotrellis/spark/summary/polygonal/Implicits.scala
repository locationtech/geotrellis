package geotrellis.spark.summary.polygonal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.util._

import org.apache.spark.rdd._
import scala.reflect._

object Implicits extends Implicits

trait Implicits {
  implicit class withZonalSummaryTileLayerRDDMethods[
    K,
    M: GetComponent[?, LayoutDefinition]
  ](val self: RDD[(K, Tile)] with Metadata[M])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends PolygonalSummaryTileLayerRDDMethods[K, M] with Serializable

  implicit class withZonalSummaryFeatureRDDMethods[G <: Geometry, D](val featureRdd: RDD[Feature[G, D]])
      extends PolygonalSummaryFeatureRDDMethods[G, D]

  implicit class withZonalSummaryKeyedFeatureRDDMethods[K, G <: Geometry, D](val featureRdd: RDD[(K, Feature[G, D])])
    (implicit val keyClassTag: ClassTag[K])
      extends PolygonalSummaryKeyedFeatureRDDMethods[K, G, D]

  implicit class withZonalSummaryTileLayerCollectionMethods[
    K,
    M: GetComponent[?, LayoutDefinition]
  ](val self: Seq[(K, Tile)] with Metadata[M])
   (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
    extends PolygonalSummaryTileLayerCollectionMethods[K, M] with Serializable

  implicit class withZonalSummaryFeatureCollectionMethods[G <: Geometry, D](val featureCollection: Seq[Feature[G, D]])
    extends PolygonalSummaryFeatureCollectionMethods[G, D]

  implicit class withZonalSummaryKeyedFeatureCollectionMethods[K, G <: Geometry, D](val featureCollection: Seq[(K, Feature[G, D])])
    (implicit val keyClassTag: ClassTag[K])
      extends PolygonalSummaryKeyedFeatureCollectionMethods[K, G, D]

}
