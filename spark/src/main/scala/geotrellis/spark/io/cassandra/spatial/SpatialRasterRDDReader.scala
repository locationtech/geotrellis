package geotrellis.spark.io.cassandra.spatial

import java.nio.ByteBuffer
import geotrellis.spark.io.avro.KeyCodecs._

import com.datastax.spark.connector.rdd.CassandraRDD
import geotrellis.spark._
import geotrellis.spark.io.cassandra._
import geotrellis.spark.io.index._
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer

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
    logger.debug(s"queryKeyBounds has ${ranges.length} ranges")

    for (range <- ranges) {
      logger.debug(s"range has ${range.toString()} ")
    }

    for ( bounds <- queryKeyBounds ) {
      tileBoundSet = true

      for(row <- bounds.minKey._2 to bounds.maxKey._2) {
        val min = index.toIndex(SpatialKey(bounds.minKey._1, row))
        logger.debug(s"index.toIndex(SpatialKey(${bounds.minKey._1}, ${row}))")
        val max = index.toIndex(SpatialKey(bounds.maxKey._1, row))
        logger.debug(s"index.toIndex(SpatialKey(${bounds.maxKey._1}, ${row}))")
        rdds += rdd.where("zoom = ? AND indexer >= ? AND indexer <= ?", layerId.zoom, min, max)
      }

    }

    if (!tileBoundSet) {
      rdds += rdd.where("zoom = ?", layerId.zoom)
    }

    // TODO: eventually find a more performant approach than union of thousands of RDDs
    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]]
  }
}
