package geotrellis.spark.etl.accumulo

import geotrellis.spark._
import geotrellis.spark.io.accumulo.AccumuloRasterCatalog
import geotrellis.spark.io.index.KeyIndexMethod
import scala.reflect._

class SpaceTimeAccumuloSink extends AccumuloSink {
  val key = classTag[SpaceTimeKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    AccumuloRasterCatalog()(getInstance(props), rdd.sparkContext)
      .writer[SpaceTimeKey](method.asInstanceOf[KeyIndexMethod[SpaceTimeKey]], props("table"))
      .write(id, rdd.asInstanceOf[RasterRDD[SpaceTimeKey]])
  }
}
