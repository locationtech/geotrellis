package geotrellis.spark.io.cog

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags.{BasicTags, TiffTags, TileTags}
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
import monocle.syntax.apply._
import monocle.macros.Lenses
import java.net.URI
import java.nio.ByteBuffer

import geotrellis.spark.tiling.{LayoutDefinition, LayoutLevel, LayoutScheme, MapKeyTransform, ZoomedLayoutScheme}

/** See http://www.gdal.org/gdal_vrttut.html */
object COGMetadataReader {

  // contains LayoutDefinition
  case class VRT[T <: CellGrid](
   COGMetadata: List[COGMetadata[T]],
   layoutDefinition: LayoutDefinition
  ) {
    // how to query intersecting tiles?
    // how to get the correct tiff
    // how to get the correct overview (a smart resample function would wokr out)
    // zoom => cellSize function?
    def getTile(x: Int, y: Int, z: Int) = {



      FetchCOG.getTile(x, y, z)
    }

    def query(filter: String) =
      FetchCOG.query(filter)
  }

  /** Contains PATH and all tags for each tiff, including segments offsets */
  case class COGMetadata[T <: CellGrid](
    path: String,
    tiffTags: TiffTags,
    tiff: GeoTiff[T],
    overviews: List[COGMetadata[T]] = Nil
  ) {
    lazy val localMapTransform =
      MapKeyTransform(tiffTags.extent, tiffTags.geoTiffSegmentLayout.tileLayout.layoutDimensions)
    lazy val (tileCols, tileRows) = tiffTags.cols -> tiffTags.rows
    lazy val extent = tiffTags.extent

    // keys relative to the current tiff
    def localKeys: Iterator[SpatialKey] =
      localMapTransform(tiffTags.extent)
        .coordsIter
        .map { spatialComponent => spatialComponent: SpatialKey }

    def keys(layoutDefinition: LayoutDefinition): Iterator[SpatialKey] =
      layoutDefinition
        .mapTransform(tiffTags.extent)
        .coordsIter
        .map { spatialComponent => spatialComponent: SpatialKey }

    // how to deal with zoom level?

    // smartly gets correct raster
    def crop(x: Int, y: Int, z: Int)(layoutDefinition: LayoutDefinition, layoutScheme: ZoomedLayoutScheme): Raster[T] = {
      tiff.crop(layoutDefinition.mapTransform(x -> y), layoutScheme.levelForZoom(z).layout.cellSize)
    }

    def layoutLevel(layoutScheme: LayoutScheme): LayoutLevel =
      layoutScheme.levelFor(this.extent, tiffTags.cellSize)


    def getClosestOverview(zoom: Int, layoutScheme: LayoutScheme): COGMetadata[T] = {
      (this :: overviews)
        .map { v => v.layoutLevel(layoutScheme).zoom -> v }
        .filter(_._1 >= zoom)
        .minBy { case (z, _) => math.abs(z - zoom) }
        ._2
    }
  }

  object FetchCOG {
    def getTile(x: Int, y: Int, z: Int) = {
    }

    def query(filter: String) = {

    }
  }

  class Prototype2(tiff: SinglebandGeoTiff) {
    tiff
  }


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

        null
        //apply[I, K, V](pathsAndDimensions, uriToKey, options)
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

}
