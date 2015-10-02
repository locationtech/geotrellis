package geotrellis.spark.etl

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.raster.RasterExtent
import geotrellis.spark.ingest._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark._
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import geotrellis.spark.op.stats._
import geotrellis.raster.io.json._
import org.apache.spark.SparkContext
import scala.reflect._
import spray.json._

import scala.reflect.runtime.universe._

object Etl {
  val defaultModules = Array(s3.S3Module, hadoop.HadoopModule, accumulo.AccumuloModule)

  def apply[K: TypeTag: SpatialComponent](args: Seq[String]): Etl[K] =
    apply(args, defaultModules)

  def apply[K: TypeTag: SpatialComponent](args: Seq[String], modules: Seq[TypedModule]): Etl[K] =
    new Etl[K](args, modules)

}

class Etl[K: TypeTag: SpatialComponent](args: Seq[String], modules: Seq[TypedModule]) extends LazyLogging {
  implicit def classTagK = ClassTag(typeTag[K].mirror.runtimeClass( typeTag[K].tpe )).asInstanceOf[ClassTag[K]]

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
      .findSubclassOf[InputPlugin[K]]
      .find( _.suitableFor(conf.input(), conf.format()) )
      .getOrElse(sys.error(s"Unable to find input module of type '${conf.input()}' for format `${conf.format()}"))
    
  lazy val outputPlugin =
    combinedModule
      .findSubclassOf[OutputPlugin]
      .find { _.suitableFor(conf.output(), classTagK) }
      .getOrElse(sys.error(s"Unable to find output module of type '${conf.output()}'"))

  def load()(implicit sc: SparkContext): (LayerId, RasterRDD[K]) = {
    val (zoom, rdd) = inputPlugin(conf.cache(), conf.crs(), scheme, conf.cellType.get, conf.inputProps)
    LayerId(conf.layerName(), zoom) -> rdd
  }

  def save(id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K]): Unit = {
    val attributes = outputPlugin.attributes(conf.outputProps)
    def savePyramid(zoom: Int, rdd: RasterRDD[K]): Unit = {
      val currentId = id.copy( zoom = zoom)
      outputPlugin(currentId, rdd, method, conf.outputProps)
      if (conf.histogram()) {
        val histogram = rdd.histogram
        logger.info(s"Histogram for $currentId: ${histogram.toJson.compactPrint}")
        attributes.write(currentId, "histogram", histogram)
      }

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