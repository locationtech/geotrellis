package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.client.mapreduce.InputFormatBase
import org.apache.accumulo.core.data.{Key, Value, Range => ARange}
import org.apache.accumulo.core.util.{Pair => APair}

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.collection.JavaConversions._

object SpatialRasterRDDReader extends RasterRDDReader[SpatialKey] {

  def getCube(
    job: Job,
    layerId: LayerId,
    keyBounds: KeyBounds[SpatialKey],
    keyIndex: KeyIndex[SpatialKey]
  )(implicit sc: SparkContext): RDD[(Key, Value)] = {
    val ranges = 
      keyIndex.indexRanges(keyBounds) 
      .map { case (min: Long, max: Long) =>
        new ARange(rowId(layerId,min), true, rowId(layerId,max), true)
      }

    InputFormatBase.setRanges(job, ranges)
    InputFormatBase.fetchColumns(job, new APair(new Text(layerId.name), null: Text) :: Nil)   

    sc.newAPIHadoopRDD(
      job.getConfiguration,
      classOf[BatchAccumuloInputFormat],
      classOf[Key],
      classOf[Value]
    )
  }
}
