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

package geotrellis.spark.io

import geotrellis.raster.io.geotiff.reader.GeoTiffReader.GeoTiffInfo
import geotrellis.vector.Geometry
import geotrellis.raster.GridBounds
import geotrellis.raster.io.geotiff.GeoTiffSegmentLayoutTransform

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import java.net.URI

private [geotrellis] trait GeoTiffInfoReader extends LazyLogging {
  def geoTiffInfoRDD(implicit sc: SparkContext): RDD[String]
  def getGeoTiffInfo(uri: String): GeoTiffInfo

  def getSegmentLayoutTransform(geoTiffInfo: GeoTiffInfo): GeoTiffSegmentLayoutTransform =
    GeoTiffSegmentLayoutTransform(geoTiffInfo.segmentLayout, geoTiffInfo.bandCount)

  /**
    * Generates and partitions windows for GeoTiff based on desired window size
    * and query geometry. Partitioning is determined by total window sizes per partition.
    *
    * @param info.
    * @param partitionBytes  The desired number of bytes per partition.
    * @param maxSize         The maximum (linear) size of any window (any GridBounds)
    */
  private def windowsByPartition(
    info: GeoTiffInfo,
    maxSize: Int,
    partitionBytes: Long,
    geometry: Option[Geometry]
  ): Array[Array[GridBounds[Int]]] = {
    val windows =
      geometry match {
        case Some(geometry) =>
          info.segmentLayout.listWindows(maxSize, info.extent, geometry)
        case None =>
          info.segmentLayout.listWindows(maxSize)
      }

    info.segmentLayout.partitionWindowsBySegments(windows, partitionBytes / math.max(info.cellType.bytes, 1))
  }

  def readWindows[O, I, K, V](
    files: RDD[URI],
    uriToKey: (URI, I) => K,
    maxSize: Int,
    partitionBytes: Long,
    rasterReaderOptions: O,
    geometry: Option[Geometry]
  )(
    implicit sc: SparkContext, rr: RasterReader[O, (I, V)]
  ): RDD[(K, V)] = {
    val windows: RDD[(URI, Array[GridBounds[Int]])] =
      files.flatMap({ uri =>
        windowsByPartition(
          info = getGeoTiffInfo(uri.toString),
          maxSize = maxSize,
          partitionBytes = partitionBytes,
          geometry = geometry
        ).map { windows => (uri, windows) }
      })

    windows.persist()

    val repartition = {
      val windowCount = windows.count.toInt
      if (windowCount > windows.partitions.length) {
        logger.info(s"Repartition into ${windowCount} partitions.")
        windows.repartition(windowCount)
      }
      else windows
    }

    val result = repartition.flatMap { case (uri, windows) =>
      val info = getGeoTiffInfo(uri.toString)
      rr.readWindows(windows, info, rasterReaderOptions).map { case (k, v) =>
        uriToKey(uri, k) -> v
      }
    }

    windows.unpersist()
    result
  }
}

