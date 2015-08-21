package geotrellis.spark.io.cassandra

import java.nio.ByteBuffer

import com.datastax.spark.connector._
import com.datastax.spark.connector.rdd.CassandraRDD
import com.typesafe.scalalogging.slf4j._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec, TupleCodec}
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

abstract class RasterRDDReader[K: AvroRecordCodec: ClassTag] extends LazyLogging {

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

    val readCodec = KryoWrapper(TupleCodec[K, Tile])
    val CassandraLayerMetaData(_, rasterMetaData, tileTable) = metaData

    val rdd: CassandraRDD[(String, ByteBuffer)] =
      sc.cassandraTable[(String, ByteBuffer)](session.keySpace, tileTable).select("reverse_index", "value")

    val filteredRDD = {
      if (queryKeyBounds.isEmpty) {
         rdd.where("zoom = ?", layerId.zoom)
      } else {
        applyFilter(rdd, layerId, queryKeyBounds, keyBounds, index)
      }
    }

    val tileRDD =
      filteredRDD.map { case (_, value) =>
        val byteArray = new Array[Byte](value.remaining)
        value.get(byteArray, 0, byteArray.length)
        val (key, tile) = AvroEncoder.fromBinary(byteArray)(readCodec.value)
        (key, tile: Tile)
      }

    new RasterRDD(tileRDD, rasterMetaData)
  }
}
