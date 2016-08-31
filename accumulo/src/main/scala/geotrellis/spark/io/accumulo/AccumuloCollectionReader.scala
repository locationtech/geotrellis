package geotrellis.spark.io.accumulo

import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.{Boundable, KeyBounds}

import scalaz.std.vector._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, channel, nondeterminism, tee}
import org.apache.accumulo.core.data.{Range => AccumuloRange}
import org.apache.accumulo.core.security.Authorizations
import org.apache.avro.Schema
import org.apache.hadoop.io.Text
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.reflect.ClassTag
import java.util.concurrent.Executors

object AccumuloCollectionReader {
  def read[K: Boundable: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    table: String,
    columnFamily: Text,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[AccumuloRange],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.accumulo.threads.collection.read")
  )(implicit instance: AccumuloInstance): Seq[(K, V)] = {
    if(queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val codec = KeyValueRecordCodec[K, V]
    val includeKey = (key: K) => queryKeyBounds.includeKey(key)

    val ranges = queryKeyBounds.flatMap(decomposeBounds).toIterator

    val pool = Executors.newFixedThreadPool(threads)

    val range: Process[Task, AccumuloRange] = Process.unfold(ranges) { iter =>
      if (iter.hasNext) Some(iter.next(), iter)
      else None
    }

    val readChannel = channel.lift { (range: AccumuloRange) => Task {
      val scanner = instance.connector.createScanner(table, new Authorizations())
      scanner.setRange(range)
      scanner.fetchColumnFamily(columnFamily)
      val result = scanner.iterator.map { case entry =>
        AvroEncoder.fromBinary(writerSchema.getOrElse(codec.schema), entry.getValue.get)(codec)
      }.flatMap { pairs: Vector[(K, V)] =>
        if(filterIndexOnly) pairs
        else pairs.filter { pair => includeKey(pair._1) }
      }.toVector
      scanner.close()
      result
    }(pool) }

    val read = range.tee(readChannel)(tee.zipApply).map(Process.eval)


    try {
      nondeterminism
        .njoin(maxOpen = threads, maxQueued = threads) { read }(Strategy.Executor(pool))
        .runFoldMap(identity).unsafePerformSync: Seq[(K, V)]
    } finally pool.shutdown()
  }
}
