package geotrellis.spark.io.cassandra

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import geotrellis.spark.io.index._

trait RasterRDDWriter[K] extends LazyLogging {

  def createTableIfNotExists(tileTable: String)(implicit session: CassandraSession): Unit

  def saveRasterRDD(
    layerId: LayerId,
    raster: RasterRDD[K], 
    kIndex: KeyIndex[K],
    tileTable: String)(implicit session: CassandraSession): Unit

  def write(
    layerMetaData: CassandraLayerMetaData, 
    keyBounds: KeyBounds[K], 
    index: KeyIndex[K]
  )(layerId: LayerId, raster: RasterRDD[K])(implicit session: CassandraSession): Unit = {
    val tileTable = layerMetaData.tileTable        
    createTableIfNotExists(tileTable)
    saveRasterRDD(layerId, raster, index, tileTable)
  }
}
