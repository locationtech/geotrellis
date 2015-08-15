package geotrellis.spark.etl.hadoop

import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{LayerId, RasterRDD, SpatialKey}
import scala.reflect._
import org.apache.hadoop.fs.Path

class SpatialHadoopOutput extends HadoopOutput {
  val key = classTag[SpatialKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    HadoopRasterCatalog(new Path(props("path")))(rdd.sparkContext)
      .writer[SpatialKey](method.asInstanceOf[KeyIndexMethod[SpatialKey]], props.getOrElse("subDir",""), props.getOrElse("clobber", "false").toBoolean)
      .write(id, rdd.asInstanceOf[RasterRDD[SpatialKey]])
  }
}