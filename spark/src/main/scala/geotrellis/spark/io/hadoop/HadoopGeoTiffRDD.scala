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

object HadoopGeoTiffRDD {
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

  def apply[K, V](path: Path)(implicit sc: SparkContext, gif: GeoTiffInputFormattable[K, V]): RDD[(K, V)] =
    apply(path, Options.DEFAULT)

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


  def spatial(path: Path)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    spatial(path, Options.DEFAULT)

  def spatial(path: Path, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, Tile)] =
    apply[ProjectedExtent, Tile](path, options)

  def spatialMultiband(path: Path)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    spatialMultiband(path, Options.DEFAULT)

  def spatialMultiband(path: Path, options: Options)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    apply[ProjectedExtent, MultibandTile](path, options)

  def temporal(path: Path)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    temporal(path, Options.DEFAULT)

  def temporal(path: Path, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    apply[TemporalProjectedExtent, Tile](path, options)

  def temporalMultiband(path: Path)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    temporalMultiband(path, Options.DEFAULT)

  def temporalMultiband(path: Path, options: Options)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    apply[TemporalProjectedExtent, MultibandTile](path, options)
}
