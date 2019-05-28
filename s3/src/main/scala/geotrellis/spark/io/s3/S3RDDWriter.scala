/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.s3

import geotrellis.spark.io._
import geotrellis.layers.io.avro._
import geotrellis.layers.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.util.KryoWrapper
import geotrellis.spark.io.s3.conf.S3Config

import cats.effect.{IO, Timer}
import cats.syntax.apply._
import com.amazonaws.services.s3.model.{AmazonS3Exception, ObjectMetadata, PutObjectRequest, PutObjectResult}
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import org.apache.spark.rdd.RDD

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext
import scala.reflect._

trait S3RDDWriter {
  final val defaultThreadCount = S3Config.threads.rdd.writeThreads

  def getS3Client: () => S3Client

  def write[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    bucket: String,
    keyPath: K => String,
    putObjectModifier: PutObjectRequest => PutObjectRequest = { p => p },
    threads: Int = defaultThreadCount
  ): Unit = {
    update(rdd, bucket, keyPath, None, None, putObjectModifier, threads)
  }

  private[s3] def update[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    bucket: String,
    keyPath: K => String,
    writerSchema: Option[Schema],
    mergeFunc: Option[(V, V) => V],
    putObjectModifier: PutObjectRequest => PutObjectRequest = { p => p },
    threads: Int = defaultThreadCount
  ): Unit = {
    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    implicit val sc = rdd.sparkContext

    val _getS3Client = getS3Client
    val _codec = codec

    val pathsToTiles =
      // Call groupBy with numPartitions; if called without that argument or a partitioner,
      // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
      // on a key type that may no longer by valid for the key type of the resulting RDD.
      rdd.groupBy({ row => keyPath(row._1) }, numPartitions = rdd.partitions.length)

    val _recordCodec = KeyValueRecordCodec[K, V]
    val kwWriterSchema = KryoWrapper(writerSchema)

    pathsToTiles.foreachPartition { partition: Iterator[(String, Iterable[(K, V)])] =>
      if(partition.nonEmpty) {
        import geotrellis.layers.utils.TaskUtils._
        val getS3Client = _getS3Client
        val s3client: S3Client = getS3Client()
        val schema = kwWriterSchema.value.getOrElse(_recordCodec.schema)

        val pool = Executors.newFixedThreadPool(threads)
        implicit val ec = ExecutionContext.fromExecutor(pool)
        implicit val timer: Timer[IO] = IO.timer(ec)
        implicit val cs = IO.contextShift(ec)

        val rows: fs2.Stream[IO, (String, Vector[(K, V)])] =
          fs2.Stream.fromIterator[IO, (String, Vector[(K, V)])](
            partition.map { case (key, value) => (key, value.toVector) }
          )

        def elaborateRow(row: (String, Vector[(K,V)])): fs2.Stream[IO, (String, Vector[(K,V)])] = {
          fs2.Stream eval IO.shift(ec) *> IO ({
            val (key, current) = row
            val updated = LayerWriter.updateRecords(mergeFunc, current, existing = {
              try {
                val bytes = IOUtils.toByteArray(s3client.getObject(bucket, key).getObjectContent)
                AvroEncoder.fromBinary(schema, bytes)(_recordCodec)
              } catch {
                case e: AmazonS3Exception if e.getStatusCode == 404 => Vector.empty
              }
            })
            (key, updated)
          })
        }

        def rowToRequest(row: (String, Vector[(K,V)])): fs2.Stream[IO, PutObjectRequest] = {
          fs2.Stream eval IO.shift(ec) *> IO ({
            val (key, kvs) = row
            val bytes = AvroEncoder.toBinary(kvs)(_codec)
            val metadata = new ObjectMetadata()
            metadata.setContentLength(bytes.length)
            val is = new ByteArrayInputStream(bytes)
            putObjectModifier(new PutObjectRequest(bucket, key, is, metadata))
          })
        }

        def retire(request: PutObjectRequest): fs2.Stream[IO, PutObjectResult] = {
          fs2.Stream eval IO.shift(ec) *> IO ({
            request.getInputStream.reset() // reset in case of retransmission to avoid 400 error
            s3client.putObject(request)
          }).retryEBO {
            case e: AmazonS3Exception if e.getStatusCode == 503 => true
            case _ => false
          }
        }

        rows
          .flatMap(elaborateRow)
          .flatMap(rowToRequest)
          .map(retire)
          .parJoin(threads)
          .compile
          .drain
          .unsafeRunSync()

        pool.shutdown()
      }
    }
  }
}

object S3RDDWriter extends S3RDDWriter {
  def getS3Client: () => S3Client = () => S3Client.DEFAULT
}
