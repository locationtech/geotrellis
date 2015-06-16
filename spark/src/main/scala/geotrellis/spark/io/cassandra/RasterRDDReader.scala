package geotrellis.spark.io.cassandra

import java.nio.ByteBuffer

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.zcurve._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.spark.connector._

import scala.reflect.ClassTag

abstract class RasterRDDReader[K: ClassTag] {

def applyFilter(
    rdd: CassandraRDD[(String, ByteBuffer)],
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[K]],
    keyBounds: KeyBounds[K],
    index: KeyIndex[K]
  ): RDD[(String, ByteBuffer)]

  def read(metaData: CassandraLayerMetaData,
    keyBounds: KeyBounds[K],
    index: KeyIndex[K]
  )(layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[K]]
  )(
    implicit session: CassandraSession,
    sc: SparkContext
  ): RasterRDD[K] = {
    val CassandraLayerMetaData(_, rasterMetaData, tileTable) = metaData

    val rdd: CassandraRDD[(String, ByteBuffer)] =
      sc.cassandraTable[(String, ByteBuffer)](session.keySpace, tileTable).select("reverse_index", "value")

// TODO use queryKeyBounds.map ..
    val filteredRDD = {
      // if (keyBounds.isEmpty) {
      //   rdd.where("zoom = ?", layerId.zoom)
      //} else {
        applyFilter(rdd, layerId, queryKeyBounds, keyBounds, index)
      // }
    }

    val tileRDD =
      filteredRDD.map { case (_, value) =>
        // seems not ideal? Cassandra resultset java.nio.nytebuffer to ByteArray conversions
        val byteArray = new Array[Byte](value.remaining)
        value.get(byteArray, 0, byteArray.length)

        val (key, tileBytes) = KryoSerializer.deserialize[(K, Array[Byte])](byteArray)
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
