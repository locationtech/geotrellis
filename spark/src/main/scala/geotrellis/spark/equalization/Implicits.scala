package geotrellis.spark.equalization

import geotrellis.raster._
import geotrellis.spark._

import org.apache.spark.rdd.RDD


object Implicits extends Implicits

trait Implicits {
  implicit class withRDDSinglebandEqualizationMethods[K, M](val self: RDD[(K, Tile)] with Metadata[M]) extends RDDSinglebandEqualizationMethods[K, M]

  implicit class withRDDMultibandEqualizationMethods[K, M](val self: RDD[(K, MultibandTile)] with Metadata[M]) extends RDDMultibandEqualizationMethods[K, M]
}
