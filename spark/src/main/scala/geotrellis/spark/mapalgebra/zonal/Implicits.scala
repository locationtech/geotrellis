package geotrellis.spark.mapalgebra.zonal

import geotrellis.raster._
import geotrellis.spark._
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withZonalTileRDDMethods[K](val self: RDD[(K, Tile)])
    (implicit val keyClassTag: ClassTag[K]) extends ZonalTileRDDMethods[K]
}