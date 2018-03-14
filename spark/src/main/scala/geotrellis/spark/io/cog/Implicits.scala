package geotrellis.spark.io.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.util._

import org.apache.spark.rdd.RDD

import java.net.URI

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withCOGLayerWriteMethods[K: SpatialComponent: ClassTag, V <: CellGrid: ClassTag](val self: RDD[(K, GeoTiff[V])]) extends MethodExtensions[RDD[(K, GeoTiff[V])]] {
    def write(keyIndex: KeyIndex[K], uri: URI): Unit =
      COGLayer.write[K, V](self)(keyIndex, uri)
  }
}
