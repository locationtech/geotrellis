package geotrellis.spark.graph

import geotrellis.spark._

import org.apache.spark.rdd.RDD

import org.apache.spark.graphx.Graph

import reflect.ClassTag

class GraphRDD[K: ClassTag](
  val graph: Graph[Double, Double],
  val keysRDD: RDD[K],
  val metaData: RasterMetaData) extends Serializable
