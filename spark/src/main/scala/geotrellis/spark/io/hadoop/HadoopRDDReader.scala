package geotrellis.spark.io.hadoop

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import geotrellis.spark.io.hadoop.formats._
import org.apache.hadoop.fs.Path

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class HadoopRDDReader[K, V](catalogConfig: HadoopCatalogConfig)(implicit format: HadoopFormat[K, V]) extends LazyLogging {

  def readFully(path: Path)(implicit sc: SparkContext): RDD[(K, V)] = {
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
