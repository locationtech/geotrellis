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

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.GeoTiffInfo
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.util.LazyLogging
import geotrellis.vector.Geometry

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.mutable

private [geotrellis] trait GeoTiffInfoReader extends LazyLogging {
  val geoTiffInfo: List[(String, GeoTiffInfo)]
  def geoTiffInfoRdd(implicit sc: SparkContext): RDD[String]
  def getGeoTiffInfo(uri: String): GeoTiffInfo
  def getGeoTiffTags(uri: String): TiffTags

  /**
    * Generates and partitions windows for GeoTiff based on desired window size
    * and query geometry. Partitioning is determined by total window sizes per partition.
    *
    * @param info.
    * @param partitionBytes  The desired number of bytes per partition.
    * @param maxSize         The maximum (linear) size of any window (any GridBounds)
    */
  def windowsByPartition(
    info: GeoTiffInfo,
    maxSize: Int,
    partitionBytes: Long,
    geometry: Option[Geometry]
  ): Array[Array[GridBounds]] = {
    val windows =
      geometry match {
        case Some(geometry) =>
          info.segmentLayout.listWindows(maxSize, info.extent, geometry)
        case None =>
          info.segmentLayout.listWindows(maxSize)
      }

    info.segmentLayout.partitionWindowsBySegments(windows, partitionBytes / info.cellType.bytes)
  }
}
