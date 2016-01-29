package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.utils.KryoWrapper

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.avro.Schema
import org.apache.hadoop.io._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

object HadoopRDDReader extends LazyLogging {

  def readFully[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](path: Path, writerSchema: Option[Schema] = None)(implicit sc: SparkContext): RDD[(K, V)] = {
    val dataPath = path.suffix(HadoopCatalogConfig.SEQFILE_GLOB)

    logger.debug(s"Loading from $dataPath")

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    val codec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable

    sc.newAPIHadoopRDD(
      inputConf,
      classOf[SequenceFileInputFormat[LongWritable, BytesWritable]],
      classOf[LongWritable],
      classOf[BytesWritable]
     )
      .flatMap { case (keyWritable, valueWritable) =>
        AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(codec.schema), valueWritable.getBytes)(codec)
      }
  }

  def readFiltered[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](
    path: Path,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    writerSchema: Option[Schema] = None)
  (implicit sc: SparkContext): RDD[(K, V)] = {
    val dataPath = path.suffix(HadoopCatalogConfig.SEQFILE_GLOB)

    logger.debug(s"Loading from $dataPath")

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val indexRanges = queryKeyBounds.flatMap(decomposeBounds).toArray
    inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY, indexRanges)

    val codec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable

    sc.newAPIHadoopRDD(
      inputConf,
      classOf[FilterMapFileInputFormat],
      classOf[LongWritable],
      classOf[BytesWritable]
    )
      .flatMap { case (keyWritable, valueWritable) =>
        AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(codec.schema), valueWritable.getBytes)(codec)
          .filter { row => includeKey(row._1) }
      }
  }
}
