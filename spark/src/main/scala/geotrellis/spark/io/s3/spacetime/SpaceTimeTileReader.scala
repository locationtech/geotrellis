package geotrellis.spark.io.s3.spacetime

import geotrellis.spark._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.accumulo.core.security.Authorizations
import scala.reflect.ClassTag
import scala.collection.JavaConversions._

class SpaceTimeTileReader[T: ClassTag] extends TileReader[SpaceTimeKey, T] {
  val encodeKey = spacetime.encodeKey
}
