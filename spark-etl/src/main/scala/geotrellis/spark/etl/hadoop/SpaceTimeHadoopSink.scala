package geotrellis.spark.etl.hadoop

import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{SpaceTimeKey, LayerId, RasterRDD}
import org.apache.hadoop.fs.Path

import scala.reflect._

class SpaceTimeHadoopSink extends HadoopSink {
  val key = classTag[SpaceTimeKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    HadoopRasterCatalog(new Path(props("path")))(rdd.sparkContext)
      .writer[SpaceTimeKey](method.asInstanceOf[KeyIndexMethod[SpaceTimeKey]], props.getOrElse("subDir",""), props.getOrElse("clobber", "false").toBoolean)
      .write(id, rdd.asInstanceOf[RasterRDD[SpaceTimeKey]])
  }
}