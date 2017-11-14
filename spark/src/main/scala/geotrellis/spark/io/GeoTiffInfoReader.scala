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

  lazy val averagePixelSize: Option[Int] =
    if(geoTiffInfo.nonEmpty) {
      Some((geoTiffInfo.map(_._2.bandType.bytesPerSample.toLong).sum / geoTiffInfo.length).toInt)
    } else {
      logger.error(s"No tiff tags found.")
      None
    }

  def windowsCount(maxTileSize: Option[Int] = None): Int =
    geoTiffInfo
      .flatMap { case (_, info) =>
        RasterReader.listWindows(info.segmentLayout.totalCols, info.segmentLayout.totalRows, maxTileSize)
      }
      .length

  def estimatePartitionsNumber(partitionBytes: Long, maxTileSize: Option[Int] = None): Option[Int] = {
    (maxTileSize, averagePixelSize) match {
      case (Some(tileSize), Some(pixelSize)) =>
        val numPartitions = (tileSize * pixelSize * windowsCount(maxTileSize) / partitionBytes).toInt
        logger.info(s"Estimated partitions number: $numPartitions")
        if (numPartitions > 0) Some(numPartitions)
        else None
      case _ =>
        logger.error("Can't estimate partitions number")
        None
    }
  }

  /**
    * Generate an RDD of URI, GridBounds pairs.  The URIs point to
    * files and the GridBounds conform to GeoTiff segments (if
    * possible).
    *
    * @param  partitionBytes  The desired number of bytes per partition.
    * @param  maxSize         The maximum (linear) size of any window (any GridBounds)
    */
  def windowsByBytes(
    partitionBytes: Long,
    maxSize: Int,
    geometry: Option[Geometry]
  )(implicit sc: SparkContext): RDD[(String, Array[GridBounds])] = {
    geoTiffInfoRdd.flatMap({ uri =>
      val md = getGeoTiffInfo(uri)
      val cols = md.segmentLayout.totalCols
      val rows = md.segmentLayout.totalRows
      val segCols = md.segmentLayout.tileLayout.tileCols
      val segRows = md.segmentLayout.tileLayout.tileRows
      val cellType = md.cellType

      val fileWindows =
        geometry match {
          case Some(geometry) =>
            val tags = getGeoTiffTags(uri)
            val extent = tags.extent
            RasterReader.listWindows(cols, rows, maxSize, extent, segCols, segRows, geometry)
          case None =>
            RasterReader.listWindows(cols, rows, maxSize, segCols, segRows)
        }

      val partitions = md.segmentLayout.partitionWindowsBySegments(fileWindows, partitionBytes / cellType.bytes)
      partitions.map({ windows => (uri, windows)})
    })
  }
}
