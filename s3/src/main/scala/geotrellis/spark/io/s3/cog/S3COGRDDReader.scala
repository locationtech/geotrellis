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

package geotrellis.spark.io.s3.cog

import java.net.URI

import com.amazonaws.services.s3.AmazonS3URI
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.util.KryoWrapper

import scalaz.concurrent.{Strategy, Task}
import scalaz.std.vector._
import scalaz.stream.{Process, nondeterminism}
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import com.typesafe.config.ConfigFactory
import geotrellis.raster.{CellSize, Tile}
import geotrellis.raster.io.geotiff.{Auto, GeoTiff, SinglebandGeoTiff}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util._
import geotrellis.vector.Extent
import spire.syntax.cfor


trait S3COGRDDReader[V] {
  final val DefaultThreadCount =
    ConfigFactory.load().getThreads("geotrellis.s3.threads.rdd.read")

  def getS3Client: () => S3Client

  def readTiff(uri: URI): GeoTiff[V]

  /*def readSingleband(uri: URI): SinglebandGeoTiff = {
    val auri = new AmazonS3URI(uri)
    val (bucket, key) = auri.getBucket -> auri.getKey
    val ovrKey = s"$key.ovr"
    val ovrReader: Option[ByteReader] =
      if (getS3Client().doesObjectExist(bucket, ovrKey)) Some(S3RangeReader(bucket, ovrKey, getS3Client())) else None

    GeoTiffReader
      .readSingleband(
        StreamingByteReader(
          S3RangeReader(
            bucket = bucket,
            key = key,
            client = getS3Client()
          )
        ),
        false,
        true,
        true,
        ovrReader
      )
  }*/

  def read[K: SpatialComponent: Boundable](
    bucket: String,
    transformKey: Long => Long, // transforms query key to filename key
    keyPath: Long => String,
    indexToKey: Long => K,
    keyToExtent: K => Extent,
    keyBoundsToExtent: KeyBounds[K] => Extent,
    queryKeyBounds: Seq[KeyBounds[K]],
    baseQueryKeyBounds: Seq[(KeyBounds[K], Seq[(KeyBounds[K], KeyBounds[K])])],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    cellSize: Option[CellSize] = None,
    numPartitions: Option[Int] = None,
    threads: Int = DefaultThreadCount
  )(implicit sc: SparkContext): RDD[(K, V)] = {
    if (queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    // should query by a base layer
    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(sc.defaultParallelism))

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
        partition flatMap { seq =>
          LayerReader.njoin[K, V](seq.toIterator, threads){ index: Long =>
            try {
              val tiff = readTiff(new URI(s"s3://$bucket/${keyPath(index)}.tif"))

              val key: K = ???

              queryKeyBounds.map { case KeyBounds(minKey, maxKey) =>


              }


              val tile =
                tiff.crop(
                  subExtent      = null /*keyBoundsToExtent(queryKb)*/,
                  cellSize       = cellSize.getOrElse(tiff.cellSize),
                  resampleMethod = NearestNeighbor,
                  strategy       = Auto(0)
                ).tile

              Vector(key -> tile)
            } catch {
              case e: AmazonS3Exception if e.getStatusCode == 404 => Vector.empty
            }
          }
        }
      }

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
        partition flatMap { seq =>
          LayerReader.njoin[K, V](seq.toIterator, threads){ index: Long =>
            try {
              val tiff = readTiff(new URI(s"s3://$bucket/${keyPath(transformKey(index))}.tif"))

              val key  = indexToKey(index)
              val tile =
                tiff.crop(
                  subExtent      = keyToExtent(key),
                  cellSize       = cellSize.getOrElse(tiff.cellSize),
                  resampleMethod = NearestNeighbor,
                  strategy       = Auto(0)
                ).tile

              Vector(key -> tile)
            } catch {
              case e: AmazonS3Exception if e.getStatusCode == 404 => Vector.empty
            }
          }
        }
      }
  }
}

