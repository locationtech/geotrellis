package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.LayerIOError
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.hadoop.formats.FilterMapFileInputFormat
import geotrellis.spark.util.cache.LRUCache

import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io._
import org.apache.hadoop.fs.Path

class HadoopCollectionReader(maxOpenFiles: Int) {
  val readers = new LRUCache[Path, MapFile.Reader](maxOpenFiles.toLong, {x => 1l}) {
    override def evicted(reader: MapFile.Reader) = reader.close()
  }

  def read[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](path: Path,
    conf: Configuration,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    indexFilterOnly: Boolean,
    writerSchema: Option[Schema] = None): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)
    val indexRanges = queryKeyBounds.flatMap(decomposeBounds).toArray

    val codec = KeyValueRecordCodec[K, V]

    val ranges: Vector[(Path, Long, Long)] =
      FilterMapFileInputFormat.layerRanges(path, conf)

    (for {
      range <- indexRanges
      index <- range._1 to range._2
    } yield {
      val keyWritable = new LongWritable(index)
      keyWritable -> ranges
        .find { row => index >= row._2 && index <= row._3 }
        .map { case (p, _, _) => readers.getOrInsert(p, new MapFile.Reader(p, conf)) }
        .getOrElse(throw new LayerIOError(s"Index ${index} not found."))
        .get(keyWritable, new BytesWritable())
        .asInstanceOf[BytesWritable]
    }).flatMap { case (keyWritable, valueWritable) =>
      if (valueWritable == null) Vector()
      else {
        val items = AvroEncoder.fromBinary(writerSchema.getOrElse(codec.schema), valueWritable.getBytes)(codec)
        if (indexFilterOnly)
          items
        else
          items.filter { row => includeKey(row._1) }
      }
    }
  }
}

object HadoopCollectionReader {
  def apply(maxOpenFiles: Int = 16) = new HadoopCollectionReader(maxOpenFiles)
}
