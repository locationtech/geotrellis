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

package geotrellis.spark.io.cog

import java.net.URI
import java.nio.ByteBuffer

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark._
import geotrellis.spark.io.RasterReader
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.util.StreamingByteReader
import geotrellis.vector.ProjectedExtent
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

/**
  * Allows for reading of whole or windowed GeoTiff as RDD[(K, V)]s through Hadoop FileSystem API.
  */
object GeoTiffRDD {
  final val GEOTIFF_TIME_TAG_DEFAULT = "TIFFTAG_DATETIME"
  final val GEOTIFF_TIME_FORMAT_DEFAULT = "yyyy:MM:dd HH:mm:ss"

  /**
    * This case class contains the various parameters one can set when reading RDDs from Hadoop using Spark.
    *
    * @param tiffExtensions Read all file with an extension contained in the given list.
    * @param crs           Override CRS of the input files. If [[None]], the reader will use the file's original CRS.
    * @param timeTag       Name of tiff tag containing the timestamp for the tile.
    * @param timeFormat    Pattern for [[java.time.format.DateTimeFormatter]] to parse timeTag.
    * @param maxTileSize   Maximum allowed size of each tiles in output RDD.
    *                      May result in a one input GeoTiff being split amongst multiple records if it exceeds this size.
    *                      If no maximum tile size is specific, then each file file is read fully.
    * @param numPartitions How many partitions Spark should create when it repartitions the data.
    * @param chunkSize     How many bytes should be read in at a time.
    */

  case class Options(
    tiffExtensions: Seq[String] = Seq(".tif", ".TIF", ".tiff", ".TIFF"),
    crs: Option[CRS] = None,
    timeTag: String = GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = GEOTIFF_TIME_FORMAT_DEFAULT,
    maxTileSize: Option[Int] = None,
    numPartitions: Option[Int] = None,
    chunkSize: Option[Int] = None
  ) extends RasterReader.Options

  object Options {
    def DEFAULT = Options()
  }

  /**
    * Create Configuration for [[BinaryFileInputFormat]] based on parameters and options.
    *
    * @param path     Hdfs GeoTiff path.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  private def configuration(path: Path, options: Options)(implicit sc: SparkContext): Configuration = {
    val conf = sc.hadoopConfiguration.withInputDirectory(path, options.tiffExtensions)
    conf
  }

  /**
    * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
    *
    * @param path     Hdfs GeoTiff path.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def apply[I, K, V](path: Path, uriToKey: (URI, I) => K, options: Options)(implicit sc: SparkContext, rr: RasterReader[Options, (I, V)]): RDD[(K, V)] = {
    val conf = configuration(path, options)
    options.maxTileSize match {
      case Some(tileSize) =>
        val pathsAndDimensions: RDD[(Path, (Int, Int))] =
          sc.newAPIHadoopRDD(
            conf,
            classOf[TiffTagsInputFormat],
            classOf[Path],
            classOf[TiffTags]
          ).mapValues { tiffTags => (tiffTags.cols, tiffTags.rows) }

        apply[I, K, V](pathsAndDimensions, uriToKey, options)
      case None =>
        sc.newAPIHadoopRDD(
          conf,
          classOf[BytesFileInputFormat],
          classOf[Path],
          classOf[Array[Byte]]
        ).mapPartitions(
          _.map { case (p, bytes) =>
            val (k, v) = rr.readFully(ByteBuffer.wrap(bytes), options)
            uriToKey(p.toUri, k) -> v
          },
          preservesPartitioning = true
        )
    }
  }

  /**
    * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
    *
    * @param path     Hdfs GeoTiff path.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def apply[K, V](path: Path, options: Options)(implicit sc: SparkContext, rr: RasterReader[Options, (K, V)]): RDD[(K, V)] =
    apply[K, K, V](path, (_: URI, key: K) => key, options)

  /**
    * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
    *
    * @param pathsToDimensions  RDD keyed by GeoTiff path with (cols, rows) tuple as value.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options            An instance of [[Options]] that contains any user defined or default settings.
    */
  def apply[I, K, V](pathsToDimensions: RDD[(Path, (Int, Int))], uriToKey: (URI, I) => K, options: Options)
                    (implicit rr: RasterReader[Options, (I, V)]): RDD[(K, V)] = {

    val conf = new SerializableConfiguration(pathsToDimensions.sparkContext.hadoopConfiguration)

    val windows: RDD[(Path, GridBounds)] =
      pathsToDimensions
        .flatMap { case (objectRequest, (cols, rows)) =>
          RasterReader.listWindows(cols, rows, options.maxTileSize).map((objectRequest, _))
        }

    // Windowed reading may have produced unbalanced partitions due to files of differing size
    val repartitioned =
      options.numPartitions match {
        case Some(p) => windows.repartition(p)
        case None => windows
      }

    repartitioned.map { case (path: Path, pixelWindow: GridBounds) =>
      val reader = options.chunkSize match {
        case Some(chunkSize) =>
          StreamingByteReader(HdfsRangeReader(path, conf.value), chunkSize)
        case None =>
          StreamingByteReader(HdfsRangeReader(path, conf.value))
      }

      val (k, v) = rr.readWindow(reader, pixelWindow, options)
      uriToKey(path.toUri, k) -> v
    }
  }

