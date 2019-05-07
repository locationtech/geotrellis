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

import geotrellis.tiling.SpatialKey
import geotrellis.spark.io.s3.conf.S3Config
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest, PutObjectResult}
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.spark.rdd.RDD
import cats.effect.{IO, Timer}
import cats.syntax.apply._

import scala.concurrent.ExecutionContext
import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import java.net.URI

import geotrellis.layers.LayerId

object SaveToS3 {
  final val defaultThreadCount = S3Config.threads.rdd.writeThreads

  /**
    * @param id           A Layer ID
    * @param pathTemplate The template used to convert a Layer ID and a SpatialKey into an S3 URI
    * @return             A functon which takes a spatial key and returns an S3 URI
    */
  def spatialKeyToPath(id: LayerId, pathTemplate: String): (SpatialKey => String) = {
    // Return λ
    { key =>
      pathTemplate
        .replace("{x}", key.col.toString)
        .replace("{y}", key.row.toString)
        .replace("{z}", id.zoom.toString)
        .replace("{name}", id.name)
    }
  }

  /**
    * @param keyToUri  A function that maps each key to full s3 uri
    * @param rdd       An RDD of K, Byte-Array pairs (where the byte-arrays contains image data) to send to S3
    * @param putObjectModifier  Function that will be applied ot S3 PutObjectRequests, so that they can be modified (e.g. to change the ACL settings)
    * @param s3Maker   A function which returns an S3 Client (real or mock) into-which to save the data
    * @param threads   Number of threads dedicated for the IO
    */
  def apply[K](
    rdd: RDD[(K, Array[Byte])],
    keyToUri: K => String,
    putObjectModifier: PutObjectRequest => PutObjectRequest = { p => p },
    s3Maker: () => S3Client = () => S3Client.DEFAULT,
    threads: Int = defaultThreadCount
  ): Unit = {
    val keyToPrefix: K => (String, String) = key => {
      val uri = new URI(keyToUri(key))
      require(uri.getScheme == "s3", s"SaveToS3 only supports s3 scheme: $uri")
      val bucket = uri.getAuthority
      val prefix = uri.getPath.substring(1) // drop the leading / from the prefix
      (bucket, prefix)
    }

    rdd.foreachPartition { partition =>
      val s3Client = s3Maker()
      val requests: fs2.Stream[IO, PutObjectRequest] =
        fs2.Stream.fromIterator[IO, PutObjectRequest](
          partition.map { case (key, bytes) =>
            val metadata = new ObjectMetadata()
            metadata.setContentLength(bytes.length)
            val is = new ByteArrayInputStream(bytes)
            val (bucket, path) = keyToPrefix(key)
            putObjectModifier(new PutObjectRequest(bucket, path, is, metadata))
          }
        )

      val pool = Executors.newFixedThreadPool(threads)
      implicit val ec = ExecutionContext.fromExecutor(pool)
      implicit val timer: Timer[IO] = IO.timer(ec)
      implicit val cs = IO.contextShift(ec)

      import geotrellis.layers.utils.TaskUtils._
      val write: PutObjectRequest => fs2.Stream[IO, PutObjectResult] = { request =>
        fs2.Stream eval IO.shift(ec) *> IO {
          request.getInputStream.reset() // reset in case of retransmission to avoid 400 error
          s3Client.putObject(request)
        }.retryEBO {
          case e: AmazonS3Exception if e.getStatusCode == 503 => true
          case _ => false
        }
      }

      requests
        .map(write)
        .parJoin(threads)
        .compile
        .toVector
        .unsafeRunSync()
      pool.shutdown()
    }
  }
}
