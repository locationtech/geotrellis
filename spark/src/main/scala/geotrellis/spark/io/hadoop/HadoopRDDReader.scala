package geotrellis.spark.io.hadoop

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.raster._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.InputFormat

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat

import scala.reflect._

trait HadoopFormat[K, V] extends Serializable {
  type KW <: AvroKeyWritable[K, KW]
  type VW <: AvroWritable[V]
  type FilteredInputFormat <: InputFormat[KW, VW]
  type FullInputFormat <: InputFormat[KW, VW]

  def kClass: Class[KW]
  def vClass: Class[VW]
  def fullInputFormatClass: Class[FullInputFormat]
  def filteredInputFormatClass: Class[FilteredInputFormat]
}

object HadoopFormat {
  def Aux[K, V, KWP <: AvroKeyWritable[K, KWP] : ClassTag, VWP <: AvroWritable[V] : ClassTag, FIF <: InputFormat[KWP, VWP]: ClassTag]: HadoopFormat[K, V] =
    new HadoopFormat[K, V] {
      type KW = KWP
      type VW  = VWP
      type FilteredInputFormat = FIF
      type FullInputFormat = SequenceFileInputFormat[KWP, VWP]

      val kClass = classTag[KWP].runtimeClass.asInstanceOf[Class[KW]]
      val vClass = classTag[VWP].runtimeClass.asInstanceOf[Class[VW]]
      val filteredInputFormatClass = classTag[FIF].runtimeClass.asInstanceOf[Class[FIF]]
      val fullInputFormatClass = classOf[SequenceFileInputFormat[KWP, VWP]]
    }
}

class HadoopRDDReader[K, V](catalogConfig: HadoopCatalogConfig)(implicit format: HadoopFormat[K, V]) extends LazyLogging {

  def readFully(
    path: Path)
  (implicit sc: SparkContext): RDD[(K, V)] = {
    val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

    logger.debug(s"Loading from $dataPath")

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    sc.newAPIHadoopRDD(
        inputConf,
        format.fullInputFormatClass,
        format.kClass,
        format.vClass)
      .map { case (keyWritable, valueWritable) =>
        (keyWritable.get()._2, valueWritable.get())
      }
  }

  def readFiltered(
    path: Path,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)])
  (implicit sc: SparkContext): RDD[(K, V)] = {
    val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

    logger.debug(s"Loading from $dataPath")

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    inputConf.setSerialized (FilterMapFileInputFormat.FILTER_INFO_KEY,
      (queryKeyBounds, queryKeyBounds.flatMap(decomposeBounds).toArray))

    sc.newAPIHadoopRDD(
        inputConf,
        format.filteredInputFormatClass,
        format.kClass,
        format.vClass)
      .map  { case (keyWritable, tileWritable) =>
        (keyWritable.get()._2, tileWritable.get())
      }
  }
}
