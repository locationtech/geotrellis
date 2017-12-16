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
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.typesafe.config.ConfigFactory
import geotrellis.raster._
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.s3._
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect._

/**
  * Generate VRTs to use from GDAL
  * */
trait S3COGCollectionReader[V <: CellGrid] extends Serializable {
  @transient lazy val DefaultThreadCount: Int =
    ConfigFactory.load().getThreads("geotrellis.s3.threads.rdd.read")

  implicit val tileMergeMethods: V => TileMergeMethods[V]
  implicit val tilePrototypeMethods: V => TilePrototypeMethods[V]

  def getS3Client: () => S3Client

  def readTiff(uri: URI, index: Int): GeoTiff[V]

  def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds

  def tileTiff[K](tiff: GeoTiff[V], gridBounds: Map[GridBounds, K]): Vector[(K, V)]

  def read[K: SpatialComponent: Boundable: ClassTag](
    bucket: String,
    keyPath: BigInt => String,
    baseQueryKeyBounds: Seq[KeyBounds[K]],
    realQueryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    sourceLayout: LayoutDefinition,
    overviewIndex: Int,
    threads: Int = DefaultThreadCount
  ): Seq[(K, V)] = {
    if (baseQueryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (baseQueryKeyBounds.length > 1)
      MergeQueue(baseQueryKeyBounds.flatMap(decomposeBounds))
    else
      baseQueryKeyBounds.flatMap(decomposeBounds)

    val realQueryKeyBoundsRange: Vector[K] =
      realQueryKeyBounds
        .flatMap { case KeyBounds(minKey, maxKey) =>
          val SpatialKey(minCol, minRow) = minKey.getComponent[SpatialKey]
          val SpatialKey(maxCol, maxRow) = maxKey.getComponent[SpatialKey]

          for {
            c <- minCol to maxCol
            r <- minRow to maxRow
          } yield minKey.setComponent[SpatialKey](SpatialKey(c, r))
        }
        .toVector


    LayerReader.njoin[K, V](ranges.toIterator, threads) { index: BigInt =>
      try {
        val uri = new URI(s"s3://$bucket/${keyPath(index)}.tiff")
        val tiff = readTiff(uri, overviewIndex)
        val rgb = sourceLayout.mapTransform(tiff.extent)

        val gb = tiff.rasterExtent.gridBounds
        val getGridBounds = getSegmentGridBounds(uri, overviewIndex)

        val map: Map[GridBounds, K] =
          realQueryKeyBoundsRange.flatMap { key =>
            val spatialKey = key.getComponent[SpatialKey]
            val minCol = (spatialKey.col - rgb.colMin) * sourceLayout.tileLayout.tileCols
            val minRow = (spatialKey.row - rgb.rowMin) * sourceLayout.tileLayout.tileRows

            if (minCol >= 0 && minRow >= 0 && minCol < tiff.cols && minRow < tiff.rows) {
              val currentGb = getGridBounds(minCol, minRow)
              gb.intersection(currentGb).map(gb => gb -> key)
            } else None
          }.toMap

        tileTiff(tiff, map)
      } catch {
        case e: AmazonS3Exception if e.getStatusCode == 404 => Vector.empty
      }
    }
    .groupBy(_._1)
    .map { case (key, (seq: Iterable[(K, V)])) => key -> seq.map(_._2).reduce(_ merge _) }
    .toSeq
  }
}

object S3COGCollectionReader {
  /** TODO: Think about it in a more generic fasion */
  private final val S3COGCollectionReaderRegistry: Map[String, S3COGCollectionReader[_]] =
    Map(
      classTag[Tile].runtimeClass.getCanonicalName -> implicitly[S3COGCollectionReader[Tile]],
      classTag[MultibandTile].runtimeClass.getCanonicalName -> implicitly[S3COGCollectionReader[MultibandTile]]
    )

  private [geotrellis] def fromRegistry[V <: CellGrid: ClassTag]: S3COGCollectionReader[V] =
    S3COGCollectionReaderRegistry
      .getOrElse(
        classTag[V].runtimeClass.getCanonicalName,
        throw new Exception(s"No S3COGCollectionReaderRegistry for the type ${classTag[V].runtimeClass.getCanonicalName}")
      ).asInstanceOf[S3COGCollectionReader[V]]

  def getReaders(uri: URI, getS3Client: () => S3Client = () => S3Client.DEFAULT): (ByteReader, Option[ByteReader]) = {
    val auri = new AmazonS3URI(uri)
    val (bucket, key) = auri.getBucket -> auri.getKey
    val ovrKey = s"$key.ovr"
    val ovrReader: Option[ByteReader] =
      if (getS3Client().doesObjectExist(bucket, ovrKey)) Some(S3RangeReader(bucket, ovrKey, getS3Client())) else None

    val reader =
      StreamingByteReader(
        S3RangeReader(
          bucket = bucket,
          key = key,
          client = getS3Client()
        )
      )

    reader -> ovrReader
  }
}

