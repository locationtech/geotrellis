package geotrellis.spark.pipeline.json.read

import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.io.hadoop.HadoopGeoTiffRDD
import geotrellis.spark.io.s3.S3GeoTiffRDD
import geotrellis.spark.pipeline.json._
import geotrellis.vector.ProjectedExtent

import io.circe.generic.extras.ConfiguredJsonCodec
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import com.amazonaws.services.s3.AmazonS3URI

import java.net.URI

trait Read extends PipelineExpr {
  val uri: String
  val crs: Option[String]
  val tag: Option[String]
  val maxTileSize: Option[Int]
  val partitions: Option[Int]

  def getURI = new URI(uri)
  def getTag = tag.getOrElse("default")
}

trait S3Read extends Read {
  def getS3URI = new AmazonS3URI(getURI)
}

@ConfiguredJsonCodec
case class SpatialS3(
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  partitionBytes: Option[Long] = None,
  chunkSize: Option[Int] = None,
  delimiter: Option[String] = None,
  `type`: String = "spatial.singleband.s3.read"
) extends S3Read {
  def eval(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = {
    val s3Uri = getS3URI
    S3GeoTiffRDD.spatial(
      s3Uri.getBucket, s3Uri.getKey,
      S3GeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions,
        partitionBytes = partitionBytes,
        chunkSize = chunkSize,
        delimiter = delimiter
      )
    )
  }
}

@ConfiguredJsonCodec
case class SpatialMultibandS3(
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  partitionBytes: Option[Long] = None,
  chunkSize: Option[Int] = None,
  delimiter: Option[String] = None,
  `type`: String = "spatial.multiband.s3.read"
) extends S3Read {
  def eval(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] = {
    val s3Uri = getS3URI
    S3GeoTiffRDD.spatialMultiband(
      s3Uri.getBucket, s3Uri.getKey,
      S3GeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions,
        partitionBytes = partitionBytes,
        chunkSize = chunkSize,
        delimiter = delimiter
      )
    )
  }
}

@ConfiguredJsonCodec
case class TemporalS3(
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  partitionBytes: Option[Long] = None,
  chunkSize: Option[Int] = None,
  delimiter: Option[String] = None,
  timeTag: String = HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT,
  timeFormat: String = HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT,
  `type`: String = "temporal.singleband.s3.read"
) extends S3Read {
  def eval(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = {
    val s3Uri = getS3URI
    S3GeoTiffRDD.temporal(
      s3Uri.getBucket, s3Uri.getKey,
      S3GeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions,
        partitionBytes = partitionBytes,
        chunkSize = chunkSize,
        delimiter = delimiter,
        timeTag = timeTag,
        timeFormat = timeFormat
      )
    )
  }
}

@ConfiguredJsonCodec
case class TemporalMultibandS3(
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  partitionBytes: Option[Long] = None,
  chunkSize: Option[Int] = None,
  delimiter: Option[String] = None,
  timeTag: String = HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT,
  timeFormat: String = HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT,
  `type`: String = "temporal.multiband.s3.read"
) extends S3Read {
  def eval(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = {
    val s3Uri = getS3URI
    S3GeoTiffRDD.temporalMultiband(
      s3Uri.getBucket, s3Uri.getKey,
      S3GeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions,
        partitionBytes = partitionBytes,
        chunkSize = chunkSize,
        delimiter = delimiter,
        timeTag = timeTag,
        timeFormat = timeFormat
      )
    )
  }
}

@ConfiguredJsonCodec
case class SpatialHadoop(
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  `type`: String = "spatial.singleband.hadoop.read"
) extends Read {
  def eval(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] = {
    HadoopGeoTiffRDD.spatial(
      new Path(uri),
      HadoopGeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions
      )
    )
  }
}

@ConfiguredJsonCodec
case class SpatialMultibandHadoop(
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  `type`: String = "spatial.multiband.hadoop.read"
) extends Read {
  def eval(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] = {
    HadoopGeoTiffRDD.spatialMultiband(
      new Path(uri),
      HadoopGeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions
      )
    )
  }
}

@ConfiguredJsonCodec
case class TemporalHadoop(
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  timeTag: String = HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT,
  timeFormat: String = HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT,
  `type`: String = "temporal.singleband.hadoop.read"
) extends Read {
  def eval(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = {
    HadoopGeoTiffRDD.temporal(
      new Path(uri),
      HadoopGeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions,
        timeTag = timeTag,
        timeFormat = timeFormat
      )
    )
  }
}

@ConfiguredJsonCodec
case class TemporalMultibandHadoop(
  uri: String,
  crs: Option[String] = None,
  tag: Option[String] = None,
  maxTileSize: Option[Int] = None,
  partitions: Option[Int] = None,
  timeTag: String = HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT,
  timeFormat: String = HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT,
  `type`: String = "temporal.multiband.hadoop.read"
) extends Read {
  def eval(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] = {
    HadoopGeoTiffRDD.temporalMultiband(
      new Path(uri),
      HadoopGeoTiffRDD.Options(
        crs = this.getCRS,
        maxTileSize = maxTileSize,
        numPartitions = partitions,
        timeTag = timeTag,
        timeFormat = timeFormat
      )
    )
  }
}
