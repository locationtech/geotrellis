package geotrellis.spark.summary

import geotrellis.raster._
import geotrellis.spark._
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withStatsTileRDDMethods[K](val self: RDD[(K, Tile)])
    (implicit val keyClassTag: ClassTag[K]) extends StatsTileRDDMethods[K]

  implicit class withStatsMultibandTileRDDMethods[K](val self: RDD[(K, MultibandTile)])
    (implicit val keyClassTag: ClassTag[K]) extends StatsMultibandTileRDDMethods[K]

  implicit class withStatsTileCollectionMethods[K](val self: Seq[(K, Tile)]) extends StatsTileCollectionMethods[K]
}
