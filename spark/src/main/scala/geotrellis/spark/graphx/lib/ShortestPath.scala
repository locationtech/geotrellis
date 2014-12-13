package geotrellis.spark.graphx.lib

import geotrellis.spark.graphx._

import org.apache.spark.graphx._

import reflect.ClassTag

object ShortestPath {

  private val init = Int.MaxValue

  def apply[K](graphRDD: GraphRDD[K], sources: Seq[VertexId])
    (implicit keyClassTag: ClassTag[K]): GraphRDD[K] = {
    val g = graphRDD.mapVertices { case(id, (key, v)) =>
      if (sources.contains(id)) (key, 0) else (key, init)
    }

    def vertexProgram(id: VertexId, attr: (K, Int), msg: Int): (K, Int) = {
      val (key, before) = attr
      (key, math.min(before, msg))
    }

    def sendMessage(
      edge: EdgeTriplet[(K, Int), Int]):  Iterator[(VertexId, Int)] = {
      val (srcKey, srcAttr) = edge.srcAttr
      val newAttr = srcAttr + edge.attr
      val (_, destAttr) = edge.dstAttr
      if (destAttr != newAttr) Iterator((edge.dstId, newAttr))
      else Iterator.empty
    }

    val res = g.pregel(initialMsg = init, activeDirection = EdgeDirection.Both)(
      vertexProgram, sendMessage, math.min)

    new GraphRDD(res, graphRDD.metaData)
  }

}
