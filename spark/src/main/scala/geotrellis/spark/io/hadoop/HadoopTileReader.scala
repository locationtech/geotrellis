package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.hadoop.formats.FilterMapFileInputFormat
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.util.KryoWrapper

import org.apache.avro.Schema
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io._
import org.apache.spark.SparkContext
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag

class HadoopTileReader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](val attributeStore: HadoopAttributeStore)
    (implicit sc: SparkContext) extends Reader[LayerId, Reader[K, V]] {

  val conf = attributeStore.hadoopConfiguration

  def read(layerId: LayerId): Reader[K, V] = new Reader[K, V] {

    val (layerMetadata, _, keyIndex, schema) =
      attributeStore.readLayerAttributes[HadoopLayerHeader, Unit, KeyIndex[K], Schema](layerId)

    val dataPath = layerMetadata.path.suffix(HadoopCatalogConfig.SEQFILE_GLOB)
    val inputConf = conf.withInputPath(dataPath)

    val codec = KeyValueRecordCodec[K, V]
    val kwSchema = KryoWrapper(schema) //Avro Schema is not Serializable

    def read(key: K): V = {
      val keyBounds = KeyBounds[K](key, key)
      val filterDefinition = keyIndex.indexRanges(keyBounds).toArray
      inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY, filterDefinition)

      val _codec = codec
      val _kwSchema = kwSchema

      // TODO: There must be a way to do this through Readers, which must be faster
      sc.newAPIHadoopRDD(
        inputConf,
        classOf[FilterMapFileInputFormat],
        classOf[LongWritable],
        classOf[BytesWritable]
      )
        .flatMap { case (keyWritable, valueWritable) =>
          AvroEncoder.fromBinary(_kwSchema.value, valueWritable.getBytes)(_codec)
            .filter { row => row._1 == key }
        }.first()._2
    }
  }
}

object HadoopTileReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](attributeStore: HadoopAttributeStore)
    (implicit sc: SparkContext): HadoopTileReader[K, V] =
    new HadoopTileReader[K, V](attributeStore)

  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](rootPath: Path)
    (implicit sc: SparkContext): HadoopTileReader[K, V] =
    apply(HadoopAttributeStore(rootPath))
}
