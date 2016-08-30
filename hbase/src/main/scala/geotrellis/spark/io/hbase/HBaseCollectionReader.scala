package geotrellis.spark.io.hbase

import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.index.MergeQueue
import geotrellis.spark.util.KryoWrapper
import geotrellis.spark.{Boundable, KeyBounds, LayerId}

import scalaz.std.vector._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{channel, nondeterminism, tee}
import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.filter.PrefixFilter
import org.apache.avro.Schema
import scalaz.stream.Process
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.reflect.ClassTag
import java.util.concurrent.Executors

object HBaseCollectionReader {
  def read[K: Boundable : AvroRecordCodec : ClassTag, V: AvroRecordCodec : ClassTag](
    instance: HBaseInstance,
    table: String,
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.hbase.threads.collection.read")
  ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) // Avro Schema is not Serializable

    val ranges: Seq[(Long, Long)] = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val pool = Executors.newFixedThreadPool(threads)

    val filter = new PrefixFilter(HBaseRDDWriter.layerIdString(layerId))
    val scans = ranges.map { case (start, stop) =>
      HBaseUtils.buildScan(
        table, HBaseRDDWriter.tilesCF,
        HBaseKeyEncoder.encode(layerId, start),
        HBaseKeyEncoder.encode(layerId, stop, trailingByte = true), // add trailing byte, to include stop row
        filter)
    }.toIterator

    val result = instance.withTableConnectionDo(table) { tableConnection =>
      val scan: Process[Task, Scan] = Process.unfold(scans) { iter =>
        if (iter.hasNext) Some(iter.next(), iter)
        else None
      }

      val readChannel = channel.lift { (scan: Scan) => Task {
        val scanner = tableConnection.getScanner(scan)
        val result = scanner.iterator().flatMap { row =>
          val bytes = row.getValue(HBaseRDDWriter.tilesCF, "")
          val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
          if (filterIndexOnly) recs
          else recs.filter { row => includeKey(row._1) }
        } toVector

        scanner.close()
        result
      }(pool) }

      val read = scan.tee(readChannel)(tee.zipApply).map(Process.eval)

      nondeterminism
        .njoin(maxOpen = threads, maxQueued = threads) { read }(Strategy.Executor(pool))
        .runFoldMap(identity).unsafePerformSync
    }: Seq[(K, V)]

    pool.shutdown(); result
  }
}
