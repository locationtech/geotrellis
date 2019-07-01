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

package geotrellis.spark.store.s3.slippy

import geotrellis.spark.store.slippy._
import geotrellis.layer.SpatialKey
import geotrellis.store.s3._

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, ListObjectsV2Request, S3Object}
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters._
import java.io.File

class S3SlippyTileReader[T](
  uri: String,
  s3Client: => S3Client = S3ClientProducer.get()
)(fromBytes: (SpatialKey, Array[Byte]) => T) extends SlippyTileReader[T] {
  import SlippyTileReader.TilePath

  val parsed = new java.net.URI(uri)
  val bucket = parsed.getHost
  val prefix = {
    val path = parsed.getPath
    path.substring(1, path.length)
  }

  def read(zoom: Int, spatialkey: SpatialKey): T = {
    val s3key = new File(prefix, s"$zoom/${spatialkey.col}/${spatialkey.row}").getPath

    val listRequest = ListObjectsV2Request.builder()
      .bucket(bucket)
      .prefix(s3key)
      .build()

    val objectList: List[S3Object] = s3Client.listObjectsV2(listRequest)
      .contents
      .asScala
      .toList

    objectList match {
      case List() => sys.error(s"KeyNotFound: $s3key not found in bucket $bucket")
      case List(s3obj) =>
        val getRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(s3obj.key)
          .build()
        val s3objStream = s3Client.getObject(getRequest)
        val bytes = IOUtils.toByteArray(s3objStream)
        val t = fromBytes(spatialkey, bytes)
        s3objStream.close()
        t
      case _ => sys.error(s"Multiple keys found for prefix $s3key in bucket $bucket")
    }
  }

  def read(zoom: Int)(implicit sc: SparkContext): RDD[(SpatialKey, T)] = {
    val s3keys = {
      val listRequest = ListObjectsV2Request.builder()
        .bucket(bucket)
        .prefix(new File(prefix, zoom.toString).getPath)
        .build()
      s3Client.listObjectsV2Paginator(listRequest)
        .contents
        .asScala
        .flatMap { s3obj =>
          s3obj.key match {
            case TilePath(x, y) => Some((SpatialKey(x.toInt, y.toInt), s3obj.key))
            case _ => None
          }
        }
        .toSeq
    }

    val numPartitions = math.min(s3keys.size, math.max(s3keys.size / 10, 50)).toInt
    sc.parallelize(s3keys)
      .partitionBy(new HashPartitioner(numPartitions))
      .mapPartitions({ partition =>
        val s3Client = this.s3Client

        partition.map { case (spatialKey, s3Key) =>
          val getRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .build()
          val s3obj = s3Client.getObject(getRequest)
          val bytes = IOUtils.toByteArray(s3obj)
          val t = fromBytes(spatialKey, bytes)
          s3obj.close()
          (spatialKey, t)
        }
      }, preservesPartitioning = true)
  }
}
