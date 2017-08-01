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

package geotrellis.spark.io.s3

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.spark.io.{GeoTiffInfoReader, RasterReader}
import geotrellis.spark.io.s3.util.S3RangeReader
import geotrellis.util.{LazyLogging, StreamingByteReader}
import geotrellis.vector._
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import com.amazonaws.services.s3.model._
import java.net.URI
import java.nio.ByteBuffer

import com.typesafe.config.ConfigFactory

/**
 * The S3GeoTiffRDD object allows for the creation of whole or windowed RDD[(K, V)]s from files on S3.
 */
object S3GeoTiffRDD extends LazyLogging {
  final val GEOTIFF_TIME_TAG_DEFAULT = "TIFFTAG_DATETIME"
  final val GEOTIFF_TIME_FORMAT_DEFAULT = "yyyy:MM:dd HH:mm:ss"
  lazy val windowSize: Option[Int] = try {
    Some(ConfigFactory.load().getInt("geotrellis.s3.rdd.read.windowSize"))
  } catch {
    case _: Throwable =>
      logger.warn("geotrellis.s3.rdd.read.windowSize is not set in .conf file.")
      None
  }

  /**
    * This case class contains the various parameters one can set when reading RDDs from S3 using Spark.
    *
    * TODO: Add persistLevel option
    *
    * @param tiffExtensions     Read all file with an extension contained in the given list.
    * @param crs            Override CRS of the input files. If [[None]], the reader will use the file's original CRS.
    * @param timeTag        Name of tiff tag containing the timestamp for the tile.
    * @param timeFormat     Pattern for [[java.time.format.DateTimeFormatter]] to parse timeTag.
    * @param maxTileSize    Maximum allowed size of each tiles in output RDD.
    *                       May result in a one input GeoTiff being split amongst multiple records if it exceeds this size.
    *                       If no maximum tile size is specific, then each file file is read fully.
    *                       1024 by defaut.
    * @param numPartitions  How many partitions Spark should create when it repartitions the data.
    * @param partitionBytes Desired partition size in bytes, at least one item per partition will be assigned.
                            This option is incompatible with the maxTileSize option.
    *                       128 Mb by default.
    * @param chunkSize      How many bytes should be read in at a time.
    * @param delimiter      Delimiter to use for S3 objet listings. See
    * @param getS3Client    A function to instantiate an S3Client. Must be serializable.
    */
  case class Options(
    tiffExtensions: Seq[String] = Seq(".tif", ".TIF", ".tiff", ".TIFF"),
    crs: Option[CRS] = None,
    timeTag: String = GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = GEOTIFF_TIME_FORMAT_DEFAULT,
    maxTileSize: Option[Int] = None,
    numPartitions: Option[Int] = None,
    partitionBytes: Option[Long] = Some(128l * 1024 * 1024),
    chunkSize: Option[Int] = None,
    delimiter: Option[String] = None,
    getS3Client: () => S3Client = () => S3Client.DEFAULT
  ) extends RasterReader.Options

  object Options {
    def DEFAULT = Options()
  }

  /**
   * Create Configuration for [[S3InputFormat]] based on parameters and options.
   *
   * @param bucket   Name of the bucket on S3 where the files are kept.
   * @param prefix   Prefix of all of the keys on S3 that are to be read in.
   * @param options  An instance of [[Options]] that contains any user defined or default settings.
   */
  private def configuration(bucket: String, prefix: String, options: S3GeoTiffRDD.Options)(implicit sc: SparkContext): Configuration = {
    val conf = sc.hadoopConfiguration
    S3InputFormat.setBucket(conf, bucket)
    S3InputFormat.setPrefix(conf, prefix)
    S3InputFormat.setExtensions(conf, options.tiffExtensions)
    S3InputFormat.setCreateS3Client(conf, options.getS3Client)
    options.numPartitions.foreach{ n => S3InputFormat.setPartitionCount(conf, n) }
    options.partitionBytes.foreach{ n => S3InputFormat.setPartitionBytes(conf, n) }
    options.delimiter.foreach { n => S3InputFormat.setDelimiter(conf, n) }
    conf
  }

