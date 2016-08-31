package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._
import geotrellis.util._

import org.apache.spark.rdd._
import spray.json._
import scalaz.std.vector._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}

import scala.reflect._
import java.util.concurrent.Executors

trait LayerReader[ID] {
  def defaultNumPartitions: Int

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, numPartitions: Int): RDD[(K, V)] with Metadata[M]

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID): RDD[(K, V)] with Metadata[M] =
    read(id, defaultNumPartitions)

  def reader[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ]: Reader[ID, RDD[(K, V)] with Metadata[M]] =
    new Reader[ID, RDD[(K, V)] with Metadata[M]] {
      def read(id: ID): RDD[(K, V)] with Metadata[M] =
        LayerReader.this.read[K, V, M](id)
    }
}

object LayerReader {
  def njoin[K, V](
    ranges: Iterator[(Long, Long)],
    readFunc: Iterator[Long] => Option[(Vector[(K, V)], Iterator[Long])],
    threads: Int
   ): Seq[(K, V)] = {
    val pool = Executors.newFixedThreadPool(threads)

    val range: Process[Task, Iterator[Long]] = Process.unfold(ranges) { iter =>
      if (iter.hasNext) {
        val (start, end) = iter.next()
        Some((start to end).toIterator, iter)
      }
      else None
    }

    val read: Iterator[Long] => Process[Task, Vector[(K, V)]] = { iterator => Process.unfold(iterator)(readFunc) }

    try {
      nondeterminism
        .njoin(maxOpen = threads, maxQueued = threads) { range map read }(Strategy.Executor(pool))
        .runFoldMap(identity).unsafePerformSync
    } finally pool.shutdown()
  }
}
