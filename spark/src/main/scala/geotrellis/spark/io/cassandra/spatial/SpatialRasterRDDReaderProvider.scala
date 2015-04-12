package geotrellis.spark.io.cassandra.spatial

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

import geotrellis.index.zcurve._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cassandra._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.spark.connector._

object SpatialRasterRDDIndex {

  val rowIdRx = new Regex("""(\d+)_(\d+)""", "zoom", "zindex")

  def rowId(id: LayerId, key: SpatialKey): String = {
    val SpatialKey(col, row) = key
    val zindex = Z2(col, row)
    f"${id.zoom}%02d_${zindex.z}%019d"
  }
}

object SpatialRasterRDDReaderProvider extends RasterRDDReaderProvider[SpatialKey] {
  import SpatialRasterRDDIndex._

  def applyFilter(rdd: CassandraRDD[(String, ByteBuffer)], layerId: LayerId, filterSet: FilterSet[SpatialKey]): RDD[(String, ByteBuffer)] = {
    var tileBoundSet = false
    var rdds = ArrayBuffer[CassandraRDD[(String, ByteBuffer)]]()

    for (filter <- filterSet.filters) {
      filter match {
        case SpaceFilter(bounds) =>
          tileBoundSet = true

          for(row <- bounds.rowMin to bounds.rowMax) {
            val min = rowId(layerId, SpatialKey(bounds.colMin, row))
            val max = rowId(layerId, SpatialKey(bounds.colMax, row))
            rdds += rdd.where("id >= ? AND id <= ?", min, max)
          }
      }
    }
    
    if (!tileBoundSet) {
      val zmin = f"${layerId.zoom}%02d"
      val zmax = f"${layerId.zoom+1}%02d"
      rdds += rdd.where("id >= ? AND id < ?", zmin, zmax)
    }
    
    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]] // Coalesce afterwards?
  }

  def reader(instance: CassandraInstance, metaData: CassandraLayerMetaData, keyBounds: KeyBounds[SpatialKey])(implicit sc: SparkContext): FilterableRasterRDDReader[SpatialKey] =
    new FilterableRasterRDDReader[SpatialKey] {
      def read(layerId: LayerId, filters: FilterSet[SpatialKey]): RasterRDD[SpatialKey] = {
        val CassandraLayerMetaData(rasterMetaData, _, _, tileTable) = metaData

        val rdd: CassandraRDD[(String, ByteBuffer)] = 
          sc.cassandraTable[(String, ByteBuffer)](instance.keyspace, tileTable).select("id", "tile")

        val filteredRDD = applyFilter(rdd, layerId, filters)

        val tileRDD =
          filteredRDD.map { case (id, tilebytes) =>
                    val rowIdRx(zoom, zindex) = id
                    val Z2(col, row) = new Z2(zindex.toLong)
                    val bytes = new Array[Byte](tilebytes.capacity)
                    tilebytes.get(bytes, 0, bytes.length)
                    val tile = 
                      ArrayTile.fromBytes(
                        bytes,
                        rasterMetaData.cellType, 
                        rasterMetaData.tileLayout.tileCols,
                        rasterMetaData.tileLayout.tileRows
                      )

                    SpatialKey(col.toInt, row.toInt) -> tile.asInstanceOf[Tile]
                  }
    
        new RasterRDD(tileRDD, rasterMetaData)
      }
    }
}
