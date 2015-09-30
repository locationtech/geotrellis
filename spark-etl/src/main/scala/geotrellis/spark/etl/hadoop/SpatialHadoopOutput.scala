package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{SpatialKey, LayerId, RasterRDD}
import scala.reflect._
import org.apache.hadoop.fs.Path

class SpatialHadoopOutput extends HadoopOutput {
  val key = classTag[SpatialKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    implicit val sc = rdd.sparkContext
    HadoopLayerWriter[SpatialKey, Tile, RasterRDD](new Path(props("path")), method.asInstanceOf[KeyIndexMethod[SpatialKey]])
      .write(id, rdd.asInstanceOf[RasterRDD[SpatialKey]])
  }
}