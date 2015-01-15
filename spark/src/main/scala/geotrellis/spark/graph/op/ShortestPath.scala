package geotrellis.spark.graph.op

import geotrellis.spark.graph._

import org.apache.spark.graphx._

import spire.syntax.cfor._

import reflect.ClassTag

object ShortestPath {

  def apply[K](graphRDD: GraphRDD[K], sources: Seq[VertexId])
    (implicit keyClassTag: ClassTag[K]): GraphRDD[K] = {
    val init = Double.MaxValue

    val g = graphRDD.mapVertices { case(id, v) =>
      if (sources.contains(id)) 0.0
      else init
    }

    def vertexProgram(id: VertexId, attr: Double, msg: Double): Double =
      math.min(attr, msg)

    def sendMessage(
      edge: EdgeTriplet[Double, Double]): Iterator[(VertexId, Double)] = {
      val newAttr = edge.srcAttr + edge.attr
      if (newAttr < edge.dstAttr) Iterator((edge.dstId, newAttr))
      else Iterator.empty
    }

    def mergeMsg(a: Double, b: Double): Double = math.min(a, b)

    val res = g.pregel(initialMsg = init, activeDirection = EdgeDirection.Out)(
      vertexProgram, sendMessage, mergeMsg)

    new GraphRDD(res, graphRDD.keysRDD, graphRDD.metaData)
  }

  def apply[K](graphRDD: GraphRDD[K], source: VertexId, dest: VertexId)
    (implicit keyClassTag: ClassTag[K]): Seq[Seq[VertexId]] = {
    val shortestPathWithRememberParentGraph =
      shortestPathWithRememberParent(graphRDD, source)

    accumulatePath(shortestPathWithRememberParentGraph, source, dest)
  }

  private def shortestPathWithRememberParent[K](
    graphRDD: GraphRDD[K],
    source: VertexId)(
    implicit keyClassTag: ClassTag[K]
  ): Graph[(Double, Seq[VertexId]), Double] = {
    val initValue = Double.MaxValue

    val g = graphRDD.mapVertices { case(id, v) =>
      if (source == id) (0.0, Seq[VertexId]())
      else (initValue, Seq[VertexId]())
    }

    def vertexProgram(
      id: VertexId,
      attr: (Double, Seq[VertexId]),
      msg: (Double, Seq[VertexId])
    ): (Double, Seq[VertexId]) = {
      val (oldCost, bestNeighborSet) = attr
      val (newCost, incomingIds) = msg

      if (newCost < oldCost) (newCost, incomingIds)
      else if (newCost == oldCost) (newCost, incomingIds ++ bestNeighborSet)
      else attr
    }

    def sendMessage(
      edge: EdgeTriplet[(Double, Seq[VertexId]), Double]
    ): Iterator[(VertexId, (Double, Seq[VertexId]))] = {
      val (srcAttr, _) = edge.srcAttr
      val (dstAttr, _) = edge.dstAttr

      val newAttr = srcAttr + edge.attr
      if (dstAttr > newAttr) Iterator((edge.dstId, (newAttr, Seq(edge.srcId))))
      else Iterator.empty
    }

    def mergeMessage(
      a: (Double, Seq[VertexId]),
      b: (Double, Seq[VertexId])): (Double, Seq[VertexId]) = {
      val (ac, as) = a
      val (bc, bs) = b

      if (bc < ac) b
      else if (ac < bc) a
      else (ac, as ++ bs)
    }

    val init = (initValue, Seq[VertexId]())

    g.pregel(initialMsg = init, activeDirection = EdgeDirection.Out)(
      vertexProgram, sendMessage, mergeMessage)
  }

  private def accumulatePath[K](
    graph: Graph[(Double, Seq[VertexId]), Double],
    source: Long,
    dest: Long
  ): Seq[Seq[VertexId]] = {

    val g = graph.subgraph { edge =>
      val (_, previous) = edge.srcAttr
      previous.contains(edge.dstId)
    }.mapVertices { case(id, (v, previous)) =>
        if (id == dest) Seq[Seq[VertexId]](Seq(id))
        else Seq[Seq[VertexId]]()
    }

    def vertexProgram(
      id: VertexId,
      attr: Seq[Seq[VertexId]],
      msg: Seq[Seq[VertexId]]
    ): Seq[Seq[VertexId]] =
      if (!msg.isEmpty) msg.map(id +: _) ++ attr
      else attr

    def sendMessage(
      edge: EdgeTriplet[Seq[Seq[VertexId]], Double]
    ): Iterator[(VertexId, Seq[Seq[VertexId]])] = {
      val paths = edge.srcAttr
      val destPaths = edge.dstAttr

      val srcId = edge.srcId
      val dstId = edge.dstId

      if (!paths.isEmpty) {
        var valid = true
        cfor(0)(_ < destPaths.size && valid, _ + 1) { i =>
          val destPath = destPaths(i)
          valid = destPath.size < 2 || destPath(1) != srcId
        }

        if (valid) Iterator((dstId, paths))
        else Iterator.empty
      } else
        Iterator.empty
    }

    def mergeMessage(
      a: Seq[Seq[VertexId]],
      b: Seq[Seq[VertexId]]): Seq[Seq[VertexId]] = a ++ b

    val init = Seq[Seq[VertexId]]()

    val res = g.pregel(initialMsg = init, activeDirection = EdgeDirection.Out)(
      vertexProgram, sendMessage, mergeMessage)

    val (_, paths) =
      res.vertices.filter { case (id, _) => id == source }.collect.head

    paths
  }

}
