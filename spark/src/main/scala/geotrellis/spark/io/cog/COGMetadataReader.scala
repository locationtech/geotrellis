package geotrellis.spark.io.cog

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.tags.{BasicTags, TiffTags, TileTags}
import geotrellis.spark._
import geotrellis.spark.io.RasterReader
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.util._
import geotrellis.vector.{Extent, ProjectedExtent}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import monocle.syntax.apply._
import monocle.macros.Lenses
import java.net.URI
import java.nio.ByteBuffer

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader.GeoTiffInfo
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark.tiling.CutTiles.logger
import spire.syntax.cfor._
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

    val (segmentCount, segmentByteCounts, segmentOffsets) =
      (tiffTags.segmentCount, tiffTags.segmentByteCounts, tiffTags.segmentOffsets)
    val geoTiffSegmentLayout = tiffTags.geoTiffSegmentLayout

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

    def layoutLevel(layoutScheme: LayoutScheme): LayoutLevel =
      layoutScheme.levelFor(this.extent, tiffTags.cellSize)

    // smartly gets correct raster
    def crop(x: Int, y: Int, z: Int)(layoutScheme: ZoomedLayoutScheme): Raster[T] = {
      val layout = layoutScheme.levelForZoom(z).layout
      tiff.crop(layout.mapTransform(SpatialKey(x, y)), layout.cellSize)
    }

    // or go this way?
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

  object md {
    // tiffTags looks like a useless thing here
    // handle temporl case ?
    case class GeoTiffMetadata[T <: CellGrid](tiff: GeoTiff[T], tiffTags: TiffTags) {
      def imageData: GeoTiffImageData = tiff.imageData
      def segmentLayout: GeoTiffSegmentLayout = imageData.segmentLayout

      lazy val (segmentCount, segmentByteCounts, segmentOffsets) =
        (tiffTags.segmentCount, tiffTags.segmentByteCounts, tiffTags.segmentOffsets)

      def localMapTransform =
        MapKeyTransform(tiff.extent, imageData.segmentLayout.tileLayout.layoutDimensions)

      def crop(x: Int, y: Int, z: Int)(layoutScheme: ZoomedLayoutScheme): Raster[T] = {
        val layout = layoutScheme.levelForZoom(z).layout
        tiff.crop(layout.mapTransform(SpatialKey(x, y)), layout.cellSize)
      }

      // keys relative to the current tiff
      def localKeys: Iterator[SpatialKey] =
        localMapTransform(tiffTags.extent)
          .coordsIter
          .map { spatialComponent => spatialComponent: SpatialKey }

      // to persist them as Indexes?
      def keys(layoutDefinition: LayoutDefinition): Iterator[SpatialKey] =
        layoutDefinition
          .mapTransform(tiffTags.extent)
          .coordsIter
          .map { spatialComponent => spatialComponent: SpatialKey }
    }

    case class GeoTiffLayerMetadata[T <: CellGrid](
      tileDimensions: (Int, Int) = 256 -> 256,
      tiffs: List[GeoTiffMetadata[T]] = Nil
     ) {
      def extent: Extent = tiffs.map(_.tiff.extent).reduceLeft(_ combine _)
      def rasterExtent = RasterExtent(extent, tiffs.head.tiffTags.cellSize)
      def layout = LayoutDefinition(rasterExtent, tileDimensions._1, tileDimensions._2)
      def mapTransform = layout.mapTransform
      def tileLayout = layout.tileLayout
      def layoutExtent = layout.extent
      def gridBounds = mapTransform(extent)

      def combine(other: GeoTiffLayerMetadata[T]): GeoTiffLayerMetadata[T] =
        this.copy(tiffs = tiffs ::: other.tiffs)

      def getTiles(
        x: Int,
        y: Int,
        z: Int
      )(layoutScheme: ZoomedLayoutScheme): List[Raster[T]] = {
        // a real pain here, makes sense to persist somehow indexes?
        // to put into our own tags / store in a separate file
        // would be not very cloud optimized though
        tiffs.collect { case md if md.keys(layout).contains(SpatialKey(x, y)) =>
          md.crop(x, y, z)(layoutScheme)
        }
      }
    }

    object GeoTiffLayerMetadata {
      def fetchSingleband(tileDimensions: (Int, Int) = 256 -> 256, tiffPaths: List[String] = "tiff" :: Nil): GeoTiffLayerMetadata[Tile] =
        GeoTiffLayerMetadata(
          tileDimensions,
          tiffs = tiffPaths.map { path =>
            GeoTiffMetadata(SinglebandGeoTiff(path, false, true), TiffTags(path))
          }
        )
    }

    object Test {
      val path = "/Users/daunnc/subversions/git/github/pomadchin/geotrellis/raster-test/data/geotiff-test-files/overviews/singleband_co.tif"
      val p = "/data/OLI/tiff-hsl/175/017/LC81750172014163LGN00.TIF"

      import geotrellis.raster.io.geotiff.reader._
      GeoTiffReader.readMultiband(p).crop(RasterExtent(Extent(0, 0, 0, 0), CellSize(0.5, 0.5)))

      GeoTiffReader.readSingleband(p).crop(RasterExtent(Extent(0, 0, 0, 0), CellSize(0.5, 0.5)))

    }
  }


  object md2 {
    val zz = "/data/OLI/tiff-hsl/175/017/LC81750172014163LGN00_LOW5_tiled.TIF"

    // tiffTags looks like a useless thing here
    // handle temporl case ?
    case class GeoTiffMetadata(info: GeoTiffInfo) {
      def tiffTile: GeoTiffTile = GeoTiffReader.geoTiffSinglebandTile(info)
      // or go this way?
      def getClosestOverview(zoom: Int, layoutScheme: LayoutScheme): GeoTiffInfo = {
        (info :: info.overviews)
          .map { i =>
            layoutScheme.levelFor(info.extent, i.segmentLayout.tileLayout.cellSize(info.extent)).zoom -> i
          }
          .filter(_._1 >= zoom)
          .minBy { case (z, _) => math.abs(z - zoom) }
          ._2
      }



      def segmentLayout: GeoTiffSegmentLayout = info.segmentLayout

      /*def crop(x: Int, y: Int, z: Int)(layoutScheme: ZoomedLayoutScheme): Raster[T] = {
        val layout = layoutScheme.levelForZoom(z).layout

        tiff.crop(layout.mapTransform(SpatialKey(x, y)), layout.cellSize)
      }*/

      // keys relative to the current tiff
      /*def localKeys: Iterator[SpatialKey] =
        localMapTransform(tiffTags.extent)
          .coordsIter
          .map { spatialComponent => spatialComponent: SpatialKey }*/

      // to persist them as Indexes?
      /*def keys(layoutDefinition: LayoutDefinition): Iterator[SpatialKey] =
        layoutDefinition
          .mapTransform(tiffTags.extent)
          .coordsIter
          .map { spatialComponent => spatialComponent: SpatialKey }*/
    }

    /*case class GeoTiffLayerMetadata[T <: CellGrid](
                                                    tileDimensions: (Int, Int) = 256 -> 256,
                                                    tiffs: List[GeoTiffMetadata[T]] = Nil
                                                  ) {
      def extent: Extent = tiffs.map(_.tiff.extent).reduceLeft(_ combine _)
      def rasterExtent = RasterExtent(extent, tiffs.head.tiffTags.cellSize)
      def layout = LayoutDefinition(rasterExtent, tileDimensions._1, tileDimensions._2)
      def mapTransform = layout.mapTransform
      def tileLayout = layout.tileLayout
      def layoutExtent = layout.extent
      def gridBounds = mapTransform(extent)

      def combine(other: GeoTiffLayerMetadata[T]): GeoTiffLayerMetadata[T] =
        this.copy(tiffs = tiffs ::: other.tiffs)

      def getTiles(
                    x: Int,
                    y: Int,
                    z: Int
                  )(layoutScheme: ZoomedLayoutScheme): List[Raster[T]] = {
        // a real pain here, makes sense to persist somehow indexes?
        // to put into our own tags / store in a separate file
        // would be not very cloud optimized though
        tiffs.collect { case md if md.keys(layout).contains(SpatialKey(x, y)) =>
          md.crop(x, y, z)(layoutScheme)
        }
      }
    }

    object GeoTiffLayerMetadata {
      def fetchSingleband(tileDimensions: (Int, Int) = 256 -> 256, tiffPaths: List[String] = "tiff" :: Nil): GeoTiffLayerMetadata[Tile] =
        GeoTiffLayerMetadata(
          tileDimensions,
          tiffs = tiffPaths.map { path =>
            GeoTiffMetadata(SinglebandGeoTiff(path, false, true), TiffTags(path))
          }
        )
    }*/

    /*object Test {
      val path = "/Users/daunnc/subversions/git/github/pomadchin/geotrellis/raster-test/data/geotiff-test-files/overviews/singleband_co.tif"
      val p = "/data/OLI/tiff-hsl/175/017/LC81750172014163LGN00.TIF"

      import geotrellis.raster.io.geotiff.reader._
      GeoTiffReader.readMultiband(p).crop(RasterExtent(Extent(0, 0, 0, 0), CellSize(0.5, 0.5)))

      GeoTiffReader.readSingleband(p).crop(RasterExtent(Extent(0, 0, 0, 0), CellSize(0.5, 0.5)))

    }*/
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