  /**
    * Creates a RDD[(K, V)] whose K and V  on the type of the GeoTiff that is going to be read in.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def apply[I, K, V](bucket: String, prefix: String, uriToKey: (URI, I) => K, options: Options)
    (implicit sc: SparkContext, rr: RasterReader[Options, (I, V)]): RDD[(K, V)] = {

    val conf = configuration(bucket, prefix, options)
    lazy val sourceGeoTiffInfo = S3GeoTiffInfoReader(bucket, prefix, options)

    (options.maxTileSize, options.partitionBytes) match {
      case (None, Some(partitionBytes)) =>
        val segments: RDD[((String, GeoTiffReader.GeoTiffInfo), Array[GridBounds])] =
          sourceGeoTiffInfo.segmentsByPartitionBytes(partitionBytes, windowSize)

        segments.persist() // StorageLevel.MEMORY_ONLY by default
        val segmentsCount = segments.count.toInt

        logger.info(s"repartition into ${segmentsCount} partitions.")

        val repartition =
          if(segmentsCount > segments.partitions.length) segments.repartition(segmentsCount)
          else segments

        val result = repartition.flatMap { case ((key, md), segmentBounds) =>
          rr.readWindows(segmentBounds, md, options).map { case (k, v) =>
            uriToKey(new URI(s"s3://$bucket/$key"), k) -> v
          }
        }

        segments.unpersist()
        result

      case (Some(_), _) =>
        val objectRequestsToDimensions: RDD[(GetObjectRequest, (Int, Int))] =
          sc.newAPIHadoopRDD(
            conf,
            classOf[TiffTagsS3InputFormat],
            classOf[GetObjectRequest],
            classOf[TiffTags]
          ).mapValues { tiffTags => (tiffTags.cols, tiffTags.rows) }

        apply[I, K, V](objectRequestsToDimensions, uriToKey, options, sourceGeoTiffInfo)

      case _ =>
        sc.newAPIHadoopRDD(
          conf,
          classOf[BytesS3InputFormat],
          classOf[String],
          classOf[Array[Byte]]
        ).mapPartitions(
          _.map { case (key, bytes) =>
            val (k, v) = rr.readFully(ByteBuffer.wrap(bytes), options)
            uriToKey(new URI(key), k) -> v
          },
          preservesPartitioning = true
        )
    }
  }

  /**
    * Creates a RDD[(K, V)] whose K and V  on the type of the GeoTiff that is going to be read in.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def apply[K, V](bucket: String, prefix: String, options: Options)
                 (implicit sc: SparkContext, rr: RasterReader[Options, (K, V)]): RDD[(K, V)] =
    apply[K, K, V](bucket, prefix, (_: URI, key: K) => key, options)

  /**
    * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
    *
    * @param objectRequestsToDimensions A RDD of GetObjectRequest of a given GeoTiff and its cols and rows as a (Int, Int).
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options An instance of [[Options]] that contains any user defined or default settings.
    */
  def apply[I, K, V](objectRequestsToDimensions: RDD[(GetObjectRequest, (Int, Int))], uriToKey: (URI, I) => K, options: Options, sourceGeoTiffInfo: => GeoTiffInfoReader)
    (implicit rr: RasterReader[Options, (I, V)]): RDD[(K, V)] = {

    val windows =
      objectRequestsToDimensions
        .flatMap { case (objectRequest, (cols, rows)) =>
          RasterReader.listWindows(cols, rows, options.maxTileSize).map((objectRequest, _))
        }

    // Windowed reading may have produced unbalanced partitions due to files of differing size
    val repartitioned =
      options.numPartitions match {
        case Some(p) =>
          logger.info(s"repartition into $p partitions.")
          windows.repartition(p)
        case None =>
          options.partitionBytes match {
            case Some(byteCount) =>
              sourceGeoTiffInfo.estimatePartitionsNumber(byteCount, options.maxTileSize) match {
                case Some(numPartitions) if numPartitions != windows.partitions.length =>
                  logger.info(s"repartition into $numPartitions partitions.")
                  windows.repartition(numPartitions)
                case _ => windows
              }
            case _ =>
              windows
          }
      }

    repartitioned.map { case (objectRequest: GetObjectRequest, pixelWindow: GridBounds) =>
      val reader = options.chunkSize match {
        case Some(chunkSize) =>
          StreamingByteReader(S3RangeReader(objectRequest, options.getS3Client()), chunkSize)
        case None =>
          StreamingByteReader(S3RangeReader(objectRequest, options.getS3Client()))
      }

      val (k, v) = rr.readWindow(reader, pixelWindow, options)

      uriToKey(new URI(s"s3://${objectRequest.getBucketName}/${objectRequest.getKey}"), k) -> v
    }
  }

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as singleband GeoTiffs.
    * If a GeoTiff contains multiple bands, only the first will be read.
    *
    * @param bucket Name of the bucket on S3 where the files are kept.
    * @param prefix Prefix of all of the keys on S3 that are to be read in.
    * @param uriToKey function to transform input key basing on the URI information.
    */
  def singleband[I, K](bucket: String, prefix: String, uriToKey: (URI, I) => K, options: Options)(implicit sc: SparkContext, rr: RasterReader[Options, (I, Tile)]): RDD[(K, Tile)] =
    apply[I, K, Tile](bucket, prefix, uriToKey, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as singleband GeoTiffs.
    * If a GeoTiff contains multiple bands, only the first will be read.
    *
    * @param bucket Name of the bucket on S3 where the files are kept.
    * @param prefix Prefix of all of the keys on S3 that are to be read in.
    */
  def singleband[K](bucket: String, prefix: String, options: Options)(implicit sc: SparkContext, rr: RasterReader[Options, (K, Tile)]): RDD[(K, Tile)] =
    apply[K, Tile](bucket, prefix, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as multiband GeoTiffs.
    * If a GeoTiff contains multiple bands, only the first will be read.
    *
    * @param bucket Name of the bucket on S3 where the files are kept.
    * @param prefix Prefix of all of the keys on S3 that are to be read in.
    * @param uriToKey function to transform input key basing on the URI information.
    */
  def multiband[I, K](bucket: String, prefix: String, uriToKey: (URI, I) => K, options: Options)(implicit sc: SparkContext, rr: RasterReader[Options, (I, MultibandTile)]): RDD[(K, MultibandTile)] =
    apply[I, K, MultibandTile](bucket, prefix, uriToKey, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as multiband GeoTiffs.
    * If a GeoTiff contains multiple bands, only the first will be read.
    *
    * @param bucket Name of the bucket on S3 where the files are kept.
    * @param prefix Prefix of all of the keys on S3 that are to be read in.
    */
  def multiband[K](bucket: String, prefix: String, options: Options)(implicit sc: SparkContext, rr: RasterReader[Options, (K, MultibandTile)]): RDD[(K, MultibandTile)] =
    apply[K, MultibandTile](bucket, prefix, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as singleband GeoTiffs.
    * If a GeoTiff contains multiple bands, only the first will be read.
    *
    * @param bucket Name of the bucket on S3 where the files are kept.
    * @param prefix Prefix of all of the keys on S3 that are to be read in.
    */
  def spatial(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    spatial(bucket, prefix, Options.DEFAULT)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as singleband tiles.
    * If a GeoTiff contains multiple bands, only the first will be read.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def spatial(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    singleband[ProjectedExtent](bucket, prefix, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as singleband tiles.
    * If a GeoTiff contains multiple bands, only the first will be read.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def spatial(bucket: String, prefix: String, uriToKey: (URI, ProjectedExtent) => ProjectedExtent, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    singleband[ProjectedExtent, ProjectedExtent](bucket, prefix, uriToKey, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as multiband tiles.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    */
  def spatialMultiband(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    spatialMultiband(bucket, prefix, Options.DEFAULT)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as multiband tiles.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def spatialMultiband(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    multiband[ProjectedExtent](bucket, prefix, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as multiband tiles.
    * If a GeoTiff contains multiple bands, only the first will be read.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def spatialMultiband(bucket: String, prefix: String, uriToKey: (URI, ProjectedExtent) => ProjectedExtent, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    multiband[ProjectedExtent, ProjectedExtent](bucket, prefix, uriToKey, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as singleband tiles.
    * Will parse a timestamp from the default tiff tags to associate with each file.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    */
  def temporal(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    temporal(bucket, prefix, Options.DEFAULT)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as singleband tiles.
    * Will parse a timestamp from a tiff tags specified in options to associate with each tile.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    * @param options  Options for the reading process. Including the timestamp tiff tag and its pattern.
    */
  def temporal(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    singleband[TemporalProjectedExtent](bucket, prefix, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as singleband tiles.
    * Will parse a timestamp from a tiff tags specified in options to associate with each tile.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  Options for the reading process. Including the timestamp tiff tag and its pattern.
    */
  def temporal(bucket: String, prefix: String, uriToKey: (URI, TemporalProjectedExtent) => TemporalProjectedExtent, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    singleband[TemporalProjectedExtent, TemporalProjectedExtent](bucket, prefix, uriToKey, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as multiband tiles.
    * Will parse a timestamp from a tiff tags specified in options to associate with each tile.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    */
  def temporalMultiband(bucket: String, prefix: String)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    temporalMultiband(bucket, prefix, Options.DEFAULT)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as multiband tiles.
    * Will parse a timestamp from a tiff tags specified in options to associate with each tile.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    * @param options  Options for the reading process. Including the timestamp tiff tag and its pattern.
    */
  def temporalMultiband(bucket: String, prefix: String, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    multiband[TemporalProjectedExtent](bucket, prefix, options)

  /**
    * Creates RDD that will read all GeoTiffs in the given bucket and prefix as multiband tiles.
    * Will parse a timestamp from a tiff tags specified in options to associate with each tile.
    *
    * @param bucket   Name of the bucket on S3 where the files are kept.
    * @param prefix   Prefix of all of the keys on S3 that are to be read in.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  Options for the reading process. Including the timestamp tiff tag and its pattern.
    */
  def temporalMultiband(bucket: String, prefix: String, uriToKey: (URI, TemporalProjectedExtent) => TemporalProjectedExtent, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    multiband[TemporalProjectedExtent, TemporalProjectedExtent](bucket, prefix, uriToKey, options)
}
