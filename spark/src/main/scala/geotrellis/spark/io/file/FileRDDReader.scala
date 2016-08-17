package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.util.KryoWrapper
import geotrellis.util.Filesystem

import scalaz.concurrent.{Strategy, Task}
import scalaz.std.vector._
import scalaz.stream.{Process, nondeterminism}
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import com.typesafe.config.ConfigFactory

import java.io.File
import java.util.concurrent.Executors

object FileRDDReader {
  def read[K: AvroRecordCodec: Boundable, V: AvroRecordCodec](
    keyPath: Long => String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None,
    threads: Int = ConfigFactory.load().getInt("geotrellis.file.threads.rdd.read")
  )(implicit sc: SparkContext): RDD[(K, V)] = {
    if(queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]
    
    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(sc.defaultParallelism))

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
        val pool = Executors.newFixedThreadPool(threads)

        val result: Iterator[Vector[(K, V)]] = partition map { seq =>
          val range: Process[Task, Iterator[Long]] = Process.unfold(seq.toIterator) { iter =>
            if (iter.hasNext) {
              val (start, end) = iter.next()
              Some((start to end).toIterator, iter)
            } else None
          }

          val read: Iterator[Long] => Process[Task, Vector[(K, V)]] = { iterator =>
            Process.unfold(iterator) { iter =>
              if (iter.hasNext) {
                val index = iter.next()
                val path = keyPath(index)
                if(new File(path).exists) {
                  val bytes: Array[Byte] = Filesystem.slurp(path)
                  val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
                  if (filterIndexOnly) Some(recs, iter)
                  else Some(recs.filter { row => includeKey(row._1) }, iter)
                } else Some(Vector.empty, iter)
              } else None
            }
          }

          nondeterminism.njoin(maxOpen = threads, maxQueued = threads) { range map read }(Strategy.Executor(pool)).runFoldMap(identity).unsafePerformSync
        }

        /** Close partition pool */
        (result ++ Iterator({ pool.shutdown(); Vector.empty[(K, V)] })).flatten
      }
  }
}