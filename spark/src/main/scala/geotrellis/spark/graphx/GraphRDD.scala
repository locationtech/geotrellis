package geotrellis.spark.graphx

import geotrellis.spark._

import org.apache.spark.graphx._

import reflect.ClassTag

class GraphRDD[K: ClassTag](
  val graph: Graph[(K, Int), Int],
  val metaData: RasterMetaData)
