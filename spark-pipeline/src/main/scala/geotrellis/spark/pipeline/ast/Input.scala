/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.pipeline.ast

import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.tiling.TemporalProjectedExtent
import geotrellis.spark.store.hadoop.HadoopGeoTiffRDD
import geotrellis.spark.store.s3.S3GeoTiffRDD
import geotrellis.spark.pipeline.json.read._
import geotrellis.vector.ProjectedExtent

import geotrellis.store.s3.AmazonS3URI
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

trait Input[T] extends Node[T]

object Input {
  def spatialS3(arg: JsonRead)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = {
    val s3Uri = new AmazonS3URI(arg.uri)
    S3GeoTiffRDD.spatial(
      s3Uri.getBucket, s3Uri.getKey,
      S3GeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions,
        partitionBytes = arg.partitionBytes,
        chunkSize = arg.chunkSize,
        delimiter = arg.delimiter
      )
    )
  }

  def spatialMultibandS3(arg: JsonRead)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] = {
    val s3Uri = new AmazonS3URI(arg.uri)
    S3GeoTiffRDD.spatialMultiband(
      s3Uri.getBucket, s3Uri.getKey,
      S3GeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions,
        partitionBytes = arg.partitionBytes,
        chunkSize = arg.chunkSize,
        delimiter = arg.delimiter
      )
    )
  }

  def temporalS3(arg: JsonRead)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = {
    val s3Uri = new AmazonS3URI(arg.uri)
    S3GeoTiffRDD.temporal(
      s3Uri.getBucket, s3Uri.getKey,
      S3GeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions,
        partitionBytes = arg.partitionBytes,
        chunkSize = arg.chunkSize,
        delimiter = arg.delimiter,
        timeTag = arg.timeTag,
        timeFormat = arg.timeFormat
      )
    )
  }

  def temporalMultibandS3(arg: JsonRead)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = {
    val s3Uri = new AmazonS3URI(arg.uri)
    S3GeoTiffRDD.temporalMultiband(
      s3Uri.getBucket, s3Uri.getKey,
      S3GeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions,
        partitionBytes = arg.partitionBytes,
        chunkSize = arg.chunkSize,
        delimiter = arg.delimiter,
        timeTag = arg.timeTag,
        timeFormat = arg.timeFormat
      )
    )
  }

  def spatialHadoop(arg: JsonRead)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = {
    HadoopGeoTiffRDD.spatial(
      new Path(arg.uri),
      HadoopGeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions
      )
    )
  }

  def spatialMultibandHadoop(arg: JsonRead)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] = {
    HadoopGeoTiffRDD.spatialMultiband(
      new Path(arg.uri),
      HadoopGeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions
      )
    )
  }

  def temporalHadoop(arg: JsonRead)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = {
    HadoopGeoTiffRDD.temporal(
      new Path(arg.uri),
      HadoopGeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions
      )
    )
  }

  def temporalMultibandHadoop(arg: JsonRead)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = {
    HadoopGeoTiffRDD.temporalMultiband(
      new Path(arg.uri),
      HadoopGeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions
      )
    )
  }
}
