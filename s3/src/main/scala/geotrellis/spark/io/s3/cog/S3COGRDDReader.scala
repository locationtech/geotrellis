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

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import com.typesafe.config.ConfigFactory
import com.amazonaws.services.s3.AmazonS3URI
import java.net.URI

import scala.reflect._

/**
  * Generate VRTs to use from GDAL
  * */
trait S3COGRDDReader[V <: CellGrid] extends Serializable {
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
    keyPath: Long => String,
    baseQueryKeyBounds: Seq[KeyBounds[K]],
    realQueryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    sourceLayout: LayoutDefinition,
    overviewIndex: Int,
    numPartitions: Option[Int] = None,
    threads: Int = DefaultThreadCount
  )(implicit sc: SparkContext): RDD[(K, V)] = {
    if (baseQueryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val ranges = if (baseQueryKeyBounds.length > 1)
      MergeQueue(baseQueryKeyBounds.flatMap(decomposeBounds))
    else
      baseQueryKeyBounds.flatMap(decomposeBounds)

    // should query by a base layer
    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(sc.defaultParallelism))

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

    val queryGb: GridBounds = realQueryKeyBounds.reduce(_ combine _).toGridBounds

    val t1 = sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
        partition flatMap { seq =>
          LayerReader.njoin[K, V](seq.toIterator, threads) { index: Long =>
            if(index == 0) Vector()
            else { try {

              println(s"index!: $index")

              val uri = new URI(s"s3://$bucket/${keyPath(index)}.tiff")
              val tiff = readTiff(uri, overviewIndex)
              val gb = tiff.rasterExtent.gridBounds
              val getGridBounds = getSegmentGridBounds(uri, overviewIndex)

              val map: Map[GridBounds, K] =
                realQueryKeyBoundsRange.flatMap { key =>
                  val spatialKey = key.getComponent[SpatialKey]
                  val minCol = (spatialKey.col - queryGb.colMin) * sourceLayout.tileLayout.tileCols
                  val minRow = (spatialKey.row - queryGb.rowMin) * sourceLayout.tileLayout.tileRows

                  val currentGb = getGridBounds(minCol, minRow)
                  gb.intersection(currentGb).map(gb => gb -> key)
                }.toMap

              tileTiff(tiff, map)

              /*realQueryKeyBoundsRange.flatMap { key =>
                val spatialKey = key.getComponent[SpatialKey]
                val minCol = (spatialKey.col - queryGb.colMin) * sourceLayout.tileLayout.tileCols
                val minRow = (spatialKey.row - queryGb.rowMin) * sourceLayout.tileLayout.tileRows

                val currentGb = getGridBounds(minCol, minRow)
                gb.intersection(currentGb).map {
                  case GridBounds(minCol, minRow, maxCol, maxRow) =>
                    key -> tiff.crop(minCol, minRow, maxCol, maxRow).tile
                }
              }*/
            } catch {
              case e: AmazonS3Exception if e.getStatusCode == 404 => Vector.empty
            } }
          }
        }
      }

    val t2 =
      t1
        .groupBy(_._1)
        .map { case (key, (seq: Iterable[(K, V)])) => key -> seq.map(_._2).reduce(_ merge _) }
    t2
  }
}

object S3COGRDDReader {
  /** TODO: Think about it in a more generic fasion */
  private final val S3COGRDDReaderRegistry: Map[String, S3COGRDDReader[_]] =
    Map(
      classTag[Tile].runtimeClass.getCanonicalName -> implicitly[S3COGRDDReader[Tile]],
      classTag[MultibandTile].runtimeClass.getCanonicalName -> implicitly[S3COGRDDReader[MultibandTile]]
    )

  private [geotrellis] def fromRegistry[V <: CellGrid: ClassTag]: S3COGRDDReader[V] =
    S3COGRDDReaderRegistry
      .getOrElse(
        classTag[V].runtimeClass.getCanonicalName,
        throw new Exception(s"No S3COGRDDReaderRegistry for the type ${classTag[V].runtimeClass.getCanonicalName}")
      ).asInstanceOf[S3COGRDDReader[V]]

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
