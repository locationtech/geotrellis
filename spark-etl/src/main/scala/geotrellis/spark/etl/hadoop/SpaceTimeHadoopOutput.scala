package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{SpaceTimeKey, LayerId, RasterRDD}
import org.apache.hadoop.fs.Path

import scala.reflect._

class SpaceTimeHadoopOutput extends HadoopOutput {
  val key = classTag[SpaceTimeKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    implicit val sc = rdd.sparkContext
    HadoopLayerWriter[SpaceTimeKey, Tile, RasterRDD](new Path(props("path")), method.asInstanceOf[KeyIndexMethod[SpaceTimeKey]])
      .write(id, rdd.asInstanceOf[RasterRDD[SpaceTimeKey]])
  }
}