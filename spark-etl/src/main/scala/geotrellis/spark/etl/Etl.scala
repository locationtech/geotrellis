package geotrellis.spark.etl

import geotrellis.raster.{RasterExtent, CellGrid}
import geotrellis.raster.crop.CropMethods
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.raster.reproject._
import geotrellis.raster.resample.{ ResampleMethod, NearestNeighbor }
import geotrellis.raster.stitch.Stitcher
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.tiling._
import org.slf4j.LoggerFactory
import scala.reflect._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.tiling._
import geotrellis.vector._

import com.typesafe.scalalogging.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.slf4j.LoggerFactory

import scala.reflect._
import scala.reflect.runtime.universe._

object Etl {
  val defaultModules = Array(s3.S3Module, hadoop.HadoopModule, accumulo.AccumuloModule)

  def ingest[
    I: Component[?, ProjectedExtent]: TypeTag: ? => TilerKeyMethods[I, K],
    K: SpatialComponent: Boundable: TypeTag,
    V <: CellGrid: TypeTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](
    args: Seq[String], keyIndexMethod: KeyIndexMethod[K], modules: Seq[TypedModule] = Etl.defaultModules
  )(implicit sc: SparkContext) = {
    implicit def classTagK = ClassTag(typeTag[K].mirror.runtimeClass(typeTag[K].tpe)).asInstanceOf[ClassTag[K]]
    implicit def classTagV = ClassTag(typeTag[V].mirror.runtimeClass(typeTag[V].tpe)).asInstanceOf[ClassTag[V]]

    /* parse command line arguments */
    val etl = Etl(args)
    /* load source tiles using input module specified */
    val sourceTiles = etl.load[I, V]
    /* perform the reprojection and mosaicing step to fit tiles to LayoutScheme specified */
    val (zoom, tiled) = etl.tile(sourceTiles)
    /* save and optionally pyramid the mosaiced layer */
    etl.save[K, V](LayerId(etl.conf.layerName(), zoom), tiled, keyIndexMethod)
  }
}

