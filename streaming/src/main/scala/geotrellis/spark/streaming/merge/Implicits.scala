package geotrellis.spark.streaming.merge

import geotrellis.raster.merge._
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withTileDStreamMergeMethods[K: ClassTag, V: ClassTag: ? => TileMergeMethods[V]](self: DStream[(K, V)])
    extends TileDStreamMergeMethods[K, V](self)
}
