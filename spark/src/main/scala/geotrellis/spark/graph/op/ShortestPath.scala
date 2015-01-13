package geotrellis.spark.graph.op

import geotrellis.spark.graph._

import org.apache.spark.graphx._

import spire.syntax.cfor._

import reflect.ClassTag

// TODO: remove all NODATA vertices?
// Can rebuild later and skips a lot of crap
object ShortestPath {

  def apply[K](graphRDD: GraphRDD[K], sources: Seq[VertexId])
    (implicit keyClassTag: ClassTag[K]): GraphRDD[K] = {
    val verticesCount = graphRDD.vertices.count
    sources.foreach { id => if (id >= verticesCount)
      throw new IllegalArgumentException(s"Too large vertexId: $id")
    }

    val init = Double.MaxValue

    val g = graphRDD.subgraph { edge =>
      val (_, srcAttr) = edge.srcAttr
      val (_, dstAttr) = edge.dstAttr
      !srcAttr.isNaN && !dstAttr.isNaN
    }.mapVertices { case(id, (key, v)) =>
        if (sources.contains(id)) (key, 0.0)
        else if (v.isNaN) (key, v)
        else (key, init)
    }

    def vertexProgram(id: VertexId, attr: (K, Double), msg: Double): (K, Double) = {
      val (key, before) = attr
      (key, math.min(before, msg))
    }

    def sendMessage(
      edge: EdgeTriplet[(K, Double), Double]): Iterator[(VertexId, Double)] = {
      val (_, srcAttr) = edge.srcAttr
      val (_, dstAttr) = edge.dstAttr

      val newAttr = srcAttr + edge.attr
      if (dstAttr > newAttr) Iterator((edge.dstId, newAttr))
      else Iterator.empty
    }

    def mergeMsg(a: Double, b: Double): Double = math.min(a, b)

    val res = g.pregel(initialMsg = init, activeDirection = EdgeDirection.Out)(
      vertexProgram, sendMessage, mergeMsg)

    new GraphRDD(res, graphRDD.metaData)
  }

  def apply[K](graphRDD: GraphRDD[K], source: VertexId, dest: VertexId)
    (implicit keyClassTag: ClassTag[K]): Seq[Seq[VertexId]] = {
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
  ): Graph[(K, (Double, Seq[VertexId])), Double] = {
    val initValue = Double.MaxValue

    val g = graphRDD.subgraph { edge =>
      val (_, srcAttr) = edge.srcAttr
      val (_, dstAttr) = edge.dstAttr
      !srcAttr.isNaN && !dstAttr.isNaN
    }.mapVertices { case(id, (key, v)) =>
        if (source == id) (key, (0.0, Seq[VertexId]()))
        else if (v.isNaN) (key, (v, Seq[VertexId]()))
        else (key, (initValue, Seq[VertexId]()))
    }

    def vertexProgram(
      id: VertexId,
      attr: (K, (Double, Seq[VertexId])),
      msg: (Double, Seq[VertexId])
    ): (K, (Double, Seq[VertexId])) = {
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
      edge: EdgeTriplet[(K, (Double, Seq[VertexId])), Double]
    ): Iterator[(VertexId, (Double, Seq[VertexId]))] = {
      val (_, (srcAttr, _)) = edge.srcAttr
      val (_, (dstAttr, _)) = edge.dstAttr

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
    graph: Graph[(K, (Double, Seq[VertexId])), Double],
    source: Long,
    dest: Long
  ): Seq[Seq[VertexId]] = {

    val g = graph.subgraph { edge =>
      val (_, (_, previous)) = edge.srcAttr
      previous.contains(edge.dstId)
    }.mapVertices { case(id, (key, (v, previous))) =>
        if (id == dest) (key, (Seq[Seq[VertexId]](Seq(id))))
        else (key, (Seq[Seq[VertexId]]()))
    }

    def vertexProgram(
      id: VertexId,
      attr: (K, Seq[Seq[VertexId]]),
      msg: Seq[Seq[VertexId]]
    ): (K, Seq[Seq[VertexId]]) = {
      val (key, paths) = attr

      if (!msg.isEmpty) (key, msg.map(id +: _) ++ paths)
      else attr
    }

    def sendMessage(
      edge: EdgeTriplet[(K, Seq[Seq[VertexId]]), Double]
    ): Iterator[(VertexId, Seq[Seq[VertexId]])] = {
      val (_, paths) = edge.srcAttr
      val (_, destPaths) = edge.dstAttr

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

    val (_, (_, paths)) =
      res.vertices.filter { case (id, _) => id == source }.collect.head

    paths
  }

}