case class Etl(args: Seq[String], @transient modules: Seq[TypedModule] = Etl.defaultModules) {

  @transient lazy val logger: Logger = Logger(LoggerFactory getLogger getClass.getName)
  @transient val conf = new EtlConf(args)

  def scheme: Either[LayoutScheme, LayoutDefinition] = {
    if (conf.layoutScheme.isDefined) {
      val scheme = conf.layoutScheme()(conf.crs(), conf.tileSize())
      logger.info(scheme.toString)
      Left(scheme)
    } else if (conf.layoutExtent.isDefined) {
      val layout = LayoutDefinition(RasterExtent(conf.layoutExtent(), conf.cellSize()), conf.tileSize())
      logger.info(layout.toString)
      Right(layout)
    } else
      sys.error("Either layoutScheme or layoutExtent with cellSize must be provided")
  }

  @transient val combinedModule = modules reduce (_ union _)

  /**
    * Loads RDD of rasters using the input module specified in the arguments.
    * This RDD will contain rasters as they are stored, possibly overlapping and not conforming to any tile layout.
    *
    * @tparam I Input key type
    * @tparam V Input raster value type
    */
  def load[I: TypeTag, V <: CellGrid: TypeTag]()(implicit sc: SparkContext): RDD[(I, V)] = {
    val plugin =
      combinedModule
        .findSubclassOf[InputPlugin[I, V]]
        .find(_.suitableFor(conf.input(), conf.format()))
        .getOrElse(sys.error(s"Unable to find input module of type '${conf.input()}' for format `${conf.format()}"))

    plugin(conf.inputProps)
  }

  /**
    * Tiles RDD of arbitrary rasters to conform to a layout scheme or definition provided in the arguments.
    * First metadata will be collected over input rasters to determine the overall extent, common crs, and resolution.
    * This information will be used to select a LayoutDefinition if LayoutScheme is provided in the arguments.
    *
    * The tiling step will use this LayoutDefinition to cut input rasters into chunks that conform to the layout.
    * If multiple rasters contribute to single target tile their values will be merged cell by cell.
    *
    * The timing of the reproject steps depends on the method chosen.
    * BufferedReproject must be performed after the tiling step because it leans on SpatialComponent to identify neighboring
    * tiles and sample their edge pixels. This method is the default and produces the best results.
    *
    * PerTileReproject method will be performed before the tiling step, on source tiles. When using this method the
    * reproject logic does not have access to pixels past the tile boundary and will see them as NODATA.
    * However, this approach does not require all source tiles to share a projection.
    *
    * @param rdd    RDD of source rasters
    * @param method Resampling method to be used when merging raster chunks in tiling step
    */
  def tile[
    I: Component[?, ProjectedExtent]: (? => TilerKeyMethods[I, K]),
    V <: CellGrid: Stitcher: ClassTag: (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V]):
    (? => TileReprojectMethods[V]): (? => CropMethods[V]),
    K: SpatialComponent: Boundable: ClassTag
  ](
    rdd: RDD[(I, V)], method: ResampleMethod = NearestNeighbor
  )(implicit sc: SparkContext): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) = {
    val targetCellType = conf.cellType.get
    val destCrs = conf.crs()

    def adjustCellType(md: TileLayerMetadata[K]) =
      md.copy(cellType = targetCellType.getOrElse(md.cellType))

    conf.reproject() match {
      case PerTileReproject =>
        val reprojected = rdd.reproject(destCrs)
        val (zoom: Int, md: TileLayerMetadata[K]) = scheme match {
          case Left(layoutScheme) =>
            TileLayerMetadata.fromRdd(rdd, layoutScheme)
          case Right(layoutDefinition) =>
            TileLayerMetadata.fromRdd(rdd, layoutDefinition)
        }
        val amd = adjustCellType(md)
        zoom -> ContextRDD(reprojected.tileToLayout[K](amd, method), amd)

      case BufferedReproject =>
        val (_, md) = TileLayerMetadata.fromRdd(rdd, FloatingLayoutScheme(conf.tileSize()))
        val amd = adjustCellType(md)
        val tiled = ContextRDD(rdd.tileToLayout[K](amd, method), amd)
        scheme match {
          case Left(layoutScheme) =>
            tiled.reproject(destCrs, layoutScheme, method)
          case Right(layoutDefinition) =>
            tiled.reproject(destCrs, layoutDefinition, method)
        }
    }
  }

  /**
    * Saves provided RDD to an output module specified by the ETL arguments.
    * This step may perform two to one pyramiding until zoom level 1 is reached.
    *
    * @param id     Layout ID to b
    * @param rdd Tiled raster RDD with TileLayerMetadata
    * @param method Index Method that maps an instance of K to a Long
    * @tparam K  Key type with SpatialComponent corresponding LayoutDefinition
    * @tparam V  Tile raster with cells from single tile in LayoutDefinition
    */
  def save[
    K: SpatialComponent: TypeTag,
    V <: CellGrid: TypeTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]], method: KeyIndexMethod[K]): Unit = {
    implicit def classTagK = ClassTag(typeTag[K].mirror.runtimeClass(typeTag[K].tpe)).asInstanceOf[ClassTag[K]]
    implicit def classTagV = ClassTag(typeTag[V].mirror.runtimeClass(typeTag[V].tpe)).asInstanceOf[ClassTag[V]]

    val outputPlugin =
      combinedModule
        .findSubclassOf[OutputPlugin[K, V, TileLayerMetadata[K]]]
        .find { _.suitableFor(conf.output()) }
        .getOrElse(sys.error(s"Unable to find output module of type '${conf.output()}'"))

    def savePyramid(zoom: Int, rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]): Unit = {
      val currentId = id.copy(zoom = zoom)
      outputPlugin(currentId, rdd, method, conf.outputProps)

      scheme match {
        case Left(s) =>
          if (conf.pyramid() && zoom >= 1) {
            val (nextLevel, nextRdd) = Pyramid.up(rdd, s, zoom)
            savePyramid(nextLevel, nextRdd)
          }
        case Right(_) =>
          if (conf.pyramid())
            logger.error("Pyramiding only supported with layoutScheme, skipping pyramid step")
      }
    }

    savePyramid(id.zoom, rdd)
    logger.info("Done")
  }
}
