package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

trait RasterRDDWriter[K] {

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
