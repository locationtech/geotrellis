package geotrellis.spark.pipeline.ast

import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.io.hadoop.HadoopGeoTiffRDD
import geotrellis.spark.io.s3.S3GeoTiffRDD
import geotrellis.spark.pipeline.json.read._
import geotrellis.vector.ProjectedExtent

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

trait Read[T] extends Node[T]

object Read {
  def evalSpatialS3(arg: SpatialS3)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = {
    val s3Uri = arg.getS3URI
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

  def evalSpatialMultibandS3(arg: SpatialMultibandS3)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] = {
    val s3Uri = arg.getS3URI
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

  def evalTemporalS3(arg: TemporalS3)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = {
    val s3Uri = arg.getS3URI
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

  def evalTemporalMultibandS3(arg: TemporalMultibandS3)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = {
    val s3Uri = arg.getS3URI
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

  def evalSpatialHadoop(arg: SpatialHadoop)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = {
    HadoopGeoTiffRDD.spatial(
      new Path(arg.uri),
      HadoopGeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions
      )
    )
  }

  def evalSpatialMultibandHadoop(arg: SpatialMultibandHadoop)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] = {
    HadoopGeoTiffRDD.spatialMultiband(
      new Path(arg.uri),
      HadoopGeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions
      )
    )
  }

  def evalTemporalHadoop(arg: TemporalHadoop)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = {
    HadoopGeoTiffRDD.temporal(
      new Path(arg.uri),
      HadoopGeoTiffRDD.Options(
        crs = arg.getCRS,
        maxTileSize = arg.maxTileSize,
        numPartitions = arg.partitions
      )
    )
  }

  def evalTemporalMultibandHadoop(arg: TemporalMultibandHadoop)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = {
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
