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

package geotrellis.spark.store.s3

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.layers._
import geotrellis.store.s3._
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.vector._

import software.amazon.awssdk.services.s3.S3Client

import com.typesafe.scalalogging.LazyLogging

import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import java.net.URI
import java.nio.ByteBuffer


/**
 * The S3GeoTiffRDD object allows for the creation of whole or windowed RDD[(K, V)]s from files on S3.
 */
object S3GeoTiffRDD extends LazyLogging {
  final val GEOTIFF_TIME_TAG_DEFAULT = "TIFFTAG_DATETIME"
  final val GEOTIFF_TIME_FORMAT_DEFAULT = "yyyy:MM:dd HH:mm:ss"

  /**
    * This case class contains the various parameters one can set when reading RDDs from S3 using Spark.
    *
    * TODO: Add persistLevel option
    *
    * @param tiffExtensions Read all file with an extension contained in the given list.
    * @param crs            Override CRS of the input files. If [[None]], the reader will use the file's original CRS.
    * @param timeTag        Name of tiff tag containing the timestamp for the tile.
    * @param timeFormat     Pattern for [[java.time.format.DateTimeFormatter]] to parse timeTag.
    * @param maxTileSize    Maximum allowed size of each tiles in output RDD.
    *                       May result in a one input GeoTiff being split amongst multiple records if it exceeds this size.
    *                       If no maximum tile size is specific, then each file is broken into 256x256 tiles.
    *                       If [[None]], then the whole file will be read in.
    *                       This option is incompatible with numPartitions and anything set to that parameter will be ignored.
    * @param numPartitions  How many partitions Spark should create when it repartitions the data.
    * @param partitionBytes Desired partition size in bytes, at least one item per partition will be assigned.
    *                       If no size is specified, then partitions 128 Mb in size will be created by default.
    *                       This option is incompatible with the numPartitions option. If both are set and maxTileSize isn't,
    *                       then partitionBytes will be ignored in favor of numPartitions. However, if maxTileSize is set,
    *                       then partitionBytes will be retained.
    *                       If [[None]] and maxTileSize is defined, then the default partitionBytes' value will still be used.
    *                       If maxTileSize is also [[None]], then partitionBytes will remain [[None]] as well.
    * @param chunkSize      How many bytes should be read in at a time when reading a file.
    *                       If [[None]], then 65536 byte chunks will be read in at a time.
    * @param delimiter      Delimiter to use for S3 objet listings. This provides a way to further define what files should
    *                       be read. If [[None]], then only the prefix will be used when determing which files to read.
    * @param getClient      A function to instantiate an S3Client.
    */
  case class Options(
    tiffExtensions: Seq[String] = Seq(".tif", ".TIF", ".tiff", ".TIFF"),
    crs: Option[CRS] = None,
    timeTag: String = GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = GEOTIFF_TIME_FORMAT_DEFAULT,
    maxTileSize: Option[Int] = Some(DefaultMaxTileSize),
    numPartitions: Option[Int] = None,
    partitionBytes: Option[Long] = Some(DefaultPartitionBytes),
    chunkSize: Option[Int] = None,
    delimiter: Option[String] = None,
    getClient: () => S3Client = S3ClientProducer.get
  ) extends RasterReader.Options

  private val DefaultMaxTileSize = 256
  private val DefaultPartitionBytes = 128l * 1024 * 1024

  object Options {
    def DEFAULT = Options()
  }

  /**
   * Create Configuration for [[S3InputFormat]] based on parameters and options.
   * Important: won't pass partitionBytes into hadoop configuration if numPartition options is set.
   *
   * @param bucket   Name of the bucket on S3 where the files are kept.
   * @param prefix   Prefix of all of the keys on S3 that are to be read in.
   * @param options  An instance of [[Options]] that contains any user defined or default settings.
   */
  private def configuration(bucket: String, prefix: String, options: S3GeoTiffRDD.Options)(implicit sc: SparkContext): Configuration = {
    if(options.numPartitions.isDefined && options.partitionBytes.isDefined)
      logger.warn("Both numPartitions and partitionBytes options are set. " +
        "Only numPartitions would be passed into hadoop configuration.")

    val conf = sc.hadoopConfiguration
    S3InputFormat.setBucket(conf, bucket)
    S3InputFormat.setPrefix(conf, prefix)
    S3InputFormat.setExtensions(conf, options.tiffExtensions)
    S3InputFormat.setCreateS3Client(conf, options.getClient)
    options.numPartitions
      .fold(S3InputFormat.removePartitionCount(conf)) { n =>
        S3InputFormat.setPartitionCount(conf, n)
        S3InputFormat.removePartitionBytes(conf)
      }
    if(options.numPartitions.isEmpty)
      options.partitionBytes
        .fold(S3InputFormat.removePartitionBytes(conf))(S3InputFormat.setPartitionBytes(conf, _))
    options.delimiter.fold(S3InputFormat.removeDelimiter(conf))(S3InputFormat.setDelimiter(conf, _))
    conf
  }

  /**
    * Creates a RDD[(K, V)] whose K and V  on the type of the GeoTiff that is going to be read in.
    *
    * This function has two modes of operation:
    * When options.maxTileSize is set windows will be read from GeoTiffs and their
    * size and count will be balanced among partitions using partitionBytes option.
    * Resulting partitions will be grouped in relation to GeoTiff segment layout.
    *
    * When maxTileSize is None the GeoTiffs will be read fully and balanced among
    * partitions using either numPartitions or partitionBytes option.
    *
    * @param  bucket    Name of the bucket on S3 where the files are kept.
    * @param  prefix    Prefix of all of the keys on S3 that are to be read in.
    * @param  uriToKey  Function to transform input key basing on the URI information.
    * @param  options   An instance of [[Options]] that contains any user defined or default settings.
    * @param  geometry  An optional geometry to filter by.  If this is provided, it is assumed that all GeoTiffs are in the same CRS, and that this geometry is in that CRS.
    */
  def apply[I, K, V](
    bucket: String, prefix: String,
    uriToKey: (URI, I) => K,
    options: Options,
    geometry: Option[Geometry]
  )(implicit sc: SparkContext, rr: RasterReader[Options, (I, V)]): RDD[(K, V)] = {

    options.maxTileSize match {
      case Some(maxTileSize) =>
        if (options.numPartitions.isDefined) logger.warn("numPartitions option is ignored")
        val infoReader = S3GeoTiffInfoReader(bucket, prefix, options)

        infoReader.readWindows(
          infoReader.geoTiffInfoRDD.map(new URI(_)),
          uriToKey,
          maxTileSize,
          options.partitionBytes.getOrElse(DefaultPartitionBytes),
          options,
          geometry)

      case None =>
        sc.newAPIHadoopRDD(
          configuration(bucket, prefix, options),
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
    * @param  bucket    Name of the bucket on S3 where the files are kept.
    * @param  prefix    Prefix of all of the keys on S3 that are to be read in.
    * @param  uriToKey  Function to transform input key basing on the URI information.
    * @param  options   An instance of [[Options]] that contains any user defined or default settings.
    * @param  geometry  An optional geometry to filter by.  If this is provided, it is assumed that all GeoTiffs are in the same CRS, and that this geometry is in that CRS.
    */
  def apply[I, K, V](
    bucket: String, prefix: String,
    uriToKey: (URI, I) => K,
    options: Options
  )(implicit sc: SparkContext, rr: RasterReader[Options, (I, V)]): RDD[(K, V)] = {
    apply(bucket, prefix, uriToKey, options, None)
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