  /**
    * Creates RDDs with the [(K, V)] values where V is a [[Tile]].
    * It assumes that the provided files are [[SinglebandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def singleband[I, K](path: Path, uriToKey: (URI, I) => K, options: Options)(implicit sc: SparkContext, rr: RasterReader[Options, (I, Tile)]): RDD[(K, Tile)] =
    apply[I, K, Tile](path, uriToKey, options)

  /**
    * Creates RDDs with the [(K, V)] values where V is a [[Tile]].
    * It assumes that the provided files are [[SinglebandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def singleband[K](path: Path, options: Options)(implicit sc: SparkContext, rr: RasterReader[Options, (K, Tile)]): RDD[(K, Tile)] =
    apply[K, Tile](path, options)

  /**
    * Creates RDDs with the [(K, V)] values where V is a [[MultibandTile]].
    * It assumes that the provided files are [[MultibandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */

  def multiband[I, K](path: Path, uriToKey: (URI, I) => K, options: Options)(implicit sc: SparkContext, rr: RasterReader[Options, (I, MultibandTile)]): RDD[(K, MultibandTile)] =
    apply[I, K, MultibandTile](path, uriToKey, options)

  /**
    * Creates RDDs with the [(K, V)] values where V is a [[MultibandTile]].
    * It assumes that the provided files are [[MultibandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def multiband[K](path: Path, options: Options)(implicit sc: SparkContext, rr: RasterReader[Options, (K, MultibandTile)]): RDD[(K, MultibandTile)] =
    apply[K, MultibandTile](path,options)

  /**
    * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[Tile]], respectively.
    * It assumes that the provided files are [[SinglebandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    */
  def spatial(path: Path)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    spatial(path, Options.DEFAULT)

  /**
    * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[Tile]], respectively.
    * It assumes that the provided files are [[SinglebandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def spatial(path: Path, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    singleband[ProjectedExtent](path, options)

  /**
    * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[Tile]], respectively.
    * It assumes that the provided files are [[SinglebandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def spatial(path: Path, uriToKey: (URI, ProjectedExtent) => ProjectedExtent, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    singleband[ProjectedExtent, ProjectedExtent](path, uriToKey, options)

  /**
    * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[MultibandTile]], respectively.
    * It assumes that the provided files are [[MultibandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    */
  def spatialMultiband(path: Path)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    spatialMultiband(path, Options.DEFAULT)

  /**
    * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[MultibandTile]], respectively.
    * It assumes that the provided files are [[MultibandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def spatialMultiband(path: Path, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    multiband[ProjectedExtent](path, options)

  /**
    * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[MultibandTile]], respectively.
    * It assumes that the provided files are [[MultibandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def spatialMultiband(path: Path, uriToKey: (URI, ProjectedExtent) => ProjectedExtent, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    multiband[ProjectedExtent, ProjectedExtent](path, uriToKey, options)

  /**
    * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[Tile]], respectively.
    * It assumes that the provided files are [[SinglebandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    */
  def temporal(path: Path)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    temporal(path, Options.DEFAULT)

  /**
    * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[Tile]], respectively.
    * It assumes that the provided files are [[SinglebandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def temporal(path: Path, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    singleband[TemporalProjectedExtent](path, options)

  /**
    * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[Tile]], respectively.
    * It assumes that the provided files are [[SinglebandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def temporal(path: Path, uriToKey: (URI, TemporalProjectedExtent) => TemporalProjectedExtent, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    singleband[TemporalProjectedExtent, TemporalProjectedExtent](path, uriToKey, options)

  /**
    * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[MultibandTile]], respectively.
    * It assumes that the provided files are [[MultibandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    */
  def temporalMultiband(path: Path)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    temporalMultiband(path, Options.DEFAULT)

  /**
    * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[MultibandTile]], respectively.
    * It assumes that the provided files are [[MultibandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def temporalMultiband(path: Path, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    multiband[TemporalProjectedExtent](path, options)

  /**
    * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[MultibandTile]], respectively.
    * It assumes that the provided files are [[MultibandGeoTiff]]s.
    *
    * @param path     Hadoop path to recursively search for GeoTiffs.
    * @param uriToKey function to transform input key basing on the URI information.
    * @param options  An instance of [[Options]] that contains any user defined or default settings.
    */
  def temporalMultiband(path: Path, uriToKey: (URI, TemporalProjectedExtent) => TemporalProjectedExtent, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    multiband[TemporalProjectedExtent, TemporalProjectedExtent](path, uriToKey, options)
}
