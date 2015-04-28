package geotrellis.spark.io.cassandra.spatial

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

import geotrellis.index.zcurve._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.spark.connector._

object SpatialRasterRDDReaderProvider extends RasterRDDReaderProvider[SpatialKey] {

  def applyFilter(rdd: CassandraRDD[(String, ByteBuffer)], layerId: LayerId, filterSet: FilterSet[SpatialKey], index: KeyIndex[SpatialKey]): RDD[(String, ByteBuffer)] = {
    var tileBoundSet = false
    val rdds = ArrayBuffer[CassandraRDD[(String, ByteBuffer)]]()

    for (filter <- filterSet.filters) {
      filter match {
        case SpaceFilter(bounds) =>
          tileBoundSet = true

          for(row <- bounds.rowMin to bounds.rowMax) {
            val min = index.toIndex(SpatialKey(bounds.colMin, row))
            val max = index.toIndex(SpatialKey(bounds.colMax, row))
            rdds += rdd.where("zoom = ? AND indexer >= ? AND indexer <= ?", layerId.zoom, min, max)
          }
      }
    }
    
    if (!tileBoundSet) {
      rdds += rdd.where("zoom = ?", layerId.zoom)
    }
    
    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]] // Coalesce afterwards?
  }

  def reader(metaData: CassandraLayerMetaData, keyBounds: KeyBounds[SpatialKey], index: KeyIndex[SpatialKey])(implicit session: CassandraSession, sc: SparkContext): FilterableRasterRDDReader[SpatialKey] =
    new FilterableRasterRDDReader[SpatialKey] {
      def read(layerId: LayerId, filters: FilterSet[SpatialKey]): RasterRDD[SpatialKey] = {
        val CassandraLayerMetaData(rasterMetaData, _, _, tileTable) = metaData

        val rdd: CassandraRDD[(String, ByteBuffer)] = 
          sc.cassandraTable[(String, ByteBuffer)](session.keySpace, tileTable).select("reverse_index", "value")

        val filteredRDD = applyFilter(rdd, layerId, filters, index)

        val tileRDD =
          filteredRDD.map { case (_, value) =>
            val (key, tileBytes) = KryoSerializer.deserialize[(SpatialKey, Array[Byte])](value)
            val tile =
              ArrayTile.fromBytes(
                tileBytes,
                rasterMetaData.cellType,
                rasterMetaData.tileLayout.tileCols,
                rasterMetaData.tileLayout.tileRows
              )

            (key, tile: Tile)
          }
    
        new RasterRDD(tileRDD, rasterMetaData)
      }
    }
}
