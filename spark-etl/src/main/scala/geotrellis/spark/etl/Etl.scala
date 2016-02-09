package geotrellis.spark.etl

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.raster.RasterExtent
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.raster.reproject._
import geotrellis.spark.ingest._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.tiling._
import scala.reflect._
import geotrellis.raster.CellGrid
import geotrellis.spark._
import geotrellis.spark.ingest._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.runtime.universe._

object Etl {
  val defaultModules = Array(s3.S3Module, hadoop.HadoopModule, accumulo.AccumuloModule)
}

case class Etl[
  I: ProjectedExtentComponent: TypeTag,
  K: SpatialComponent: TypeTag,
  V <: CellGrid: TypeTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
](args: Seq[String], modules: Seq[TypedModule] = Etl.defaultModules) extends LazyLogging {
  type M = RasterMetaData
  implicit def classTagI = ClassTag(typeTag[I].mirror.runtimeClass(typeTag[I].tpe)).asInstanceOf[ClassTag[I]]
  implicit def classTagK = ClassTag(typeTag[K].mirror.runtimeClass(typeTag[K].tpe)).asInstanceOf[ClassTag[K]]
  implicit def classTagV = ClassTag(typeTag[V].mirror.runtimeClass(typeTag[V].tpe)).asInstanceOf[ClassTag[V]]

  val conf = new EtlConf(args)

  val scheme: Either[LayoutScheme, LayoutDefinition] = {
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

  val combinedModule = modules reduce (_ union _)

  lazy val inputPlugin =
    combinedModule
      .findSubclassOf[InputPlugin[I, V, M]]
      .find( _.suitableFor(conf.input(), conf.format()) )
      .getOrElse(sys.error(s"Unable to find input module of type '${conf.input()}' for format `${conf.format()}"))
    
  lazy val outputPlugin =
    combinedModule
      .findSubclassOf[OutputPlugin[K, V, M]]
      .find { _.suitableFor(conf.output()) }
      .getOrElse(sys.error(s"Unable to find output module of type '${conf.output()}'"))

  def load()(implicit sc: SparkContext): (LayerId, RDD[(I, V)] with Metadata[M]) = {
    val (zoom, rdd) = inputPlugin(conf.cache(), conf.crs(), scheme, conf.cellType.get, conf.inputProps)
    LayerId(conf.layerName(), zoom) -> rdd
  }

  def save(id: LayerId, rdd: RDD[(K, V)] with Metadata[M], method: KeyIndexMethod[K]): Unit = {
    val attributes = outputPlugin.attributes(conf.outputProps)
    def savePyramid(zoom: Int, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
      val currentId = id.copy( zoom = zoom)
      outputPlugin(currentId, rdd, method, conf.outputProps)
      /*if (conf.histogram()) {
        val histogram = rdd.histogram
        logger.info(s"Histogram for $currentId: ${histogram.toJson.compactPrint}")
        attributes.write(currentId, "histogram", histogram)
      }*/

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