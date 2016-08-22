package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._
import geotrellis.util._
import spray.json._

import scalaz.std.vector._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}
import java.util.concurrent.{ExecutorService, Executors}

import scala.reflect._

abstract class CollectionLayerReader[ID] { self =>
  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M], indexFilterOnly: Boolean): Seq[(K, V)] with Metadata[M]

  def reader[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ]: Reader[ID, Seq[(K, V)] with Metadata[M]] =
    new Reader[ID, Seq[(K, V)] with Metadata[M]] {
      def read(id: ID): Seq[(K, V)] with Metadata[M] =
        self.read[K, V, M](id)
    }

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M]): Seq[(K, V)] with Metadata[M] =
    read[K, V, M](id, rasterQuery, false)

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID): Seq[(K, V)] with Metadata[M] =
    read[K, V, M](id, new LayerQuery[K, M])

  def query[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](layerId: ID): BoundLayerQuery[K, M, Seq[(K, V)] with Metadata[M]] =
    new BoundLayerQuery(new LayerQuery, read[K, V, M](layerId, _))
}

object CollectionLayerReader {
  def njoin[K: AvroRecordCodec: Boundable, V: AvroRecordCodec](
    ranges: Seq[(Long, Long)],
    readFunc: Iterator[Long] => Option[(Vector[(K, V)], Iterator[Long])],
    threads: Int
   ): Seq[(K, V)] = {
    val pool = Executors.newFixedThreadPool(threads)

    val range: Process[Task, Iterator[Long]] = Process.unfold(ranges.toIterator) { iter =>
      if (iter.hasNext) {
        val (start, end) = iter.next()
        Some((start to end).toIterator, iter)
      }
      else None
    }

    val read: Iterator[Long] => Process[Task, Vector[(K, V)]] = { iterator => Process.unfold(iterator)(readFunc) }

    val result =
      nondeterminism
        .njoin(maxOpen = threads, maxQueued = threads) { range map read }(Strategy.Executor(pool))
        .runFoldMap(identity).unsafePerformSync

    pool.shutdown(); result
  }
}
