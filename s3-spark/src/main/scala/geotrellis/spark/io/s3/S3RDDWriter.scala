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

package geotrellis.spark.store.s3

import geotrellis.spark.store._
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs.KeyValueRecordCodec
import geotrellis.store.s3._
import geotrellis.store.s3.conf.S3Config
import geotrellis.spark.util.KryoWrapper

import cats.effect.{IO, Timer}
import cats.syntax.apply._

import software.amazon.awssdk.services.s3.model.{S3Exception, PutObjectRequest, PutObjectResponse, GetObjectRequest}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.core.sync.RequestBody

import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import org.apache.spark.rdd.RDD

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext
import scala.reflect._

class S3RDDWriter(
  val getClient: () => S3Client = S3ClientProducer.get,
  val defaultThreadCount: Int = S3Config.threads.rdd.writeThreads
) {

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

    val _getClient = getClient
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
        import geotrellis.layers.util.IOUtils._
        val getClient = _getClient
        val s3Client: S3Client = getClient()
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
                val request = GetObjectRequest.builder()
                  .bucket(bucket)
                  .key(key)
                  .build()

                val is = s3Client.getObject(request)
                val bytes = IOUtils.toByteArray(is)
                AvroEncoder.fromBinary(schema, bytes)(_recordCodec)
              } catch {
                case e: S3Exception if e.statusCode == 404 => Vector.empty
              }
            })
            (key, updated)
          })
        }

        def rowToRequest(row: (String, Vector[(K,V)])): fs2.Stream[IO, (PutObjectRequest, RequestBody)] = {
          fs2.Stream eval IO.shift(ec) *> IO ({
            val (key, kvs) = row
            val contentBytes = AvroEncoder.toBinary(kvs)(_codec)
            val request = PutObjectRequest.builder()
              .bucket(bucket)
              .key(key)
              .contentLength(contentBytes.length)
              .build()
            val requestBody = RequestBody.fromBytes(contentBytes)

            (putObjectModifier(request), requestBody)
          })
        }

        def retire(request: PutObjectRequest, requestBody: RequestBody): fs2.Stream[IO, PutObjectResponse] =
          fs2.Stream eval IO.shift(ec) *> IO { s3Client.putObject(request, requestBody) }

        rows
          .flatMap(elaborateRow)
          .flatMap(rowToRequest)
          .map(Function.tupled(retire))
          .parJoin(threads)
          .compile
          .drain
          .unsafeRunSync()

        pool.shutdown()
      }
    }
  }
}
