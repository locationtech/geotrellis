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

package geotrellis.spark.streaming.io.hadoop

import geotrellis.raster._
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.io.RasterReader
import geotrellis.spark.io.hadoop.HadoopGeoTiffRDD
import geotrellis.vector._
import geotrellis.spark.io.hadoop.formats._

import org.apache.hadoop.conf.Configuration
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.hadoop.fs.Path
import org.apache.spark.streaming.dstream.DStream

import java.nio.ByteBuffer

trait HadoopStreamingContextMethods {
  import geotrellis.spark.io.hadoop.HadoopGeoTiffRDD.Options
  implicit val ssc: StreamingContext
  implicit val sc: SparkContext = ssc.sparkContext

  def hadoopGeoTiffDStream[K, V](
    path: Path,
    filter: Path => Boolean = (p: Path) => HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions.exists(p.toString.endsWith),
    newFilesOnly: Boolean = false,
    options: Options,
    conf: Configuration = sc.hadoopConfiguration
  )(implicit rr: RasterReader[HadoopGeoTiffRDD.Options, (ProjectedExtent, Tile)]): DStream[(ProjectedExtent, Tile)] = {
    ssc.fileStream[Path, Array[Byte], BytesFileInputFormat](
      directory    = path.toUri.toString,
      filter       = filter,
      newFilesOnly = newFilesOnly,
      conf         = conf
    ).transform {
      _.mapPartitions(
        _.map { case (_, bytes) => rr.readFully(ByteBuffer.wrap(bytes), options) },
        preservesPartitioning = true
      )
    }
  }

  def hadoopTemporalGeoTiffDStream(
    path: Path,
    filter: Path => Boolean = (p: Path) => HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions.exists(p.toString.endsWith),
    newFilesOnly: Boolean = false,
    options: Options,
    conf: Configuration = sc.hadoopConfiguration
  )(implicit rr: RasterReader[HadoopGeoTiffRDD.Options, (TemporalProjectedExtent, Tile)]): DStream[(TemporalProjectedExtent, Tile)] = {
    ssc.fileStream[Path, Array[Byte], BytesFileInputFormat](
      directory    = path.toUri.toString,
      filter       = filter,
      newFilesOnly = newFilesOnly,
      conf         = conf
    ).transform {
      _.mapPartitions(
        _.map { case (_, bytes) => rr.readFully(ByteBuffer.wrap(bytes), options) },
        preservesPartitioning = true
      )
    }
  }

  def hadoopMultibandGeoTiffDStream[K, V](
    path: Path,
    filter: Path => Boolean = (p: Path) => HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions.exists(p.toString.endsWith),
    newFilesOnly: Boolean = false,
    options: Options,
    conf: Configuration = sc.hadoopConfiguration
  )(implicit rr: RasterReader[HadoopGeoTiffRDD.Options, (ProjectedExtent, MultibandTile)]): DStream[(ProjectedExtent, MultibandTile)] = {
    ssc.fileStream[Path, Array[Byte], BytesFileInputFormat](
      directory    = path.toUri.toString,
      filter       = filter,
      newFilesOnly = newFilesOnly,
      conf         = conf
    ).transform {
      _.mapPartitions(
        _.map { case (_, bytes) => rr.readFully(ByteBuffer.wrap(bytes), options) },
        preservesPartitioning = true
      )
    }
  }

  def hadoopTemporalMultibandGeoTiffDStream(
    path: Path,
    filter: Path => Boolean = (p: Path) => HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions.exists(p.toString.endsWith),
    newFilesOnly: Boolean = false,
    options: Options,
    conf: Configuration = sc.hadoopConfiguration
  )(implicit rr: RasterReader[HadoopGeoTiffRDD.Options, (TemporalProjectedExtent, MultibandTile)]): DStream[(TemporalProjectedExtent, MultibandTile)] = {
    ssc.fileStream[Path, Array[Byte], BytesFileInputFormat](
      directory    = path.toUri.toString,
      filter       = filter,
      newFilesOnly = newFilesOnly,
      conf         = conf
    ).transform {
      _.mapPartitions(
        _.map { case (_, bytes) => rr.readFully(ByteBuffer.wrap(bytes), options) },
        preservesPartitioning = true
      )
    }
  }
}
