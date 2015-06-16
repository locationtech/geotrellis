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

    // TODO alternative? use queryKeyBounds.map ..
    queryKeyBounds.map{ subKeyBound =>
      tileBoundSet = true

      val keymin = subKeyBound._1
      val keymax = subKeyBound._2

      val rowmin = keymin.row
      val rowmax = keymax.row

      val colmin = keymin.col
      val colmax = keymax.col

      for(row <- rowmin to rowmax) {
        val min = index.toIndex(SpatialKey(colmin, row))
        val max = index.toIndex(SpatialKey(colmax, row))
        rdds += rdd.where("zoom = ? AND indexer >= ? AND indexer <= ?", layerId.zoom, min, max)
      }
    }

    if (!tileBoundSet) {
      rdds += rdd.where("zoom = ?", layerId.zoom)
    }

    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]]
  }
}
