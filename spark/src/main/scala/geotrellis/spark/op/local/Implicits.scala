package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withLocalTileRDDMethods[K](val self: RDD[(K, Tile)])
    (implicit val keyClassTag: ClassTag[K]) extends LocalTileRDDMethods[K]

  implicit class withLocalRasterRDDSeqMethods[K](val self: Traversable[RDD[(K, Tile)]])
    (implicit val keyClassTag: ClassTag[K]) extends LocalRasterRDDSeqMethods[K]
}