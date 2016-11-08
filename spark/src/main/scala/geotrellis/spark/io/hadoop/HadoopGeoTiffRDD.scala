package geotrellis.spark.io.hadoop

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.tags.TiffTags
import geotrellis.spark._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.vector.ProjectedExtent

import spire.syntax.cfor._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.InputFormat
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import java.lang.Class

/**
 * The HadoopGeoTiffRDD object allows for the creation of whole or windowed RDD[(K, V)]s
 * from files on Hdfs.
 */
object HadoopGeoTiffRDD {
  /**
   * This case class contains the various parameters one can set when reading
   * RDDs from Hdfs using Spark.
   *
   * @param crs: What [[CRS]] K should be in. If `None`, the reader will use the file's original CRS.
   * @param timeTag: A `String` that states the temporal information about the tile. Uses the default timeTag
   *   if none is supplied.
   * @param timeForamt: A `String` that states how the time should be formatted. Uses the default style if not set.
   * @param maxTileSize: How big the sub-tiles of the given tile should be. For example, inputting Some(300) will return
   *   multiple RDDs with tiles of dimension 300x300. If no size is given, then the file is not windowed.
   * @param numPartitions: How many partitions Spark should create when it repartitions the data. Default is `None`.
   *
   * @return A new istance of Options that contains the RDD creation settings.
   */
  case class Options(
    tiffExtensions: Seq[String] = Seq(".tif", ".TIF", ".tiff", ".TIFF"),
    crs: Option[CRS] = None,
    timeTag: String = TemporalGeoTiffInputFormat.GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = TemporalGeoTiffInputFormat.GEOTIFF_TIME_FORMAT_DEFAULT,
    maxTileSize: Option[Int] = None,
    numPartitions: Option[Int] = None
  )

  object Options {
    def DEFAULT = Options()
  }

  /**
   * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   *
   * @return a RDD with key and value pair of type K and V, respectively.
   */
  def apply[K, V](path: Path)(implicit sc: SparkContext, gif: GeoTiffInputFormattable[K, V]): RDD[(K, V)] =
    apply(path, Options.DEFAULT)

  /**
   * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return a RDD with key and value pair of type K and V, respectively.
   */
  def apply[K, V](path: Path, options: Options)(implicit sc: SparkContext, gif: GeoTiffInputFormattable[K, V]): RDD[(K, V)] =
    options.maxTileSize match {
      case Some(tileSize) =>
        val conf = sc.hadoopConfiguration.withInputDirectory(path, options.tiffExtensions)
        val pathsAndDimensions: RDD[(Path, (Int, Int))] =
          sc.newAPIHadoopRDD(
            conf,
            classOf[TiffTagsInputFormat],
            classOf[Path],
            classOf[TiffTags]
          ).mapValues { tiffTags => (tiffTags.cols, tiffTags.rows) }

        apply[K, V](pathsAndDimensions, options)
      case None =>
        gif.load(path, options)
    }

  /**
   * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return a RDD with key and value pair of type K and V, respectively.
   */
  def apply[K, V](pathsToDimensions: RDD[(Path, (Int, Int))], options: Options)(implicit sc: SparkContext, gif: GeoTiffInputFormattable[K, V]): RDD[(K, V)] = {
    val windows: RDD[(Path, GridBounds)] =
      pathsToDimensions
        .flatMap { case (path, (cols, rows)) =>
          val result = scala.collection.mutable.ListBuffer[GridBounds]()
          options.maxTileSize match {
            case Some(tileSize) =>
              cfor(0)(_ < cols, _ + tileSize) { col =>
                cfor(0)(_ < rows, _ + tileSize) { row =>
                  result +=
                  GridBounds(
                    col,
                    row,
                    math.min(col + tileSize - 1, cols - 1),
                    math.min(row + tileSize - 1, rows - 1)
                  )
                }
              }
            case None =>
              result += GridBounds(0, 0, cols -1, rows - 1)
          }

          result.map((path, _))
      }

    val repartitioned =
      options.numPartitions match {
        case Some(p) => windows.repartition(p)
        case None => windows
      }

    gif.load(repartitioned, options)
  }


  /**
   * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[Tile]], respectiviley.
   * It assumes that the proveded files are [[SinglebandGeoTiff]]s.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   *
   * @return RDD[(ProjectedExtent, Tile)](s)
   */
  def spatial(path: Path)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    spatial(path, Options.DEFAULT)

  /**
   * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[Tile]], respectiviley.
   * It assumes that the proveded files are [[SinglebandGeoTiff]]s.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return RDD[(ProjectedExtent, Tile)](s)
   */
  def spatial(path: Path, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    apply[ProjectedExtent, Tile](path, options)

  /**
   * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[MultibandTile]], respectiviley.
   * It assumes that the proveded files are [[MultibandGeoTiff]]s.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   *
   * @return RDD[(ProjectedExtent, MultibandTile)](s)
   */
  def spatialMultiband(path: Path)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    spatialMultiband(path, Options.DEFAULT)

  /**
   * Creates RDDs with the [(K, V)] values being [[ProjectedExtent]] and [[MultibandTile]], respectiviley.
   * It assumes that the proveded files are [[MultibandGeoTiff]]s.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return RDD[(ProjectedExtent, MultibandTile)](s)
   */
  def spatialMultiband(path: Path, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    apply[ProjectedExtent, MultibandTile](path, options)

  /**
   * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[Tile]], respectiviley.
   * It assumes that the proveded files are [[SinglebandGeoTiff]]s.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   *
   * @return RDD[(TemporalProjectedExtent, Tile)](s)
   */
  def temporal(path: Path)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    temporal(path, Options.DEFAULT)

  /**
   * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[Tile]], respectiviley.
   * It assumes that the proveded files are [[SinglebandGeoTiff]]s.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return RDD[(TemporalProjectedExtent, Tile)](s)
   */
  def temporal(path: Path, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    apply[TemporalProjectedExtent, Tile](path, options)

  /**
   * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[MultibandTile]], respectiviley.
   * It assumes that the proveded files are [[MultibandbandGeoTiff]]s.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   *
   * @return RDD[(TemporalProjectedExtent, MultibandTile)](s)
   */
  def temporalMultiband(path: Path)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    temporalMultiband(path, Options.DEFAULT)

  /**
   * Creates RDDs with the [(K, V)] values being [[TemporalProjectedExtent]] and [[MultibandTile]], respectiviley.
   * It assumes that the proveded files are [[MultibandbandGeoTiff]]s.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return RDD[(TemporalProjectedExtent, MultibandTile)](s)
   */
  def temporalMultiband(path: Path, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    apply[TemporalProjectedExtent, MultibandTile](path, options)
}
