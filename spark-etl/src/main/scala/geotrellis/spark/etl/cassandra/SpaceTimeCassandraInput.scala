package geotrellis.spark.etl.cassandra

import geotrellis.proj4.CRS
import geotrellis.spark.io.cassandra.CassandraRasterCatalog
import geotrellis.spark.tiling.{LayoutLevel, LayoutScheme}
import geotrellis.spark.{SpaceTimeKey, Intersects, RasterRDD}
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel

import scala.reflect._

class SpaceTimeCassandraInput extends CassandraInput {
   def key = classTag[SpaceTimeKey]

   def apply[K](lvl: StorageLevel, crs: CRS, scheme: LayoutScheme, props: Map[String, String])(implicit sc: SparkContext) = {

     val (id, bbox) = parse(props)
     implicit val session = getSession(props)
     val catalog = CassandraRasterCatalog()

     val rdd = bbox match {
       case Some(extent) => catalog.query[SpaceTimeKey](id).where(Intersects(extent)).toRDD
       case None => catalog.read[SpaceTimeKey](id)
     }
     LayoutLevel(id.zoom, rdd.metaData.tileLayout) -> rdd.asInstanceOf[RasterRDD[K]]
   }
 }
