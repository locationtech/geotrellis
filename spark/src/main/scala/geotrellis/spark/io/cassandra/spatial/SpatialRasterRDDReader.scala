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

  def applyFilter(rdd: CassandraRDD[(String, ByteBuffer)], layerId: LayerId, filterSet: FilterSet[SpatialKey], keyBounds: KeyBounds[SpatialKey], index: KeyIndex[SpatialKey]): RDD[(String, ByteBuffer)] = {
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

    rdd.context.union(rdds.toSeq).asInstanceOf[RDD[(String, ByteBuffer)]]
  }
}
