package geotrellis.spark.etl

import com.typesafe.scalalogging.slf4j.Logger
import geotrellis.raster.{RasterExtent, CellGrid}
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.raster.reproject._
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark.ingest._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.tiling._
import org.slf4j.LoggerFactory
import scala.reflect._
import geotrellis.spark._
import geotrellis.spark.ingest._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.runtime.universe._

object Etl {
  val defaultModules = Array(s3.S3Module, hadoop.HadoopModule, accumulo.AccumuloModule)
}

case class Etl(args: Seq[String], @transient modules: Seq[TypedModule] = Etl.defaultModules) {

  @transient lazy val logger: Logger = Logger(LoggerFactory getLogger getClass.getName)

  type M = RasterMetaData

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

  def load[
    I: ProjectedExtentComponent: TypeTag,
    V <: CellGrid: TypeTag: ? => TileReprojectMethods[V]: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ]()(implicit sc: SparkContext): RDD[(I, V)] = {
    val plugin =
      combinedModule
        .findSubclassOf[InputPlugin[I, V]]
        .find(_.suitableFor(conf.input(), conf.format()))
        .getOrElse(sys.error(s"Unable to find input module of type '${conf.input()}' for format `${conf.format()}"))

    plugin(conf.inputProps)
  }

  def reproject[
    I: ProjectedExtentComponent,
    V <: CellGrid: ? => TileReprojectMethods[V]: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](rdd: RDD[(I, V)]): RDD[(I, V)] = rdd.reproject(conf.crs()).persist(conf.cache())

  def tile[
    I: ProjectedExtentComponent: ? => TilerKeyMethods[I, K],
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileReprojectMethods[V]: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](rdd: RDD[(I, V)])(implicit sc: SparkContext): (Int, RDD[(K, V)] with Metadata[M]) = {
    val crs = conf.crs()
    val targetCellType = conf.cellType.get

    val (zoom, rasterMetaData) = scheme match {
      case Left(layoutScheme) =>
        val (zoom, rmd) = RasterMetaData.fromRdd(rdd, crs, layoutScheme) { key => key.projectedExtent.extent }
        targetCellType match {
          case None => zoom -> rmd
          case Some(ct) => zoom -> rmd.copy(cellType = ct)
        }

      case Right(layoutDefinition) =>
        0 -> RasterMetaData(
          crs = crs,
          cellType = targetCellType.get,
          extent = layoutDefinition.extent,
          layout = layoutDefinition
        )
    }
    val tiles = rdd.cutTiles[K](rasterMetaData, NearestNeighbor)
    zoom -> ContextRDD(tiles, rasterMetaData)
  }

  def save[
    K: SpatialComponent: TypeTag,
    V <: CellGrid: TypeTag: ? => TileReprojectMethods[V]: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], method: KeyIndexMethod[K]): Unit = {
    implicit def classTagK = ClassTag(typeTag[K].mirror.runtimeClass(typeTag[K].tpe)).asInstanceOf[ClassTag[K]]
    implicit def classTagV = ClassTag(typeTag[V].mirror.runtimeClass(typeTag[V].tpe)).asInstanceOf[ClassTag[V]]

    val outputPlugin =
      combinedModule
        .findSubclassOf[OutputPlugin[K, V, M]]
        .find { _.suitableFor(conf.output()) }
        .getOrElse(sys.error(s"Unable to find output module of type '${conf.output()}'"))

    def savePyramid(zoom: Int, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
      val currentId = id.copy(zoom = zoom)
      outputPlugin(currentId, rdd, method, conf.outputProps)

      scheme match {
        case Left(s) =>
          if (conf.pyramid() && zoom > 1) {
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