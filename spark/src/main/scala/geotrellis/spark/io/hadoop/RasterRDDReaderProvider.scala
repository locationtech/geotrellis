package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.rdd.RDD
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat

import scala.collection.mutable
import org.joda.time.{DateTimeZone, DateTime}

import scala.reflect._

trait RasterRDDReaderProvider[Key] extends Logging {

  implicit val keyCTaggable: ClassTag[Key]
  type KeyWritable >: Null <: org.apache.hadoop.io.WritableComparable[KeyWritable] with IndexedKeyWritable[Key]
  val kwClass: Class[KeyWritable]
  implicit val kwCTaggable: ClassTag[KeyWritable] = implicitly[ClassTag[KeyWritable]]

  type FMFInputFormat <: org.apache.hadoop.mapreduce.InputFormat[KeyWritable, TileWritable]//<: FilterMapFileInputFormat[Key, KeyWritable, TileWritable]
  val fmfClass: Class[FMFInputFormat] // implicit val inputFormatCTaggable: ClassTag[FMFInputFormat]

  def filterDefinition(
    filterSet: FilterSet[Key],
    keyBounds: KeyBounds[Key],
    keyIndex: KeyIndex[Key]): FilterMapFileInputFormat.FilterDefinition[Key]

  def reader(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    keyIndex: KeyIndex[Key],
    keyBounds: KeyBounds[Key]
  )(implicit sc: SparkContext): FilterableRasterRDDReader[Key] =
    new FilterableRasterRDDReader[Key] {
      def read(layerId: LayerId, filterSet: FilterSet[Key]): RasterRDD[Key] = {
        val path = layerMetaData.path

        val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

        logDebug(s"Loading $layerId from $dataPath")

        val conf = sc.hadoopConfiguration
        val inputConf = conf.withInputPath(dataPath)

        val writableRdd: RDD[(KeyWritable, TileWritable)] =
          if(filterSet.isEmpty) {
            sc.newAPIHadoopRDD(
              inputConf,
              classOf[SequenceFileInputFormat[KeyWritable, TileWritable]],
              kwClass,
              //classTag[KeyWritable].runtimeClass.asInstanceOf[Class[KeyWritable]],
              //classOf[KeyWritable].runtimeClass,
              classOf[TileWritable]
            )
          } else {
            inputConf.setSerialized(
              FilterMapFileInputFormat.FILTER_INFO_KEY,
              filterDefinition(filterSet, keyBounds, keyIndex))

            sc.newAPIHadoopRDD(
              inputConf,
              fmfClass,
              kwClass,
              classOf[TileWritable]
            )
          }

        val rasterMetaData = layerMetaData.rasterMetaData

        asRasterRDD(rasterMetaData) {
          writableRdd
            .map  { case (keyWritable: KeyWritable, tileWritable: TileWritable) =>
              ???
          }
        }
      }
    }
}
