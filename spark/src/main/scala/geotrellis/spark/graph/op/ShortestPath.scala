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

    val vertices = graphRDD.vertices.map { case(id, (key, v)) =>
      if (sources.contains(id)) (id, (key, 0.0))
      else if (v.isNaN) (id, (key, v))
      else (id, (key, init))
    }

    val g = Graph(vertices, graphRDD.edges)

    def vertexProgram(id: VertexId, attr: (K, Double), msg: Double): (K, Double) = {
      val (key, before) = attr
      (key, math.min(before, msg))
    }

    def sendMessage(
      edge: EdgeTriplet[(K, Double), Double]): Iterator[(VertexId, Double)] = {
      val (_, srcAttr) = edge.srcAttr
      val (_, dstAttr) = edge.dstAttr
      if (srcAttr.isNaN || dstAttr.isNaN) Iterator.empty
      else {
        val newAttr = srcAttr + edge.attr
        if (dstAttr > newAttr) Iterator((edge.dstId, newAttr))
        else Iterator.empty
      }
    }

    def mergeMsg(a: Double, b: Double): Double = math.min(a, b)

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
  ): Graph[(K, (Double, Set[VertexId])), Double] = {
    val initValue = Double.MaxValue

    val init = (initValue, Set[VertexId]())

    val vertices = graphRDD.vertices.map { case(id, (key, v)) =>
      if (source == id) (id, (key, (0.0, Set[VertexId]())))
      else if (v.isNaN) (id, (key, (v, Set[VertexId]())))
      else (id, (key, (initValue, Set[VertexId]())))
    }

    val g = Graph(vertices, graphRDD.edges).subgraph { edge =>
      val (_, (srcAttr, _)) = edge.srcAttr
      val (_, (dstAttr, _)) = edge.dstAttr
      !srcAttr.isNaN && !dstAttr.isNaN
    }

    def vertexProgram(
      id: VertexId,
      attr: (K, (Double, Set[VertexId])),
      msg: (Double, Set[VertexId])
    ): (K, (Double, Set[VertexId])) = {
      val (key, (oldCost, bestNeighborSet)) = attr
      val (newCost, incomingIds) = msg

      if (newCost < oldCost)
        (key, (newCost, incomingIds))
      else if (newCost == oldCost)
        (key, (newCost, incomingIds ++ bestNeighborSet))
      else
        attr
    }

    def sendMessage(
      edge: EdgeTriplet[(K, (Double, Set[VertexId])), Double]
    ): Iterator[(VertexId, (Double, Set[VertexId]))] = {
      val (_, (srcAttr, _)) = edge.srcAttr
      val (_, (dstAttr, _)) = edge.dstAttr

      val newAttr = srcAttr + edge.attr
      if (dstAttr > newAttr) Iterator((edge.dstId, (newAttr, Set(edge.srcId))))
      else Iterator.empty
    }

    def mergeMessage(
      a: (Double, Set[VertexId]),
      b: (Double, Set[VertexId])): (Double, Set[VertexId]) = {
      val (ac, as) = a
      val (bc, bs) = b

      if (bc < ac) b
      else if (ac < bc) a
      else (ac, as ++ bs)
    }

    g.pregel(init)(vertexProgram, sendMessage, mergeMessage)
  }

  private def accumulatePath[K](
    graph: Graph[(K, (Double, Set[VertexId])), Double],
    source: Long,
    dest: Long
  ): Set[Seq[VertexId]] = {

    val vertices = graph.vertices.map { case(id, (key, (v, later))) =>
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
