package geotrellis.spark.graph.op

import geotrellis.spark.graph._

import org.apache.spark.graphx._

import reflect.ClassTag

object ShortestPath {

  def apply[K](graphRDD: GraphRDD[K], sources: Seq[VertexId])
    (implicit keyClassTag: ClassTag[K]): GraphRDD[K] = {
    val verticesCount = graphRDD.vertices.count
    sources.foreach { id => if (id >= verticesCount)
      throw new IllegalArgumentException(s"Too large vertexId: $id")
    }

    val init = Double.MaxValue

    val edges = graphRDD.edges.flatMap(e => Seq(e, Edge(e.dstId, e.srcId, e.attr)))

    val vertices = graphRDD.vertices.map { case(id, (key, v)) =>
      if (sources.contains(id)) (id, (key, 0.0))
      else if (v.isNaN) (id, (key, v))
      else (id, (key, init))
    }

    val g = Graph(vertices, edges)

    def vertexProgram(id: VertexId, attr: (K, Double), msg: Double): (K, Double) = {
      val (key, before) = attr
      if (before.isNaN) (key, msg)
      else if (msg.isNaN) (key, before)
      else (key, math.min(before, msg))
    }

    def sendMessage(
      edge: EdgeTriplet[(K, Double), Double]): Iterator[(VertexId, Double)] = {
      val (srcKey, srcAttr) = edge.srcAttr
      if (srcAttr.isNaN) Iterator.empty
      else {
        val newAttr = srcAttr + edge.attr
        val (_, destAttr) = edge.dstAttr
        if (destAttr > newAttr) Iterator((edge.dstId, newAttr))
        else Iterator.empty
      }
    }

    def mergeMsg(a: Double, b: Double): Double =
      if (a.isNaN) b
      else if (b.isNaN) a
      else math.min(a, b)

    val res = g.pregel(init)(vertexProgram, sendMessage, mergeMsg)

    new GraphRDD(res, graphRDD.metaData)
  }

  def apply[K](graphRDD: GraphRDD[K], source: VertexId, goal: VertexId)
    (implicit keyClassTag: ClassTag[K]): Seq[Seq[VertexId]] = {
    val verticesCount = graphRDD.vertices.count
    if (source >= verticesCount)
      throw new IllegalArgumentException(s"Too large source vertexId: $source")
    if (goal >= verticesCount)
      throw new IllegalArgumentException(s"Too large goal vertexId: $goal")

    val shortestPathWithRememberParentGraph =
      shortestPathWithRememberParent(graphRDD, source, goal)

    // Now we want to extract the friggin path too then it's GG
    // Need to pregel this crap too? NOPE accum under the way

    accumulatePath(shortestPathWithRememberParentGraph)

    ???
  }

  private def shortestPathWithRememberParent[K](
    graphRDD: GraphRDD[K],
    source: VertexId,
    goal: VertexId)(
    implicit keyClassTag: ClassTag[K]
  ): Graph[(K, (Double, Set[VertexId], Set[VertexId])), Double] = {
    val initValue = Double.MaxValue

    val init = (initValue, Set[VertexId](), Set[VertexId]())

    val edges = graphRDD.edges.flatMap(e => Seq(e, Edge(e.dstId, e.srcId, e.attr)))

    val vertices = graphRDD.vertices.map { case(id, (key, v)) =>
      if (source == id) (id, (key, (0.0, Set[VertexId](), Set(id))))
      else if (v.isNaN) (id, (key, (v, Set[VertexId](), Set(id))))
      else (id, (key, (initValue, Set[VertexId](), Set(id))))
    }

    val g = Graph(vertices, edges)

    def vertexProgram(
      id: VertexId,
      attr: (K, (Double, Set[VertexId], Set[VertexId])),
      msg: (Double, Set[VertexId], Set[VertexId])
    ): (K, (Double, Set[VertexId], Set[VertexId])) = {
      val (key, (oldCost, bestNeighborSet, id)) = attr
      val (newCost, _, incomingIds) = msg

      if (oldCost.isNaN || newCost < oldCost)
        (key, (newCost, incomingIds, id))
      else if (newCost == oldCost)
        (key, (newCost, incomingIds ++ bestNeighborSet, id))
      else
        attr
    }

    def sendMessage(
      edge: EdgeTriplet[(K, (Double, Set[VertexId], Set[VertexId])), Double]
    ):  Iterator[(VertexId, (Double, Set[VertexId], Set[VertexId]))] = {
      val (srcKey, (srcAttr, _, froms)) = edge.srcAttr
      if (srcAttr.isNaN) Iterator.empty
      else {
        val newAttr = srcAttr + edge.attr
        val (_, (destAttr, _, _)) = edge.dstAttr
        if (destAttr > newAttr) Iterator((edge.dstId, (newAttr, Set(), froms)))
        else Iterator.empty
      }
    }

    def mergeMessage(
      a: (Double, Set[VertexId], Set[VertexId]),
      b: (Double, Set[VertexId], Set[VertexId])
    ): (Double, Set[VertexId], Set[VertexId]) = {
      val (aCost, aSet, aIds) = a
      val (bCost, bSet, bIds) = b

      if (aCost.isNaN || bCost < aCost) a
      else if (bCost.isNaN || aCost < bCost) b
      else (aCost, Set(), aIds ++ bIds)
    }

    g.pregel(init)(vertexProgram, sendMessage, mergeMessage)
  }

  private def accumulatePath[K](
    graph: Graph[(K, (Double, Set[VertexId], Set[VertexId])), Double]
  ): Seq[Seq[VertexId]] = ??? // Create the paths from dest to src.

}
