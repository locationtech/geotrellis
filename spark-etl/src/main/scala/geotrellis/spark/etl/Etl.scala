package geotrellis.spark.etl

import geotrellis.spark.ingest.Pyramid
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{SpatialComponent, RasterRDD, LayerId}
import geotrellis.spark.tiling.{LayoutLevel, ZoomedLayoutScheme}
import com.google.inject._
import org.apache.spark.SparkContext
import scala.reflect._
import scala.collection.JavaConverters._

object Etl {
  def apply[K: ClassTag: SpatialComponent](args: Seq[String]): Etl[K] = {
    new Etl(args, s3.S3Module, hadoop.HadoopModule)
  }
}

case class Etl[K: ClassTag: SpatialComponent](args: Seq[String], modules: Module*) {
  val conf = new EtlConf(args)

  val (ingestPlugin, outputPlugin) = {
    val injector = Guice.createInjector(modules: _*)

    val ingest = injector
      .findBindingsByType(new TypeLiteral[IngestPlugin]{})
      .asScala
      .map { _.getProvider.get }
      .find { _.suitableFor(conf.ingest(), conf.format(), classTag[K]) }
      .getOrElse(sys.error(s"Unable to find ingest module of type '${conf.ingest()}' for format `${conf.format()} and key ${classTag[K]}"))

    ingest.validate(conf.ingestProps)

    val output = injector
      .findBindingsByType(new TypeLiteral[SinkPlugin]{})
      .asScala
      .map { _.getProvider.get }
      .find { _.suitableFor(conf.output(), classTag[K]) }
      .getOrElse(sys.error(s"Unable to find output module of type '${conf.output()}' and key ${classTag[K]}"))

    output.validate(conf.outputProps)

    (ingest, output)
  }

  def ingest()(implicit sc: SparkContext): (LayerId, RasterRDD[K]) = {
    val (level, rdd) = ingestPlugin[K](conf.cache(), conf.crs(), ZoomedLayoutScheme(conf.tileSize()), conf.ingestProps)
    LayerId(conf.layerName(), level.zoom) -> rdd
  }

  def save(id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K]): Unit = {
    def savePyramid(level: LayoutLevel, rdd: RasterRDD[K]): Unit = {
      outputPlugin(id.copy( zoom = level.zoom), rdd, method, conf.outputProps)
      if (level.zoom > 1) {
        val (nextLevel, nextRdd) = Pyramid.up(rdd, level, ZoomedLayoutScheme(conf.tileSize()))
        savePyramid(nextLevel, nextRdd)
      }
    }

    if (conf.pyramid())
      savePyramid(LayoutLevel(id.zoom, rdd.metaData.tileLayout), rdd)
    else
      outputPlugin(id, rdd, method, conf.outputProps)
  }
}