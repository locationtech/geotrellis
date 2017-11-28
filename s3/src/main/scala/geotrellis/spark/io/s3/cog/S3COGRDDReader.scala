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
import geotrellis.spark.crop._
import geotrellis.raster.crop._
import geotrellis.raster.crop.Crop.{Options => CropOptions}
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
import geotrellis.raster.{CellGrid, CellSize, GridBounds, GridExtent, RasterExtent, Tile, TileLayout}
import geotrellis.raster.io.geotiff.{Auto, BandInterleave, GeoTiff, GeoTiffSegmentLayout, GeoTiffTile, PixelInterleave, SinglebandGeoTiff, Tiled}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.{geoTiffSinglebandTile, readGeoTiffInfo}
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._
import geotrellis.vector.Extent
import spire.syntax.cfor

import scala.collection.immutable


trait S3COGRDDReader[V <: CellGrid] extends Serializable {
  @transient lazy val DefaultThreadCount =
    ConfigFactory.load().getThreads("geotrellis.s3.threads.rdd.read")

  def getS3Client: () => S3Client

  def readTiff(uri: URI): GeoTiff[V]

  def getSegmentGridBounds(uri: URI, index: Int): (Int, Int) => GridBounds

  def read[K: SpatialComponent: Boundable](
    bucket: String,
    transformKey: Long => Long, // transforms query key to filename key
    keyPath: Long => String,
    indexToKey: Long => K,
    keyToExtent: K => Extent,
    keyBoundsToExtent: KeyBounds[K] => Extent,
    queryKeyBounds: Seq[KeyBounds[K]],
    realQueryKeyBounds: Seq[KeyBounds[K]],
    baseQueryKeyBounds: Seq[(KeyBounds[K], Seq[(KeyBounds[K], KeyBounds[K])])],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    gbToKey: (GridBounds/*, K*/) => K,
    gbToGb: GridBounds => GridBounds,
    sourceLayout: LayoutDefinition,
    filterIndexOnly: Boolean,
    overviewIndex: Int,
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

    val realQueryKeyBoundsRange: Seq[SpatialKey] = {
      realQueryKeyBounds.flatMap { case KeyBounds(minKey, maxKey) =>
        val SpatialKey(minc, minr) = minKey.getComponent[SpatialKey]
        val SpatialKey(maxc, maxr) = maxKey.getComponent[SpatialKey]

        for { c <- minc to maxc; r <- minr to maxr } yield SpatialKey(c, r)
      }
    }

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
        partition flatMap { seq =>
          LayerReader.njoin[K, V](seq.toIterator, threads){ index: Long =>
            try {
              val uri = new URI(s"s3://$bucket/${keyPath(index)}.tiff")
              println(uri)
              println(s"overviewIndex: $overviewIndex")
              val tiff =
                if(overviewIndex < 0) readTiff(uri)
                else readTiff(uri).getOverview(overviewIndex)

              val extent = tiff.extent

              val gb = tiff.rasterExtent.gridBounds

              val getGridBounds = getSegmentGridBounds(uri, overviewIndex)

              val ld = LayoutDefinition(GridExtent(tiff.extent, tiff.cellSize), 1)

              def keyTtExt(key: K): Extent = {
                sourceLayout.mapTransform(key)
              }

              val res: GridBounds = ld.mapTransform(extent)


              val gbkkkb = realQueryKeyBounds.reduce(_ combine _).toGridBounds

              realQueryKeyBoundsRange.toVector.sortBy(k => (k._2 -> k._1)).flatMap { spatialKey =>
                println(s"spatialKey: $spatialKey")

                val minCol = (spatialKey.col - gbkkkb.colMin) * sourceLayout.tileLayout.tileCols
                val minRow = (spatialKey.row - gbkkkb.rowMin) * sourceLayout.tileLayout.tileRows

                val currentGb = getGridBounds(minCol, minRow)

                println(s"currentGb: ${currentGb}")

                gb.intersection(currentGb).map {
                  case lol @ GridBounds(mic, mir, maxc, maxr) => {

                    println(s"lol: $lol")
                    println(s"gbToKey(lol): ${gbToKey(lol)}")
                    println(s"spatialKey: ${spatialKey}")

                    spatialKey.asInstanceOf[K] ->
                      tiff.crop(
                        mic,
                        mir,
                        maxc,
                        maxr
                      ).tile
                  }
                }


                /*if(minCol >= 0 && minCol <= tiff.cols && minRow >= 0 && minRow <= tiff.rows &&
                   maxCol >= 0 && maxCol <= tiff.cols && maxRow >= 0 && maxRow <= tiff.rows) {
                  Some(spatialKey.asInstanceOf[K] ->
                    tiff.crop(
                      math.min(minCol, maxCol),
                      math.min(minRow, maxRow),
                      math.max(maxCol, minCol),
                      math.max(maxRow, minRow)
                    ).tile
                  )
                } else None*/

                /*val ee = sourceLayout.mapTransform(spatialKey)
                if(extent.contains(ee)) {
                  Some(spatialKey.asInstanceOf[K] ->
                    tiff
                      .crop(ee).tile
                  )
                } else None*/
              }

              /*realQueryKeyBounds.toVector.flatMap { kb =>
                println(s"kb: $kb")
                println(s"kb.toGridBounds(): ${kb.toGridBounds()}")

                val KeyBounds(minKey, maxKey) = kb
                val cextent: Extent = sourceLayout.mapTransform(minKey) combine sourceLayout.mapTransform(maxKey)

                println(s"extent: $extent")
                println(s"cextent: $cextent")

                val res: GridBounds = ld.mapTransform(cextent)


                println(s"gb: ${gb}")
                println(s"res: ${res}")

                println(s"sourceLayout.mapTransform(kb.toGridBounds): ${sourceLayout.mapTransform(kb.toGridBounds)}")
                println(s"sourceLayout.mapTransform(sourceLayout.mapTransform(kb.toGridBounds).center): ${sourceLayout.mapTransform(sourceLayout.mapTransform(kb.toGridBounds).center)}")

                extent.intersection(sourceLayout.mapTransform(kb.toGridBounds))
                if(true)
                  Some(gbToKey(gb) ->
                    tiff
                      .crop(sourceLayout.mapTransform(gb)).tile
                  )
                else None
              }*/
            } catch {
              case e: AmazonS3Exception if e.getStatusCode == 404 => Vector.empty
            }
          }
        }
      }

    /*sc.parallelize(bins, bins.size)
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
      }*/
  }
}

object S3COGRDDReader {
  def getReaders(s3Client: S3Client, uri: URI): (ByteReader, Option[ByteReader]) = {
    val auri = new AmazonS3URI(uri)
    val (bucket, key) = auri.getBucket -> auri.getKey
    val ovrKey = s"$key.ovr"
    val ovrReader: Option[ByteReader] =
      if (s3Client.doesObjectExist(bucket, ovrKey)) Some(S3RangeReader(bucket, ovrKey, s3Client)) else None
  }

}

