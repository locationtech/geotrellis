package geotrellis.spark.io.cassandra

import java.nio.ByteBuffer;

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

import geotrellis.raster.{Tile, ArrayTile}
import geotrellis.spark._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.spark.connector.cql.CassandraConnector

//import org.locationtech.curve.zcurve.Z2
import geotrellis.index.zcurve._

object RasterCassandraDriver extends CassandraDriver[SpatialKey] {

  val rowIdRx = new Regex("""(\d+)_(\d+)""", "zoom", "zindex")

  def rowId(id: LayerId, key: SpatialKey): String = {
    val SpatialKey(col, row) = key
    val zindex = Z2(col, row)
    f"${id.zoom}%02d_${zindex.z}%019d"
  }

  def loadTile(connector: CassandraConnector)(id: LayerId, metaData: RasterMetaData, table: String, key: SpatialKey): Tile = ???

  def decode(rdd: RDD[(String, ByteBuffer)], rasterMetaData: RasterMetaData): RasterRDD[SpatialKey] = {
    val tileRDD =
      rdd.map { case (id, tilebytes) =>
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

  def applyFilter(rdd: CassandraRDD[(String, ByteBuffer)], layerId: LayerId, filterSet: FilterSet[SpatialKey]): CassandraRDD[(String, ByteBuffer)] = {
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
    
    rdd.context.union(rdds.toSeq).asInstanceOf[CassandraRDD[(String, ByteBuffer)]] // Coalesce afterwards?
  }
}
