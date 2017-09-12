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
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.util.KryoWrapper

import com.amazonaws.services.s3.model.{AmazonS3Exception, ObjectMetadata, PutObjectRequest, PutObjectResult}

import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import org.apache.spark.rdd.RDD

import com.typesafe.config.ConfigFactory

import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

import scala.reflect._


trait S3RDDWriter {

  def getS3Client: () => S3Client

  def write[K: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    rdd: RDD[(K, V)],
    bucket: String,
    keyPath: K => String,
    putObjectModifier: PutObjectRequest => PutObjectRequest = { p => p },
    threads: Int = ConfigFactory.load().getThreads("geotrellis.s3.threads.rdd.write")
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
    threads: Int = ConfigFactory.load().getThreads("geotrellis.s3.threads.rdd.write")
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

    pathsToTiles.foreachPartition { partition =>
      if(partition.nonEmpty) {
        import geotrellis.spark.util.TaskUtils._
        val getS3Client = _getS3Client
        val s3client: S3Client = getS3Client()
        val schema = kwWriterSchema.value.getOrElse(_recordCodec.schema)

        val requests: Process[Task, PutObjectRequest] =
          Process.unfold(partition) { iter =>
            if (iter.hasNext) {
              val recs = iter.next()
              val key = recs._1
              val rows1: Vector[(K,V)] = recs._2.toVector
              val rows2: Vector[(K,V)] =
                if (mergeFunc != None) {
                  try {
                    val bytes = IOUtils.toByteArray(s3client.getObject(bucket, key).getObjectContent)
                    AvroEncoder.fromBinary(schema, bytes)(_recordCodec)
                  } catch {
                    case e: AmazonS3Exception if e.getStatusCode == 404 => Vector.empty
                  }
                }
                else Vector.empty
              val outRows: Vector[(K, V)] =
                mergeFunc match {
                  case Some(fn) =>
                    (rows2 ++ rows1)
                      .groupBy({ case (k,v) => k })
                      .map({ case (k, kvs) =>
                        val vs = kvs.map({ case (k,v) => v }).toSeq
                        val v: V = vs.tail.foldLeft(vs.head)(fn)
                        (k, v) })
                      .toVector
                  case None => rows1
                }

              val bytes = AvroEncoder.toBinary(outRows)(_codec)
              val metadata = new ObjectMetadata()
              metadata.setContentLength(bytes.length)
              val is = new ByteArrayInputStream(bytes)
              val request = putObjectModifier(new PutObjectRequest(bucket, key, is, metadata))
              Some(request, iter)
            } else {
              None
            }
          }

        val pool = Executors.newFixedThreadPool(threads)

        val write: PutObjectRequest => Process[Task, PutObjectResult] = { request =>
          Process eval Task {
            request.getInputStream.reset() // reset in case of retransmission to avoid 400 error
            s3client.putObject(request)
          }(pool).retryEBO {
            case e: AmazonS3Exception if e.getStatusCode == 503 => true
            case _ => false
          }
        }

        val results = nondeterminism.njoin(maxOpen = threads, maxQueued = threads) { requests map write }(Strategy.Executor(pool))
        results.run.unsafePerformSync
        pool.shutdown()
      }
    }
  }
}

object S3RDDWriter extends S3RDDWriter {
  def getS3Client: () => S3Client = () => S3Client.DEFAULT
}
