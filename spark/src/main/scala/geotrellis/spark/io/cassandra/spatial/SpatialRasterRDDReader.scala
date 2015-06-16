package geotrellis.spark.io.cassandra.spatial

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

import geotrellis.spark.io.index.zcurve._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import com.datastax.spark.connector.rdd.CassandraRDD
import com.datastax.spark.connector._

object SpatialRasterRDDReader extends RasterRDDReader[SpatialKey] {

  def applyFilter(rdd: CassandraRDD[(String, ByteBuffer)],
    layerId: LayerId,
    queryKeyBounds: Seq[KeyBounds[SpatialKey]],
    keyBounds: KeyBounds[SpatialKey],
    index: KeyIndex[SpatialKey]
  ): RDD[(String, ByteBuffer)] = {

    var tileBoundSet = false
    val rdds = ArrayBuffer[CassandraRDD[(String, ByteBuffer)]]()

    val ranges = queryKeyBounds.map{ index.indexRanges(_) }.flatten
    logInfo(s"queryKeyBounds has ${ranges.length} ranges")

    for (range <- ranges) {
      logInfo(s"range has ${range.toString()} ")
    }

    for ( bounds <- queryKeyBounds ) {
      tileBoundSet = true

      /*
      KeyBounds(minKey, maxKey of K Type)

      SpatialKey
      def _1 = col
      def _2 = row
       */
      for(row <- bounds.minKey._2 to bounds.maxKey._2) {
        val min = index.toIndex(SpatialKey(bounds.minKey._1, row))
        logInfo(s"index.toIndex(SpatialKey(${bounds.minKey._1}, ${row}))")
        val max = index.toIndex(SpatialKey(bounds.maxKey._1, row))
        logInfo(s"index.toIndex(SpatialKey(${bounds.maxKey._1}, ${row}))")
        rdds += rdd.where("zoom = ? AND indexer >= ? AND indexer <= ?", layerId.zoom, min, max)
      }

    }

    if (!tileBoundSet) {
      rdds += rdd.where("zoom = ?", layerId.zoom)
    }

    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]]
  }
}
