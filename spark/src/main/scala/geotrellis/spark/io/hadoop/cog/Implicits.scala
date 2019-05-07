package geotrellis.spark.io.hadoop.cog

import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.tiling.SpatialComponent
import geotrellis.layers.LayerId
import geotrellis.layers.Reader
import geotrellis.layers.hadoop._
import geotrellis.layers.hadoop.cog._
import geotrellis.spark.io.hadoop._
import geotrellis.util.MethodExtensions

import spray.json._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import org.apache.spark.SparkContext

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withHadoopCOGCollectionLayerReaderMethods(val self: HadoopCOGCollectionLayerReader.type) extends MethodExtensions[HadoopCOGCollectionLayerReader.type] {
    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCOGCollectionLayerReader =
      HadoopCOGCollectionLayerReader(HadoopAttributeStore(rootPath))
  }

  implicit class withHadoopCOGValueReaderMethods(val self: HadoopCOGValueReader.type) extends MethodExtensions[HadoopCOGValueReader.type] {
    def apply[
      K: JsonFormat: SpatialComponent: ClassTag,
      V <: CellGrid[Int]: GeoTiffReader
    ](attributeStore: HadoopAttributeStore, layerId: LayerId)(implicit sc: SparkContext): Reader[K, V] =
      new HadoopCOGValueReader(attributeStore, sc.hadoopConfiguration).reader[K, V](layerId)

    def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCOGValueReader =
      HadoopCOGValueReader(HadoopAttributeStore(rootPath))
  }
}
