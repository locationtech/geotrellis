package geotrellis.spark.etl.accumulo

import geotrellis.spark._
import geotrellis.spark.io.accumulo.AccumuloRasterCatalog
import geotrellis.spark.io.index.KeyIndexMethod
import scala.reflect._

class SpatialAccumuloOutput extends AccumuloOutput {
  val key = classTag[SpatialKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    AccumuloRasterCatalog()(getInstance(props), rdd.sparkContext)
      .writer[SpatialKey](method.asInstanceOf[KeyIndexMethod[SpatialKey]], props("table"))
      .write(id, rdd.asInstanceOf[RasterRDD[SpatialKey]])
  }
}

