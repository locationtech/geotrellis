package geotrellis.spark.etl.accumulo

import geotrellis.proj4.CRS
import geotrellis.spark._
import geotrellis.spark.io.accumulo.AccumuloRasterCatalog
import geotrellis.spark.tiling.{LayoutLevel, LayoutScheme}
import geotrellis.vector.Extent
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import scala.reflect._

class SpaceTimeAccumuloInput extends AccumuloInput {

  def key = classTag[SpaceTimeKey]

  def apply[K](lvl: StorageLevel, crs: CRS, scheme: LayoutScheme, props: Map[String, String])(implicit sc: SparkContext) = {
    val (id, bbox) = parse(props)
    val catalog = AccumuloRasterCatalog()(getInstance(props), sc)

    val rdd = bbox match {
      case Some(extent) => catalog.query[SpaceTimeKey](id).where(Intersects(extent)).toRDD
      case None => catalog.read[SpaceTimeKey](id)
    }
    LayoutLevel(id.zoom, rdd.metaData.tileLayout) -> rdd.asInstanceOf[RasterRDD[K]]
  }

}
