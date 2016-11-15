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

package geotrellis.spark.io.slippy

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.s3._
import geotrellis.util.Filesystem

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter._
import org.apache.spark._
import org.apache.spark.rdd._
import java.io.File


class S3SlippyTileReader[T](uri: String)(fromBytes: (SpatialKey, Array[Byte]) => T) extends SlippyTileReader[T] {
  import SlippyTileReader.TilePath

  val client = S3Client.DEFAULT
  val getClient = () => S3Client.DEFAULT

  val parsed = new java.net.URI(uri)
  val bucket = parsed.getHost
  val prefix = {
    val path = parsed.getPath
    path.substring(1, path.length)
  }

  def read(zoom: Int, key: SpatialKey): T = {
    val s3key = new File(prefix, s"$zoom/${key.col}/${key.row}").getPath

    client.listKeys(bucket, s3key) match {
      case Seq() => sys.error(s"KeyNotFound: $s3key not found in bucket $bucket")
      case Seq(tileKey) => fromBytes(key, client.readBytes(bucket, tileKey))
      case _ => sys.error(s"Multiple keys found for prefix $s3key in bucket $bucket")
    }
  }

  def read(zoom: Int)(implicit sc: SparkContext): RDD[(SpatialKey, T)] = {
    val keys = {
      client.listKeys(bucket, new File(prefix, zoom.toString).getPath)
        .map { key =>
          key match {
            case TilePath(x, y) => Some((SpatialKey(x.toInt, y.toInt), key))
            case _ => None
          }
        }
        .flatten
        .toSeq
    }

    val numPartitions = math.min(keys.size, math.max(keys.size / 10, 50)).toInt
    sc.parallelize(keys)
      .partitionBy(new HashPartitioner(numPartitions))
      .mapPartitions({ partition =>
        val client = getClient()

        partition.map { case (spatialKey, s3Key) =>

          (spatialKey, fromBytes(spatialKey, client.readBytes(bucket, s3Key)))
        }
      }, preservesPartitioning = true)
  }
}
