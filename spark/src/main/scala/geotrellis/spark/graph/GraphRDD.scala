package geotrellis.spark.graph

import geotrellis.spark._

import org.apache.spark.graphx._

import reflect.ClassTag

class GraphRDD[K: ClassTag](
  val graph: Graph[(K, Double), Double],
  val metaData: RasterMetaData) extends Serializable
