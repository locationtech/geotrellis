package geotrellis.spark.graphx.lib

import geotrellis.spark.graphx._

import org.apache.spark.graphx._

import reflect.ClassTag

object ShortestPath {

  private val init = Double.MaxValue

  def apply[K](graphRDD: GraphRDD[K], sources: Seq[VertexId])
    (implicit keyClassTag: ClassTag[K]): GraphRDD[K] = {
    val verticesCount = graphRDD.vertices.count
    sources.foreach(id => if (id >= verticesCount)
      throw new IllegalArgumentException(s"Too large vertexId: $id")
    )

    // TODO: Massive memory eater
    val edges = graphRDD.edges.flatMap(e => Seq(e, Edge(e.dstId, e.srcId, e.attr)))

    val vertices = graphRDD.vertices.map { case(id, (key, v)) =>
      if (sources.contains(id)) (id, (key, 0.0))
      else if (v.isNaN) (id, (key, v))
      else (id, (key, init))
    }

    val g = Graph(vertices, edges)

    def vertexProgram(id: VertexId, attr: (K, Double), msg: Double): (K, Double) = {
      val (key, before) = attr
      (key, math.min(before, msg))
    }

    def sendMessage(
      edge: EdgeTriplet[(K, Double), Double]):  Iterator[(VertexId, Double)] = {
      val (srcKey, srcAttr) = edge.srcAttr
      if (srcAttr.isNaN) Iterator.empty
      else {
        val newAttr = srcAttr + edge.attr
        val (_, destAttr) = edge.dstAttr
        if (destAttr > newAttr) Iterator((edge.dstId, newAttr))
        else Iterator.empty
      }
    }

    val res = g.pregel(initialMsg = init, activeDirection = EdgeDirection.Either)(
      vertexProgram, sendMessage, math.min)

    new GraphRDD(res, graphRDD.metaData)
  }

}
