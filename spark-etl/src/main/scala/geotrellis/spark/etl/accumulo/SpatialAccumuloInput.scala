package geotrellis.spark.etl.accumulo

import geotrellis.proj4.CRS
import geotrellis.spark._
import geotrellis.spark.io.accumulo.AccumuloRasterCatalog
import geotrellis.spark.tiling.{LayoutLevel, LayoutScheme}
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import scala.reflect._

class SpatialAccumuloInput extends AccumuloInput {

  def key = classTag[SpatialKey]

  def apply[K](lvl: StorageLevel, crs: CRS, scheme: LayoutScheme, props: Map[String, String])(implicit sc: SparkContext) = {
    val (id, bbox) = parse(props)
    val catalog = AccumuloRasterCatalog()(getInstance(props), sc)

    val rdd = bbox match {
      case Some(extent) => catalog.query[SpatialKey](id).where(Intersects(extent)).toRDD
      case None => catalog.read[SpatialKey](id)
    }
    id.zoom -> rdd.asInstanceOf[RasterRDD[K]]
  }

}
