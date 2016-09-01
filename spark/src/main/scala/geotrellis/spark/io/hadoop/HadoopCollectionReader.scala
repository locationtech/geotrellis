package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.hadoop.formats.FilterMapFileInputFormat
import geotrellis.spark.util.cache.LRUCache

import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io._
import org.apache.hadoop.fs.Path
import scalaz.std.vector._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}
import com.typesafe.config.ConfigFactory

import java.util.concurrent.Executors

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
    writerSchema: Option[Schema] = None,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.hadoop.threads.collection.read")): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)
    val indexRanges = queryKeyBounds.flatMap(decomposeBounds).toIterator

    val codec = KeyValueRecordCodec[K, V]

    val pathRanges: Vector[(Path, Long, Long)] =
      FilterMapFileInputFormat.layerRanges(path, conf)

    LayerReader.njoin[K, V](indexRanges, threads){ index: Long =>
      val valueWritable = pathRanges
        .find { row => index >= row._2 && index <= row._3 }
        .map { case (p, _, _) => readers.getOrInsert(p, new MapFile.Reader(p, conf)) }
        .map(_.get(new LongWritable(index), new BytesWritable()).asInstanceOf[BytesWritable])
        .getOrElse { println(s"Index ${index} not found."); null }

      if (valueWritable == null) Vector.empty
      else {
        val items = AvroEncoder.fromBinary(writerSchema.getOrElse(codec.schema), valueWritable.getBytes)(codec)
        if (indexFilterOnly) items
        else items.filter { row => includeKey(row._1) }
      }
    }
  }
}

object HadoopCollectionReader {
  def apply(maxOpenFiles: Int = 16) = new HadoopCollectionReader(maxOpenFiles)
}
