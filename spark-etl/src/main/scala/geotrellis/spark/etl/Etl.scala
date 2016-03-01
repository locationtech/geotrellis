package geotrellis.spark.etl

import com.typesafe.scalalogging.slf4j.Logger
import geotrellis.raster.crop.CropMethods
import geotrellis.raster.stitch.Stitcher
import geotrellis.raster.{RasterExtent, CellGrid}
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.raster.reproject._
import geotrellis.raster.resample.NearestNeighbor
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

  def ingest[
  I: ProjectedExtentComponent: TypeTag: ? => TilerKeyMethods[I, K],
  K: SpatialComponent: TypeTag,
  V <: CellGrid: TypeTag: Stitcher: ? => TileReprojectMethods[V]: ? => CropMethods[V]: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](args: Seq[String], keyIndexMethod: KeyIndexMethod[K], modules: Seq[TypedModule] = Etl.defaultModules)(implicit sc: SparkContext) = {
    implicit def classTagK = ClassTag(typeTag[K].mirror.runtimeClass(typeTag[K].tpe)).asInstanceOf[ClassTag[K]]
    implicit def classTagV = ClassTag(typeTag[V].mirror.runtimeClass(typeTag[V].tpe)).asInstanceOf[ClassTag[V]]

    val etl = Etl(args)
    val sourceTiles = etl.load[I, V]
    val (_, metadata) = etl.collectMetadata(sourceTiles)
    val tiled = sourceTiles.tileToLayout[K](metadata, NearestNeighbor)
    val (zoom, reprojected) = etl.reproject(ContextRDD(tiled, metadata))
    etl.save[K, V](LayerId(etl.conf.layerName(), zoom), reprojected, keyIndexMethod)
  }
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

  def load[I: TypeTag, V <: CellGrid: TypeTag]()(implicit sc: SparkContext): RDD[(I, V)] = {
    val plugin =
      combinedModule
        .findSubclassOf[InputPlugin[I, V]]
        .find(_.suitableFor(conf.input(), conf.format()))
        .getOrElse(sys.error(s"Unable to find input module of type '${conf.input()}' for format `${conf.format()}"))

    plugin(conf.inputProps)
  }

  def reproject[
  K: SpatialComponent: ClassTag,
  V <: CellGrid: ClassTag: Stitcher: ? => TileReprojectMethods[V]: ? => CropMethods[V]: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[M]): (Int, RDD[(K, V)] with Metadata[M]) = {
    val tuple = rdd.reproject(conf.crs(), conf.layoutScheme()(conf.crs(), conf.tileSize()))
    tuple._2.persist(conf.cache())
    tuple
  }

  def collectMetadata[I: ProjectedExtentComponent, V <: CellGrid](rdd: RDD[(I, V)])(implicit sc: SparkContext): (Int, M) =
    scheme match {
      case Left(layoutScheme: LayoutScheme) => RasterMetaData.fromRdd(rdd, layoutScheme)
      case Right(layoutDefinition) => 0 -> RasterMetaData.fromRdd(rdd, layoutDefinition)
    }

  def save[
  K: SpatialComponent: TypeTag,
  V <: CellGrid: TypeTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
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
