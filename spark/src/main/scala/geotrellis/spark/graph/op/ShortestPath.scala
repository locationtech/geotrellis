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
      (key, math.min(before, msg))
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

  def apply[K](graphRDD: GraphRDD[K], source: VertexId, dest: VertexId)
    (implicit keyClassTag: ClassTag[K]): Set[Seq[VertexId]] = {
    val verticesCount = graphRDD.vertices.count
    if (source >= verticesCount)
      throw new IllegalArgumentException(s"Too large source vertexId: $source")
    if (dest >= verticesCount)
      throw new IllegalArgumentException(s"Too large dest vertexId: $dest")

    val shortestPathWithRememberParentGraph =
      shortestPathWithRememberParent(graphRDD, source)

    accumulatePath(shortestPathWithRememberParentGraph, source, dest)
  }

  private def shortestPathWithRememberParent[K](
    graphRDD: GraphRDD[K],
    source: VertexId)(
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

      if (newCost < oldCost)
        (key, (newCost, incomingIds, id))
      else if (newCost == oldCost)
        (key, (newCost, incomingIds ++ bestNeighborSet, id))
      else
        attr
    }

    def sendMessage(
      edge: EdgeTriplet[(K, (Double, Set[VertexId], Set[VertexId])), Double]
    ): Iterator[(VertexId, (Double, Set[VertexId], Set[VertexId]))] = {
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
    graph: Graph[(K, (Double, Set[VertexId], Set[VertexId])), Double],
    source: Long,
    dest: Long
  ): Set[Seq[VertexId]] = {

    val vertices = graph.vertices.map { case(id, (key, (v, later, ids))) =>
      if (id == dest)
        (id, (key, (later, Set[Seq[VertexId]](Seq(id)), Set[VertexId](), true)))
      else
        (id, (key, (later, Set[Seq[VertexId]](), Set[VertexId](), false)))
    }

    val g = Graph(vertices, graph.edges)

    def vertexProgram(
      id: VertexId,
      attr: (K, (Set[VertexId], Set[Seq[VertexId]], Set[VertexId], Boolean)),
      msg: (Set[VertexId], Set[Seq[VertexId]], Boolean)
    ): (K, (Set[VertexId], Set[Seq[VertexId]], Set[VertexId], Boolean)) = {
      val (key, (prev, paths, takenFrom, shouldSend)) = attr
      val (from, prevPaths, shouldAccept) = msg

      if (shouldAccept) {
        val newPaths =
          if (prevPaths.isEmpty) paths ++ Set(Seq(id))
          else prevPaths.map(id +: _) ++ paths

        (key, (prev, newPaths, takenFrom ++ from, true))
      } else
        (key, (prev, paths, takenFrom, shouldSend))
    }

    def sendMessage(
      edge: EdgeTriplet[(K, (Set[VertexId], Set[Seq[VertexId]], Set[VertexId], Boolean)), Double]
    ): Iterator[(VertexId, (Set[VertexId], Set[Seq[VertexId]], Boolean))] = {
      val (_, (goals, paths, _, shouldSendMessage)) = edge.srcAttr
      val (_, (_, _, takenFrom, _)) = edge.dstAttr

      val srcId = edge.srcId
      val dstId = edge.dstId

      if (shouldSendMessage && goals.contains(dstId) && !takenFrom.contains(srcId))
        Iterator((dstId, (Set(srcId), paths, true)))
      else
        Iterator.empty
    }

    def mergeMessage(
      a: (Set[VertexId], Set[Seq[VertexId]], Boolean),
      b: (Set[VertexId], Set[Seq[VertexId]], Boolean)
    ): (Set[VertexId], Set[Seq[VertexId]], Boolean) = {
      val (aIds, aPaths, aShouldSendMessage) = a
      val (bIds, bPaths, bShouldSendMessage) = b

      if (aShouldSendMessage && bShouldSendMessage)
        (aIds ++ bIds, aPaths ++ bPaths, true)
      else if (aShouldSendMessage) a
      else if (bShouldSendMessage) b
      else (Set[VertexId](), Set[Seq[VertexId]](), false)
    }

    val init = (Set[VertexId](), Set[Seq[VertexId]](), false)

    val res = g.pregel(init)(vertexProgram, sendMessage, mergeMessage)

    val (_, (_, (_, paths, _, _ ))) =
      res.vertices.filter { case (id, _) => id == source }.collect.head

    paths
  }

}
