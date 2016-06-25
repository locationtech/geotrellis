package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.hadoop.formats.FilterMapFileInputFormat
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.util.KryoWrapper
import geotrellis.spark.util.cache._

import org.apache.avro.Schema
import org.apache.hadoop.fs._
import org.apache.hadoop.io._
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkContext
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.collection.immutable._
import scala.reflect.ClassTag

class HadoopValueReader(val attributeStore: HadoopAttributeStore, maxOpenFiles: Int = 16)
    (implicit sc: SparkContext) extends ValueReader[LayerId] {

  val conf = attributeStore.hadoopConfiguration
  val readers = new LRUCache[(LayerId, Path), MapFile.Reader](maxOpenFiles.toLong, {x => 1l}) {
    override def evicted(reader: MapFile.Reader) = reader.close()
  }

  def reader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[HadoopLayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)
    val codec = KeyValueRecordCodec[K, V]
    // Map from last key in each reader to that reader

    val ranges: Vector[(Path, Long, Long)] =
      FilterMapFileInputFormat.layerRanges(header.path, conf)

    def read(key: K): V = {
      val index: Long = keyIndex.toIndex(key)
      val valueWritable: BytesWritable =
        ranges
          .find{ row =>
            index >= row._2 && index <= row._3
          }
          .map { case (path, _, _) =>
            readers.getOrInsert((layerId, path), new MapFile.Reader(path, conf))
          }
          .getOrElse(throw new TileNotFoundError(key, layerId))
          .get(new LongWritable(index), new BytesWritable())
          .asInstanceOf[BytesWritable]

      if (valueWritable == null) throw new TileNotFoundError(key, layerId)
      AvroEncoder
        .fromBinary(writerSchema, valueWritable.getBytes)(codec)
        .filter { row => row._1 == key }
        .headOption
        .getOrElse(throw new TileNotFoundError(key, layerId))
        ._2
    }
  }
}

object HadoopValueReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
    attributeStore: HadoopAttributeStore,
    layerId: LayerId
  )(implicit sc: SparkContext): Reader[K, V] =
    new HadoopValueReader(attributeStore).reader[K, V](layerId)

  def apply(attributeStore: HadoopAttributeStore)
    (implicit sc: SparkContext): HadoopValueReader =
    new HadoopValueReader(attributeStore)

  def apply(rootPath: Path)
    (implicit sc: SparkContext): HadoopValueReader =
    apply(HadoopAttributeStore(rootPath))

  /** Return index from last key index to map file reader for each partition in a layer */
  def partitionReaders(layerPath: Path, conf: Configuration): Array[MapFile.Reader] =
    layerPath
      .getFileSystem(conf)
      .globStatus(layerPath)
      .filter(_.isDirectory)
      .map( status => new MapFile.Reader(status.getPath, conf))
}
